package com.recall.app.sync

import android.content.Context
import androidx.work.*
import com.recall.app.core.util.Constants
import com.recall.app.sync.workers.SyncNotesWorker
import com.recall.app.sync.workers.UploadAttachmentsWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule a one-time sync chain:
     * Upload Attachments → Sync Notes
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

        // Chain: Upload attachments THEN sync notes
        workManager.beginUniqueWork(
            Constants.SYNC_NOTES_WORK,
            ExistingWorkPolicy.KEEP,
            uploadAttachmentsWork
        )
            .then(syncNotesWork)
            .enqueue()

        Timber.d("Scheduled sync chain")
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
     * Cancel all sync work
     */
    fun cancelAllSync() {
        workManager.cancelUniqueWork(Constants.SYNC_NOTES_WORK)
        workManager.cancelUniqueWork(Constants.UPLOAD_ATTACHMENTS_WORK)
        workManager.cancelUniqueWork("periodic_sync")
        Timber.d("Cancelled all sync work")
    }

    private fun getSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
