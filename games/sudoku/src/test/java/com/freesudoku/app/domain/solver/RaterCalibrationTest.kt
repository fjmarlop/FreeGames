package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GridCodec
import com.google.common.truth.Truth.assertThat
import org.junit.BeforeClass
import org.junit.Test
import kotlin.random.Random

/**
 * Calibrates and guards [RatingWeights.DEFAULT] against a labelled sample of the
 * sudoku-exchange puzzle bank (`fixtures/rated_puzzles.csv`: 510 puzzles across
 * easy / medium / hard / diabolical with numeric human ratings 1.2 .. 9.2).
 *
 * The guard tests assert the shipped weights still rank puzzles like humans do.
 * `search prints the best weights it can find` re-runs the fit and prints a
 * candidate — run it after changing the solver's technique set and copy the
 * winner into [RatingWeights.DEFAULT] if it beats what ships.
 */
class RaterCalibrationTest {

    private val rater = DifficultyRater()

    // -- guards -------------------------------------------------------------

    @Test fun `default weights rank puzzles like the human ratings`() {
        val rho = spearman(SCORES_DEFAULT, RATINGS)
        println("Spearman rho (default weights) = ${"%.3f".format(rho)}")
        assertThat(rho).isAtLeast(0.80)
    }

    @Test fun `mean score increases strictly across the tiers`() {
        val byTier = TIERS.indices.groupBy { TIERS[it] }
            .mapValues { (_, idx) -> idx.map { SCORES_DEFAULT[it] }.average() }
        println("tier means: $byTier")
        val ordered = listOf("easy", "medium", "hard", "diabolical").map { byTier.getValue(it) }
        assertThat(ordered).isInStrictOrder()
    }

    @Test fun `easy puzzles land low and diabolical puzzles land high`() {
        for (i in FEATURES.indices) {
            val band = DifficultyBand.fromScore(SCORES_DEFAULT[i])
            when (TIERS[i]) {
                "easy" -> assertThat(band.ordinal).isAtMost(DifficultyBand.FACIL.ordinal)
                "diabolical" -> assertThat(band.ordinal).isAtLeast(DifficultyBand.DIFICIL.ordinal)
            }
        }
    }

    @Test fun `band cut points give every band some coverage across the sample`() {
        val counts = SCORES_DEFAULT.map { DifficultyBand.fromScore(it) }.groupingBy { it }.eachCount()
        println("band distribution: $counts")
        // every band except the very top should be represented by the sampled corpus
        for (b in listOf(
            DifficultyBand.FACIL, DifficultyBand.MEDIO,
            DifficultyBand.DIFICIL, DifficultyBand.EXPERTO,
        )) {
            assertThat(counts.getOrDefault(b, 0)).isGreaterThan(0)
        }
    }

    // -- fit ---------------------------------------------------------------

    /** Target band per puzzle, derived from its human rating (the calibration ground truth). */
    private fun targetBand(rating: Double): DifficultyBand = when {
        rating < 2.4 -> DifficultyBand.FACIL
        rating < 3.3 -> DifficultyBand.MEDIO
        rating < 4.8 -> DifficultyBand.DIFICIL
        rating < 7.0 -> DifficultyBand.EXPERTO
        else -> DifficultyBand.MAESTRO
    }

    @Test fun `search prints the best weights and band cut points it can find`() {
        val targets = RATINGS.map { targetBand(it) }
        val rng = Random(20260904)

        var bestW: RatingWeights? = null
        var bestCuts: DoubleArray? = null
        var bestScore = -1.0

        repeat(50_000) {
            val w = RatingWeights(
                wHardest = rng.nextDouble(0.2, 1.1),
                wFrequency = rng.nextDouble(0.0, 0.35),
                wClues = rng.nextDouble(0.1, 1.2),
                cluePivot = rng.nextInt(26, 35),
                unsolvedPenalty = rng.nextDouble(10.0, 45.0),
            )
            val scores = FEATURES.map { rater.scoreOf(it, w) }
            if (!tierMeansMonotone(scores)) return@repeat

            // ascending band cut points: MEDIO, DIFICIL, EXPERTO, MAESTRO (FACIL is the 0.0 floor)
            val raw = DoubleArray(4) { rng.nextDouble(2.0, 95.0) }.also { it.sort() }
            val rho = spearman(scores, RATINGS)
            val within1 = scores.indices.count { i ->
                kotlin.math.abs(bandOf(scores[i], raw).ordinal - targets[i].ordinal) <= 1
            }.toDouble() / scores.size
            val exact = scores.indices.count { i ->
                bandOf(scores[i], raw) == targets[i]
            }.toDouble() / scores.size

            val objective = 0.5 * rho + 0.35 * within1 + 0.15 * exact
            if (objective > bestScore) {
                bestScore = objective
                bestW = w
                bestCuts = raw
            }
        }

        val w = bestW!!
        val cuts = bestCuts!!
        val scores = FEATURES.map { rater.scoreOf(it, w) }
        val rho = spearman(scores, RATINGS)
        val within1 = scores.indices.count {
            kotlin.math.abs(bandOf(scores[it], cuts).ordinal - targets[it].ordinal) <= 1
        }.toDouble() / scores.size

        println(
            """
            === best fit: rho=${"%.3f".format(rho)} within1=${"%.2f".format(within1)} ===
            RatingWeights(
                wHardest = ${"%.2f".format(w.wHardest)},
                wFrequency = ${"%.2f".format(w.wFrequency)},
                wClues = ${"%.2f".format(w.wClues)},
                cluePivot = ${w.cluePivot},
                unsolvedPenalty = ${"%.1f".format(w.unsolvedPenalty)},
            )
            band lowerBounds: FACIL=0.0 MEDIO=${f(cuts[0])} DIFICIL=${f(cuts[1])} EXPERTO=${f(cuts[2])} MAESTRO=${f(cuts[3])}
            score range: ${f(scores.min())} .. ${f(scores.max())}
            tier means: ${tierMeans(scores)}
            """.trimIndent()
        )
        assertThat(bestScore).isGreaterThan(0.0)
    }

    private fun f(x: Double) = "%.1f".format(x)

    private fun bandOf(score: Double, cuts: DoubleArray): DifficultyBand {
        var b = 0
        for (c in cuts) if (score >= c) b++
        return DifficultyBand.entries[b.coerceAtMost(DifficultyBand.entries.lastIndex)]
    }

    // -- helpers ---------------------------------------------------------------

    private fun tierMeansMonotone(scores: List<Double>): Boolean {
        val m = tierMeans(scores)
        return m["easy"]!! < m["medium"]!! && m["medium"]!! < m["hard"]!! && m["hard"]!! < m["diabolical"]!!
    }

    private fun tierMeans(scores: List<Double>): Map<String, Double> =
        TIERS.indices.groupBy { TIERS[it] }.mapValues { (_, idx) -> idx.map { scores[it] }.average() }

    private fun percentiles(scores: List<Double>): String {
        val s = scores.sorted()
        fun p(q: Double) = "%.1f".format(s[(q * (s.size - 1)).toInt()])
        return "p10=${p(0.10)} p25=${p(0.25)} p45=${p(0.45)} p65=${p(0.65)} p82=${p(0.82)} p95=${p(0.95)}"
    }

    private fun spearman(xs: List<Double>, ys: List<Double>): Double {
        val rx = ranks(xs)
        val ry = ranks(ys)
        val n = xs.size
        val mx = rx.average()
        val my = ry.average()
        var num = 0.0
        var dx = 0.0
        var dy = 0.0
        for (i in 0 until n) {
            val a = rx[i] - mx
            val b = ry[i] - my
            num += a * b
            dx += a * a
            dy += b * b
        }
        return num / kotlin.math.sqrt(dx * dy)
    }

    private fun ranks(values: List<Double>): DoubleArray {
        val idx = values.indices.sortedBy { values[it] }
        val r = DoubleArray(values.size)
        var i = 0
        while (i < idx.size) {
            var j = i
            while (j + 1 < idx.size && values[idx[j + 1]] == values[idx[i]]) j++
            val avg = (i + j) / 2.0 + 1.0
            for (k in i..j) r[idx[k]] = avg
            i = j + 1
        }
        return r
    }

    companion object {
        private lateinit var FEATURES: List<RatingFeatures>
        private lateinit var RATINGS: List<Double>
        private lateinit var TIERS: List<String>
        private lateinit var SCORES_DEFAULT: List<Double>

        @BeforeClass
        @JvmStatic
        fun load() {
            val rater = DifficultyRater()
            val lines = RaterCalibrationTest::class.java
                .getResourceAsStream("/fixtures/rated_puzzles.csv")!!
                .bufferedReader().readLines()
                .drop(1)
                .filter { it.isNotBlank() }

            val features = ArrayList<RatingFeatures>(lines.size)
            val ratings = ArrayList<Double>(lines.size)
            val tiers = ArrayList<String>(lines.size)
            for (line in lines) {
                val (givens, rating, tier) = line.split(",")
                features += rater.features(GridCodec.decode(givens))
                ratings += rating.toDouble()
                tiers += tier
            }
            FEATURES = features
            RATINGS = ratings
            TIERS = tiers
            SCORES_DEFAULT = features.map { rater.scoreOf(it) }
        }
    }
}
