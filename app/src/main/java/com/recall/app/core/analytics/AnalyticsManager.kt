package com.recall.app.core.analytics

import android.content.Context
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.recall.app.BuildConfig
import com.recall.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import io.sentry.SentryLevel
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
 * Replace these with your actual keys for production.
 */
object AnalyticsConfig {
    // Sentry DSN - get from https://sentry.io
    // Format: https://<public_key>@<organization>.ingest.sentry.io/<project_id>
    const val SENTRY_DSN = "" // Leave empty to disable Sentry

    // PostHog API Key - get from https://posthog.com
    const val POSTHOG_API_KEY = "" // Leave empty to disable PostHog
    const val POSTHOG_HOST = "https://app.posthog.com" // Or your self-hosted instance
}

/**
 * Manages analytics and crash reporting services (Sentry, PostHog)
 * Respects user privacy preferences.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isSentryInitialized = false
    private var isPostHogInitialized = false
    private var analyticsEnabled = true
    private var crashReportingEnabled = true

    /**
     * Initialize analytics services. Should be called from Application.onCreate()
     */
    fun initialize() {
        // Listen to preference changes
        scope.launch {
            userPreferencesRepository.userPreferencesFlow.collectLatest { preferences ->
                analyticsEnabled = preferences.enableAnalytics
                crashReportingEnabled = preferences.enableCrashReporting

                // Update Sentry status
                if (isSentryInitialized) {
                    Sentry.configureScope { scope ->
                        scope.level = if (crashReportingEnabled) SentryLevel.ERROR else null
                    }
                }

                Timber.d("Analytics preferences updated - analytics: $analyticsEnabled, crash: $crashReportingEnabled")
            }
        }

        initializeSentry()
        initializePostHog()
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
                options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"

                // Set sample rates
                options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
                options.profilesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1

                // Only send errors if crash reporting is enabled
                options.beforeSend = { event ->
                    if (crashReportingEnabled) event else null
                }

                // Don't send breadcrumbs in debug
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

    /**
     * Track a screen view event
     */
    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap()) {
        if (!analyticsEnabled || !isPostHogInitialized) return

        try {
            PostHog.screen(screenName, properties)
            Timber.d("Tracked screen view: $screenName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to track screen view")
        }
    }

    /**
     * Track a custom event
     */
    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        if (!analyticsEnabled || !isPostHogInitialized) return

        try {
            PostHog.capture(eventName, properties = properties)
            Timber.d("Tracked event: $eventName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to track event")
        }
    }

    /**
     * Identify the current user
     */
    fun identifyUser(userId: String, properties: Map<String, Any> = emptyMap()) {
        if (!analyticsEnabled || !isPostHogInitialized) return

        try {
            PostHog.identify(userId, properties = properties)
            Timber.d("Identified user: $userId")

            // Also set user in Sentry
            if (isSentryInitialized && crashReportingEnabled) {
                Sentry.configureScope { scope ->
                    scope.setTag("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to identify user")
        }
    }

    /**
     * Reset analytics (e.g., on logout)
     */
    fun reset() {
        try {
            if (isPostHogInitialized) {
                PostHog.reset()
            }
            if (isSentryInitialized) {
                Sentry.configureScope { scope ->
                    scope.clear()
                }
            }
            Timber.d("Analytics reset")
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset analytics")
        }
    }

    /**
     * Log an exception to Sentry
     */
    fun logException(throwable: Throwable, additionalContext: Map<String, Any> = emptyMap()) {
        if (!crashReportingEnabled || !isSentryInitialized) return

        try {
            Sentry.configureScope { scope ->
                additionalContext.forEach { (key, value) ->
                    scope.setExtra(key, value.toString())
                }
            }
            Sentry.captureException(throwable)
            Timber.d("Logged exception to Sentry: ${throwable.message}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log exception")
        }
    }

    /**
     * Add breadcrumb for debugging
     */
    fun addBreadcrumb(message: String, category: String = "app") {
        if (!crashReportingEnabled || !isSentryInitialized) return

        try {
            Sentry.addBreadcrumb(message, category)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add breadcrumb")
        }
    }

    // Common events
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
