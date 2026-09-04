package com.freesudoku.app.domain.solver

/** A single deduction step: digits placed and/or candidates eliminated. */
data class TechniqueStep(
    val technique: TechniqueId,
    val placements: List<Pair<Int, Int>> = emptyList(),   // (cellIndex, digit)
    val eliminations: List<Pair<Int, Int>> = emptyList(),  // (cellIndex, digit)
) {
    val isProgress: Boolean get() = placements.isNotEmpty() || eliminations.isNotEmpty()
}

enum class TechniqueId {
    NAKED_SINGLE,
    HIDDEN_SINGLE,
    LOCKED_CANDIDATES,
    NAKED_SUBSET,
    HIDDEN_SUBSET,
    X_WING,
}

/** Cost weights feed the difficulty rater; ordering also drives solver technique selection. */
val TECHNIQUE_COST: Map<TechniqueId, Int> = mapOf(
    TechniqueId.NAKED_SINGLE to 10,
    TechniqueId.HIDDEN_SINGLE to 15,
    TechniqueId.LOCKED_CANDIDATES to 25,
    TechniqueId.NAKED_SUBSET to 40,
    TechniqueId.HIDDEN_SUBSET to 48,
    TechniqueId.X_WING to 65,
)

interface Technique {
    val id: TechniqueId
    val cost: Int

    /** Applies the technique to [state] in place. Returns the step taken, or null if it did not fire. */
    fun apply(state: SolverState): TechniqueStep?
}
