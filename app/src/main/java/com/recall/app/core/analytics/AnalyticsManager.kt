package com.recall.app.core.analytics

import android.content.Context
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.recall.app.BuildConfig
import com.recall.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for analytics services.
 */
object AnalyticsConfig {
    const val SENTRY_DSN = ""          // Leave empty to disable Sentry
    const val POSTHOG_API_KEY = ""     // Leave empty to disable PostHog
    const val POSTHOG_HOST = "https://app.posthog.com"
}

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isSentryInitialized = false
    private var isPostHogInitialized = false

    @Volatile private var analyticsEnabled = true
    @Volatile private var crashReportingEnabled = true

    /**
     * Call from Application.onCreate()
     */
    fun initialize() {
        // Initialize SDKs first (they will still be gated by keys + beforeSend / opt-out)
        initializeSentry()
        initializePostHog()

        // Listen to preference changes
        scope.launch {
            userPreferencesRepository.userPreferencesFlow.collectLatest { preferences ->
                analyticsEnabled = preferences.enableAnalytics
                crashReportingEnabled = preferences.enableCrashReporting

                // PostHog: opt in/out by identifying reset + disabling capture
                if (isPostHogInitialized) {
                    try {
                        if (!analyticsEnabled) {
                            PostHog.reset()
                            // Some versions have optOut(); some don't. Safe to ignore if missing.
                            runCatching { PostHog.optOut() }
                        } else {
                            runCatching { PostHog.optIn() }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed updating PostHog opt-in/out")
                    }
                }

                Timber.d(
                    "Analytics preferences updated - analytics: $analyticsEnabled, crash: $crashReportingEnabled"
                )
            }
        }
    }

    private fun initializeSentry() {
        if (AnalyticsConfig.SENTRY_DSN.isBlank()) {
            Timber.d("Sentry DSN not configured, skipping initialization")
            return
        }

        try {
            SentryAndroid.init(context) { options ->
                options.dsn = AnalyticsConfig.SENTRY_DSN
                options.isDebug = BuildConfig.DEBUG
                options.environment = if (BuildConfig.DEBUG) "development" else "production"
                options.release =
                    "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"

                options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
                options.profilesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1

                // ✅ Correct signature: (event, hint) -> SentryEvent?
                options.beforeSend = io.sentry.SentryOptions.BeforeSendCallback { event: SentryEvent, _: Hint ->
                    if (crashReportingEnabled) event else null
                }

                // Avoid noisy session tracking in debug if you want
                options.isEnableAutoSessionTracking = !BuildConfig.DEBUG
            }

            isSentryInitialized = true
            Timber.d("Sentry initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Sentry")
        }
    }

    private fun initializePostHog() {
        if (AnalyticsConfig.POSTHOG_API_KEY.isBlank()) {
            Timber.d("PostHog API key not configured, skipping initialization")
            return
        }

        try {
            val config = PostHogAndroidConfig(
                apiKey = AnalyticsConfig.POSTHOG_API_KEY,
                host = AnalyticsConfig.POSTHOG_HOST
            ).apply {
                debug = BuildConfig.DEBUG
                captureApplicationLifecycleEvents = true
                captureDeepLinks = true
                captureScreenViews = false // We'll track manually
            }

            PostHogAndroid.setup(context, config)
            isPostHogInitialized = true
            Timber.d("PostHog initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize PostHog")
        }
    }

    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap()) {
        if (!analyticsEnabled || !isPostHogInitialized) return
        try {
            PostHog.screen(screenName, properties)
        } catch (e: Exception) {
            Timber.e(e, "Failed to track screen view")
        }
    }

    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        if (!analyticsEnabled || !isPostHogInitialized) return
        try {
            // ✅ Use positional args for broad compatibility (avoids "no parameter named properties")
            PostHog.capture(eventName, null, properties)
        } catch (e: Exception) {
            Timber.e(e, "Failed to track event")
        }
    }

    fun identifyUser(userId: String) {
        if (!analyticsEnabled || !isPostHogInitialized) return

        try {
            PostHog.identify(userId)
            Timber.d("Identified user: $userId")

            if (isSentryInitialized && crashReportingEnabled) {
                Sentry.configureScope { scope ->
                    scope.setTag("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to identify user")
        }
    }


    fun reset() {
        try {
            if (isPostHogInitialized) PostHog.reset()
            if (isSentryInitialized) Sentry.configureScope { it.clear() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset analytics")
        }
    }

    fun logException(throwable: Throwable, additionalContext: Map<String, Any> = emptyMap()) {
        if (!crashReportingEnabled || !isSentryInitialized) return
        try {
            Sentry.configureScope { scope ->
                additionalContext.forEach { (key, value) ->
                    scope.setExtra(key, value.toString())
                }
            }
            Sentry.captureException(throwable)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log exception")
        }
    }

    fun addBreadcrumb(message: String, category: String = "app") {
        if (!crashReportingEnabled || !isSentryInitialized) return
        try {
            Sentry.addBreadcrumb(message, category)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add breadcrumb")
        }
    }

    object Events {
        const val NOTE_CREATED = "note_created"
        const val NOTE_EDITED = "note_edited"
        const val NOTE_DELETED = "note_deleted"
        const val NOTE_SHARED = "note_shared"
        const val NOTE_EXPORTED = "note_exported"
        const val SEARCH_PERFORMED = "search_performed"
        const val AI_PROCESSING_COMPLETED = "ai_processing_completed"
        const val DAILY_RECALL_VIEWED = "daily_recall_viewed"
        const val ATTACHMENT_ADDED = "attachment_added"
        const val VOICE_RECORDED = "voice_recorded"
        const val IMAGE_CAPTURED = "image_captured"
        const val OCR_PERFORMED = "ocr_performed"
        const val SYNC_COMPLETED = "sync_completed"
        const val SETTINGS_CHANGED = "settings_changed"
    }
}
