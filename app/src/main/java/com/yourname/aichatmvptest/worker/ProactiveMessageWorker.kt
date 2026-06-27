package com.yourname.aichatmvptest.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.aichatmvptest.notification.RhodesNotificationCenter
import com.yourname.aichatmvptest.shared.database.AndroidDatabaseFactory
import com.yourname.aichatmvptest.shared.database.LocalChatRepository
import com.yourname.aichatmvptest.shared.model.ChatMessage
import com.yourname.aichatmvptest.shared.model.MessageContent
import com.yourname.aichatmvptest.shared.model.MessageStatus
import com.yourname.aichatmvptest.shared.model.MessageType
import com.yourname.aichatmvptest.shared.model.SenderType
import com.yourname.aichatmvptest.shared.scheduler.ProactiveMessagePolicy
import java.util.Calendar

class ProactiveMessageWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("rhodes_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("proactive_enabled", true)) return Result.success()

        val repository = LocalChatRepository(AndroidDatabaseFactory(applicationContext).createDatabase())
        repository.seedDefaultsIfNeeded()
        val characters = repository.getCharacters()
        val conversations = repository.getConversations()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val policy = ProactiveMessagePolicy()
        val target = conversations.firstNotNullOfOrNull { conversation ->
            val character = characters.firstOrNull { it.id == conversation.characterId } ?: return@firstNotNullOfOrNull null
            if (hasRecentProactiveMessage(repository, conversation.id)) return@firstNotNullOfOrNull null
            val decision = policy.evaluate(character, conversation.unreadCount, hour)
            if (decision.shouldSend) conversation to character else null
        } ?: return Result.success()

        val (conversation, character) = target
        val content = buildProactiveMessage(character.id)
        val message = ChatMessage(
            id = "proactive_${System.currentTimeMillis()}_${character.id}",
            conversationId = conversation.id,
            senderType = SenderType.Ai,
            senderId = character.id,
            messageType = MessageType.Text,
            content = MessageContent.Text(content),
            status = MessageStatus.Sent,
            createdAtMillis = System.currentTimeMillis(),
        )
        repository.saveMessage(message)

        RhodesNotificationCenter.ensureChannels(applicationContext)
        RhodesNotificationCenter.showMessageNotification(
            context = applicationContext,
            title = character.name,
            content = content,
        )
        return Result.success()
    }

    private suspend fun hasRecentProactiveMessage(repository: LocalChatRepository, conversationId: String): Boolean {
        val twoHoursAgo = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
        return repository.getMessages(conversationId).asReversed().any { message ->
            message.id.startsWith("proactive_") && message.createdAtMillis >= twoHoursAgo
        }
    }

    private fun buildProactiveMessage(characterId: String): String {
        return when (characterId) {
            "medic" -> "例行状态确认：现在身体和情绪都还好吗？"
            "tactician" -> "我看了一下任务节奏，需要的话可以帮你拆成下一步。"
            "logistics" -> "补给检查，顺便来看看你有没有好好休息。"
            "secretary" -> "提醒事项待整理。你有需要我记录的新安排吗？"
            else -> "信号很弱，但我还在。"
        }
    }
}
