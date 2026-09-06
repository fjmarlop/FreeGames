package com.freesudoku.app.ui.game

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Emits once per second while collected. Swapped for a controllable fake in tests. */
fun interface Ticker {
    fun oneSecondTicks(): Flow<Unit>
}

class RealTicker @Inject constructor() : Ticker {
    override fun oneSecondTicks(): Flow<Unit> = flow {
        while (true) {
            delay(1_000)
            emit(Unit)
        }
    }
}
