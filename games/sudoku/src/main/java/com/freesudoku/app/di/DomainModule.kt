package com.freesudoku.app.di

import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.generator.FullGridGenerator
import com.freesudoku.app.domain.generator.PuzzleCarver
import com.freesudoku.app.domain.generator.PuzzleFactory
import com.freesudoku.app.domain.solver.DifficultyRater
import com.freesudoku.app.domain.solver.LogicalSolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun campaignCurve(): CampaignCurve = CampaignCurve()

    @Provides
    @Singleton
    fun difficultyRater(): DifficultyRater = DifficultyRater(LogicalSolver())

    @Provides
    @Singleton
    fun puzzleFactory(curve: CampaignCurve, rater: DifficultyRater): PuzzleFactory {
        val random = Random(System.nanoTime())
        return PuzzleFactory(
            random = random,
            curve = curve,
            fullGridGenerator = FullGridGenerator(random),
            carver = PuzzleCarver(random),
            rater = rater,
        )
    }
}
