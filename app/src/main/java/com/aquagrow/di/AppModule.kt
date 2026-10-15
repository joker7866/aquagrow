package com.aquagrow.di

import com.aquagrow.BuildConfig
import com.aquagrow.data.assistant.AssistantApi
import com.aquagrow.data.assistant.HttpAssistantApi
import com.aquagrow.domain.time.SystemTimeProvider
import com.aquagrow.domain.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-lifetime scope for work that must outlive a screen (seeding, scheduling). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The user's own backend (assistant.properties); the app never calls an AI provider directly. */
    @Provides
    @Singleton
    fun provideAssistantApi(): AssistantApi = HttpAssistantApi(BuildConfig.ASSISTANT_BASE_URL)
}
