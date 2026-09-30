package com.ariadna.sudoku.di

import com.ariadna.sudoku.data.game.GameRepository
import com.ariadna.sudoku.data.game.InMemoryGameRepository
import com.ariadna.sudoku.data.settings.DefaultSettingsRepository
import com.ariadna.sudoku.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindGameRepository(impl: InMemoryGameRepository): GameRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DefaultSettingsRepository): SettingsRepository
}
