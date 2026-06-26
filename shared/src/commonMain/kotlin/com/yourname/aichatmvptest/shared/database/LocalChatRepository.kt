package com.yourname.aichatmvptest.shared.database

import com.yourname.aichatmvptest.shared.data.SeedData
import com.yourname.aichatmvptest.shared.model.AiCharacter
import com.yourname.aichatmvptest.shared.model.ChatMessage
import com.yourname.aichatmvptest.shared.model.Conversation
import com.yourname.aichatmvptest.shared.model.MessageContent
import com.yourname.aichatmvptest.shared.model.MessageStatus
import com.yourname.aichatmvptest.shared.model.MessageType
import com.yourname.aichatmvptest.shared.model.SenderType
import com.yourname.aichatmvptest.shared.repository.ChatRepository
import kotlinx.serialization.json.Json

class LocalChatRepository(
    private val database: RhodesDatabase,
) : ChatRepository {
    private val queries = database.rhodesDatabaseQueries

    override suspend fun seedDefaultsIfNeeded() {
        if (queries.countCharacters().executeAsOne() > 0L) return

        val now = nowMillis()
        SeedData.characters.forEachIndexed { index, character ->
            queries.insertCharacter(
                id = character.id,
                name = character.name,
                title = character.title,
                description = character.description,
                persona_prompt = character.personaPrompt,
                speaking_style = character.speakingStyle,
                voice_style = character.voiceStyle,
                animation_pack = character.animationPack,
                proactive_level = character.proactiveLevel.toLong(),
                enabled = 1L,
                created_at = now + index,
            )
        }

        SeedData.conversations.forEachIndexed { index, conversation ->
            queries.insertConversation(
                id = conversation.id,
                character_id = conversation.characterId,
                title = conversation.title,
                last_message_id = null,
                unread_count = conversation.unreadCount.toLong(),
                pinned = 0L,
                muted = 0L,
                updated_at = now + index,
                created_at = now + index,
            )
        }

        val welcome = ChatMessage(
            id = "seed_welcome_medic",
            conversationId = "conv_medic",
            senderType = SenderType.Ai,
            senderId = "medic",
            messageType = MessageType.Text,
            content = MessageContent.Text("罗德岛通讯链路已建立。"),
            status = MessageStatus.Sent,
            createdAtMillis = now,
        )
        saveMessage(welcome)
    }

    override suspend fun getCharacters(): List<AiCharacter> {
        return queries.selectAllCharacters().executeAsList().map { row ->
            AiCharacter(
                id = row.id,
                name = row.name,
                title = row.title,
                description = row.description,
                personaPrompt = row.persona_prompt,
                speakingStyle = row.speaking_style,
                voiceStyle = row.voice_style,
                animationPack = row.animation_pack,
                proactiveLevel = row.proactive_level.toInt(),
            )
        }
    }

    override suspend fun getConversations(): List<Conversation> {
        return queries.selectAllConversations().executeAsList().map { row ->
            val lastMessageText = row.last_message_id
                ?.let { id -> queries.selectMessagesByConversation(row.id).executeAsList().firstOrNull { it.id == id } }
                ?.content_json
                ?.let(::messagePreview)
                ?: SeedData.conversations.firstOrNull { it.id == row.id }?.lastMessage.orEmpty()

            Conversation(
                id = row.id,
                characterId = row.character_id,
                title = row.title,
                lastMessage = lastMessageText,
                unreadCount = row.unread_count.toInt(),
                updatedAtText = "本地",
            )
        }
    }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> {
        return queries.selectMessagesByConversation(conversationId).executeAsList().map { row ->
            ChatMessage(
                id = row.id,
                conversationId = row.conversation_id,
                senderType = SenderType.valueOf(row.sender_type),
                senderId = row.sender_id,
                messageType = MessageType.valueOf(row.message_type),
                content = json.decodeFromString(MessageContent.serializer(), row.content_json),
                status = MessageStatus.valueOf(row.status),
                createdAtMillis = row.created_at,
            )
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        val contentJson = json.encodeToString(MessageContent.serializer(), message.content)
        queries.insertMessage(
            id = message.id,
            conversation_id = message.conversationId,
            sender_type = message.senderType.name,
            sender_id = message.senderId,
            message_type = message.messageType.name,
            content_json = contentJson,
            status = message.status.name,
            created_at = message.createdAtMillis,
            updated_at = nowMillis(),
        )
        queries.updateConversationLastMessage(
            last_message_id = message.id,
            unread_count = 0L,
            updated_at = message.createdAtMillis,
            id = message.conversationId,
        )
    }

    private fun messagePreview(contentJson: String): String {
        return runCatching {
            when (val content = json.decodeFromString(MessageContent.serializer(), contentJson)) {
                is MessageContent.Text -> content.text
                is MessageContent.Image -> "[图片]"
                is MessageContent.Voice -> "[语音]"
                is MessageContent.Gift -> "[${content.giftType}] ${content.name}"
                is MessageContent.Call -> "[${content.callType}] ${content.status}"
            }
        }.getOrDefault("")
    }

    private fun nowMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
