package com.yourname.aichatmvptest.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.aichatmvptest.notification.RhodesNotificationCenter

class ProactiveMessageWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        RhodesNotificationCenter.ensureChannels(applicationContext)
        RhodesNotificationCenter.showMessageNotification(
            context = applicationContext,
            title = "后勤伙伴",
            content = "例行通讯检查：后台主动消息通道已可用。",
        )
        return Result.success()
    }
}
