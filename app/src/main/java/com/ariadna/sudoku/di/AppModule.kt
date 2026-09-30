package com.ariadna.sudoku.di

import android.os.SystemClock
import com.ariadna.sudoku.util.ElapsedClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @DefaultDispatcher default: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + default)

    @Provides
    fun provideRandom(): Random = Random.Default

    @Provides
    fun provideElapsedClock(): ElapsedClock = ElapsedClock { SystemClock.elapsedRealtime() }
}
