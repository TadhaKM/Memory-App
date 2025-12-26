package com.recall.app.sync
import dagger.hilt.android.qualifiers.ApplicationContext

import android.content.Context
import androidx.work.*
import com.recall.app.core.util.Constants
import com.recall.app.sync.workers.ProcessAiWorker
import com.recall.app.sync.workers.ResurfaceScoreWorker
import com.recall.app.sync.workers.SyncNotesWorker
import com.recall.app.sync.workers.UploadAttachmentsWorker
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule a one-time sync chain:
     * Upload Attachments → Sync Notes → Process AI
     */
    fun scheduleSyncChain() {
        val uploadAttachmentsWork = OneTimeWorkRequestBuilder<UploadAttachmentsWorker>()
            .setConstraints(getSyncConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Constants.SYNC_BACKOFF_DELAY_MS,
                TimeUnit.MILLISECONDS
            )
            .build()

        val syncNotesWork = OneTimeWorkRequestBuilder<SyncNotesWorker>()
            .setConstraints(getSyncConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Constants.SYNC_BACKOFF_DELAY_MS,
                TimeUnit.MILLISECONDS
            )
            .build()

        val processAiWork = OneTimeWorkRequestBuilder<ProcessAiWorker>()
            .setConstraints(getSyncConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Constants.SYNC_BACKOFF_DELAY_MS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Chain: Upload attachments → Sync notes → Process AI
        workManager.beginUniqueWork(
            Constants.SYNC_NOTES_WORK,
            ExistingWorkPolicy.KEEP,
            uploadAttachmentsWork
        )
            .then(syncNotesWork)
            .then(processAiWork)
            .enqueue()

        Timber.d("Scheduled sync chain with AI processing")
    }

    /**
     * Schedule periodic sync (every 15 minutes when conditions are met)
     */
    fun schedulePeriodicSync() {
        val periodicSyncWork = PeriodicWorkRequestBuilder<SyncNotesWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(getSyncConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Constants.SYNC_BACKOFF_DELAY_MS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncWork
        )

        Timber.d("Scheduled periodic sync")
    }

    /**
     * Schedule nightly resurface score calculation at 2:30 AM
     */
    fun scheduleNightlyResurfacing() {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If target time has passed for today, schedule for tomorrow
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelayMillis = targetTime.timeInMillis - currentTime.timeInMillis

        val resurfaceWork = PeriodicWorkRequestBuilder<ResurfaceScoreWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "nightly_resurface_scores",
            ExistingPeriodicWorkPolicy.KEEP,
            resurfaceWork
        )

        Timber.d("Scheduled nightly resurface score calculation at 2:30 AM (initial delay: ${initialDelayMillis}ms)")
    }

    /**
     * Cancel all sync work
     */
    fun cancelAllSync() {
        workManager.cancelUniqueWork(Constants.SYNC_NOTES_WORK)
        workManager.cancelUniqueWork(Constants.UPLOAD_ATTACHMENTS_WORK)
        workManager.cancelUniqueWork("periodic_sync")
        workManager.cancelUniqueWork("nightly_resurface_scores")
        Timber.d("Cancelled all sync work")
    }

    private fun getSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
