package com.yourname.aichatmvptest.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ProactiveMessageScheduler {
    private const val WORK_NAME = "rhodes_proactive_message_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ProactiveMessageWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
