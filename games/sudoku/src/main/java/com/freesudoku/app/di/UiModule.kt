package com.freesudoku.app.di

import com.freesudoku.app.ui.game.RealTicker
import com.freesudoku.app.ui.game.Ticker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UiModule {
    @Binds
    abstract fun bindTicker(impl: RealTicker): Ticker
}
