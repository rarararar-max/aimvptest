package com.yourname.aichatmvptest.shared.repository

import com.yourname.aichatmvptest.shared.model.ChatMessage
import com.yourname.aichatmvptest.shared.model.Conversation
import com.yourname.aichatmvptest.shared.model.AiCharacter

interface ChatRepository {
    suspend fun seedDefaultsIfNeeded()
    suspend fun getCharacters(): List<AiCharacter>
    suspend fun getConversations(): List<Conversation>
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun saveMessage(message: ChatMessage)
}
