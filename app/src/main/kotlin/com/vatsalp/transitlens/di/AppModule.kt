package com.vatsalp.transitlens.di

import com.vatsalp.transitlens.core.fusion.ContextFusionEngine
import com.vatsalp.transitlens.core.routing.AccessibilityRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the pure-Kotlin :core engines to the Android graph. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAccessibilityRouter(): AccessibilityRouter = AccessibilityRouter()

    @Provides
    @Singleton
    fun provideContextFusionEngine(): ContextFusionEngine = ContextFusionEngine()
}
