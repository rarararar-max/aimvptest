package com.yourname.aichatmvptest.shared.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String

public class RhodesDatabaseQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectMemoriesByCharacter(character_id: String, mapper: (
    id: String,
    character_id: String,
    type: String,
    content: String,
    importance: Double,
    keywords_json: String,
    embedding_json: String,
    remote_vector_id: String?,
    source_message_id: String?,
    created_at: Long,
    updated_at: Long,
  ) -> T): Query<T> = SelectMemoriesByCharacterQuery(character_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getDouble(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7),
      cursor.getString(8),
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectMemoriesByCharacter(character_id: String): Query<Memory_items> =
      selectMemoriesByCharacter(character_id) { id, character_id_, type, content, importance,
      keywords_json, embedding_json, remote_vector_id, source_message_id, created_at, updated_at ->
    Memory_items(
      id,
      character_id_,
      type,
      content,
      importance,
      keywords_json,
      embedding_json,
      remote_vector_id,
      source_message_id,
      created_at,
      updated_at
    )
  }

  public fun <T : Any> selectAllCharacters(mapper: (
    id: String,
    name: String,
    title: String,
    description: String,
    persona_prompt: String,
    speaking_style: String,
    voice_style: String,
    animation_pack: String,
    proactive_level: Long,
    enabled: Long,
    created_at: Long,
  ) -> T): Query<T> = Query(1_657_956_351, arrayOf("characters"), driver, "RhodesDatabase.sq",
      "selectAllCharacters",
      "SELECT characters.id, characters.name, characters.title, characters.description, characters.persona_prompt, characters.speaking_style, characters.voice_style, characters.animation_pack, characters.proactive_level, characters.enabled, characters.created_at FROM characters ORDER BY created_at ASC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectAllCharacters(): Query<Characters> = selectAllCharacters { id, name, title,
      description, persona_prompt, speaking_style, voice_style, animation_pack, proactive_level,
      enabled, created_at ->
    Characters(
      id,
      name,
      title,
      description,
      persona_prompt,
      speaking_style,
      voice_style,
      animation_pack,
      proactive_level,
      enabled,
      created_at
    )
  }

  public fun countCharacters(): Query<Long> = Query(2_083_860_873, arrayOf("characters"), driver,
      "RhodesDatabase.sq", "countCharacters", "SELECT COUNT(*) FROM characters") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectAllConversations(mapper: (
    id: String,
    character_id: String,
    title: String,
    last_message_id: String?,
    unread_count: Long,
    pinned: Long,
    muted: Long,
    updated_at: Long,
    created_at: Long,
  ) -> T): Query<T> = Query(-982_150_501, arrayOf("conversations"), driver, "RhodesDatabase.sq",
      "selectAllConversations",
      "SELECT conversations.id, conversations.character_id, conversations.title, conversations.last_message_id, conversations.unread_count, conversations.pinned, conversations.muted, conversations.updated_at, conversations.created_at FROM conversations ORDER BY pinned DESC, updated_at DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectAllConversations(): Query<Conversations> = selectAllConversations { id,
      character_id, title, last_message_id, unread_count, pinned, muted, updated_at, created_at ->
    Conversations(
      id,
      character_id,
      title,
      last_message_id,
      unread_count,
      pinned,
      muted,
      updated_at,
      created_at
    )
  }

  public fun <T : Any> selectMessagesByConversation(conversation_id: String, mapper: (
    id: String,
    conversation_id: String,
    sender_type: String,
    sender_id: String,
    message_type: String,
    content_json: String,
    status: String,
    created_at: Long,
    updated_at: Long,
  ) -> T): Query<T> = SelectMessagesByConversationQuery(conversation_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectMessagesByConversation(conversation_id: String): Query<Messages> =
      selectMessagesByConversation(conversation_id) { id, conversation_id_, sender_type, sender_id,
      message_type, content_json, status, created_at, updated_at ->
    Messages(
      id,
      conversation_id_,
      sender_type,
      sender_id,
      message_type,
      content_json,
      status,
      created_at,
      updated_at
    )
  }

  public fun countMessagesByConversation(conversation_id: String): Query<Long> =
      CountMessagesByConversationQuery(conversation_id) { cursor ->
    cursor.getLong(0)!!
  }

  public fun selectConversationSummary(conversation_id: String): Query<String> =
      SelectConversationSummaryQuery(conversation_id) { cursor ->
    cursor.getString(0)!!
  }

  public fun <T : Any> selectModelConfigs(mapper: (
    id: String,
    provider: String,
    model_type: String,
    base_url: String,
    api_key_encrypted: String,
    model_name: String,
    enabled: Long,
    updated_at: Long,
  ) -> T): Query<T> = Query(-961_731_980, arrayOf("model_configs"), driver, "RhodesDatabase.sq",
      "selectModelConfigs",
      "SELECT model_configs.id, model_configs.provider, model_configs.model_type, model_configs.base_url, model_configs.api_key_encrypted, model_configs.model_name, model_configs.enabled, model_configs.updated_at FROM model_configs ORDER BY model_type ASC, provider ASC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!
    )
  }

  public fun selectModelConfigs(): Query<Model_configs> = selectModelConfigs { id, provider,
      model_type, base_url, api_key_encrypted, model_name, enabled, updated_at ->
    Model_configs(
      id,
      provider,
      model_type,
      base_url,
      api_key_encrypted,
      model_name,
      enabled,
      updated_at
    )
  }

  public fun <T : Any> selectEnabledVectorStoreConfig(mapper: (
    id: String,
    provider: String,
    base_url: String,
    api_key_encrypted: String,
    collection_name: String,
    namespace: String?,
    enabled: Long,
    updated_at: Long,
  ) -> T): Query<T> = Query(431_224_683, arrayOf("vector_store_configs"), driver,
      "RhodesDatabase.sq", "selectEnabledVectorStoreConfig",
      "SELECT vector_store_configs.id, vector_store_configs.provider, vector_store_configs.base_url, vector_store_configs.api_key_encrypted, vector_store_configs.collection_name, vector_store_configs.namespace, vector_store_configs.enabled, vector_store_configs.updated_at FROM vector_store_configs WHERE enabled = 1 LIMIT 1") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!
    )
  }

  public fun selectEnabledVectorStoreConfig(): Query<Vector_store_configs> =
      selectEnabledVectorStoreConfig { id, provider, base_url, api_key_encrypted, collection_name,
      namespace, enabled, updated_at ->
    Vector_store_configs(
      id,
      provider,
      base_url,
      api_key_encrypted,
      collection_name,
      namespace,
      enabled,
      updated_at
    )
  }

  public fun upsertMemoryItem(
    id: String,
    character_id: String,
    type: String,
    content: String,
    importance: Double,
    keywords_json: String,
    embedding_json: String,
    remote_vector_id: String?,
    source_message_id: String?,
    created_at: Long,
    updated_at: Long,
  ) {
    driver.execute(250_658_707, """
        |INSERT OR REPLACE INTO memory_items(
        |    id, character_id, type, content, importance, keywords_json, embedding_json, remote_vector_id,
        |    source_message_id, created_at, updated_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 11) {
          bindString(0, id)
          bindString(1, character_id)
          bindString(2, type)
          bindString(3, content)
          bindDouble(4, importance)
          bindString(5, keywords_json)
          bindString(6, embedding_json)
          bindString(7, remote_vector_id)
          bindString(8, source_message_id)
          bindLong(9, created_at)
          bindLong(10, updated_at)
        }
    notifyQueries(250_658_707) { emit ->
      emit("memory_items")
    }
  }

  public fun deleteMemoryItem(id: String) {
    driver.execute(1_623_179_535, """DELETE FROM memory_items WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(1_623_179_535) { emit ->
      emit("memory_items")
    }
  }

  public fun clearMemoriesByCharacter(character_id: String) {
    driver.execute(-1_242_740_874, """DELETE FROM memory_items WHERE character_id = ?""", 1) {
          bindString(0, character_id)
        }
    notifyQueries(-1_242_740_874) { emit ->
      emit("memory_items")
    }
  }

  public fun insertCharacter(
    id: String,
    name: String,
    title: String,
    description: String,
    persona_prompt: String,
    speaking_style: String,
    voice_style: String,
    animation_pack: String,
    proactive_level: Long,
    enabled: Long,
    created_at: Long,
  ) {
    driver.execute(777_337_120, """
        |INSERT OR REPLACE INTO characters(
        |    id, name, title, description, persona_prompt, speaking_style, voice_style,
        |    animation_pack, proactive_level, enabled, created_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 11) {
          bindString(0, id)
          bindString(1, name)
          bindString(2, title)
          bindString(3, description)
          bindString(4, persona_prompt)
          bindString(5, speaking_style)
          bindString(6, voice_style)
          bindString(7, animation_pack)
          bindLong(8, proactive_level)
          bindLong(9, enabled)
          bindLong(10, created_at)
        }
    notifyQueries(777_337_120) { emit ->
      emit("characters")
    }
  }

  public fun updateConversationLastMessage(
    last_message_id: String?,
    unread_count: Long,
    updated_at: Long,
    id: String,
  ) {
    driver.execute(-1_344_408_139, """
        |UPDATE conversations
        |SET last_message_id = ?, unread_count = ?, updated_at = ?
        |WHERE id = ?
        """.trimMargin(), 4) {
          bindString(0, last_message_id)
          bindLong(1, unread_count)
          bindLong(2, updated_at)
          bindString(3, id)
        }
    notifyQueries(-1_344_408_139) { emit ->
      emit("conversations")
    }
  }

  public fun insertConversation(
    id: String,
    character_id: String,
    title: String,
    last_message_id: String?,
    unread_count: Long,
    pinned: Long,
    muted: Long,
    updated_at: Long,
    created_at: Long,
  ) {
    driver.execute(1_369_933_612, """
        |INSERT OR REPLACE INTO conversations(
        |    id, character_id, title, last_message_id, unread_count, pinned, muted, updated_at, created_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, id)
          bindString(1, character_id)
          bindString(2, title)
          bindString(3, last_message_id)
          bindLong(4, unread_count)
          bindLong(5, pinned)
          bindLong(6, muted)
          bindLong(7, updated_at)
          bindLong(8, created_at)
        }
    notifyQueries(1_369_933_612) { emit ->
      emit("conversations")
    }
  }

  public fun insertMessage(
    id: String,
    conversation_id: String,
    sender_type: String,
    sender_id: String,
    message_type: String,
    content_json: String,
    status: String,
    created_at: Long,
    updated_at: Long,
  ) {
    driver.execute(-1_352_036_418, """
        |INSERT OR REPLACE INTO messages(
        |    id, conversation_id, sender_type, sender_id, message_type, content_json, status, created_at, updated_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, id)
          bindString(1, conversation_id)
          bindString(2, sender_type)
          bindString(3, sender_id)
          bindString(4, message_type)
          bindString(5, content_json)
          bindString(6, status)
          bindLong(7, created_at)
          bindLong(8, updated_at)
        }
    notifyQueries(-1_352_036_418) { emit ->
      emit("messages")
    }
  }

  public fun upsertConversationSummary(
    conversation_id: String,
    summary: String,
    message_count: Long,
    updated_at: Long,
  ) {
    driver.execute(-1_935_914_844, """
        |INSERT OR REPLACE INTO conversation_summaries(conversation_id, summary, message_count, updated_at)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, conversation_id)
          bindString(1, summary)
          bindLong(2, message_count)
          bindLong(3, updated_at)
        }
    notifyQueries(-1_935_914_844) { emit ->
      emit("conversation_summaries")
    }
  }

  public fun upsertModelConfig(
    id: String,
    provider: String,
    model_type: String,
    base_url: String,
    api_key_encrypted: String,
    model_name: String,
    enabled: Long,
    updated_at: Long,
  ) {
    driver.execute(2_103_158_732, """
        |INSERT OR REPLACE INTO model_configs(
        |    id, provider, model_type, base_url, api_key_encrypted, model_name, enabled, updated_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindString(0, id)
          bindString(1, provider)
          bindString(2, model_type)
          bindString(3, base_url)
          bindString(4, api_key_encrypted)
          bindString(5, model_name)
          bindLong(6, enabled)
          bindLong(7, updated_at)
        }
    notifyQueries(2_103_158_732) { emit ->
      emit("model_configs")
    }
  }

  public fun upsertVectorStoreConfig(
    id: String,
    provider: String,
    base_url: String,
    api_key_encrypted: String,
    collection_name: String,
    namespace: String?,
    enabled: Long,
    updated_at: Long,
  ) {
    driver.execute(-1_616_304_767, """
        |INSERT OR REPLACE INTO vector_store_configs(
        |    id, provider, base_url, api_key_encrypted, collection_name, namespace, enabled, updated_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindString(0, id)
          bindString(1, provider)
          bindString(2, base_url)
          bindString(3, api_key_encrypted)
          bindString(4, collection_name)
          bindString(5, namespace)
          bindLong(6, enabled)
          bindLong(7, updated_at)
        }
    notifyQueries(-1_616_304_767) { emit ->
      emit("vector_store_configs")
    }
  }

  private inner class SelectMemoriesByCharacterQuery<out T : Any>(
    public val character_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("memory_items", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("memory_items", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(226_489_287,
        """SELECT memory_items.id, memory_items.character_id, memory_items.type, memory_items.content, memory_items.importance, memory_items.keywords_json, memory_items.embedding_json, memory_items.remote_vector_id, memory_items.source_message_id, memory_items.created_at, memory_items.updated_at FROM memory_items WHERE character_id = ? ORDER BY importance DESC, updated_at DESC""",
        mapper, 1) {
      bindString(0, character_id)
    }

    override fun toString(): String = "RhodesDatabase.sq:selectMemoriesByCharacter"
  }

  private inner class SelectMessagesByConversationQuery<out T : Any>(
    public val conversation_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("messages", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("messages", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_955_021_906,
        """SELECT messages.id, messages.conversation_id, messages.sender_type, messages.sender_id, messages.message_type, messages.content_json, messages.status, messages.created_at, messages.updated_at FROM messages WHERE conversation_id = ? ORDER BY created_at ASC""",
        mapper, 1) {
      bindString(0, conversation_id)
    }

    override fun toString(): String = "RhodesDatabase.sq:selectMessagesByConversation"
  }

  private inner class CountMessagesByConversationQuery<out T : Any>(
    public val conversation_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("messages", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("messages", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(671_931_269,
        """SELECT COUNT(*) FROM messages WHERE conversation_id = ?""", mapper, 1) {
      bindString(0, conversation_id)
    }

    override fun toString(): String = "RhodesDatabase.sq:countMessagesByConversation"
  }

  private inner class SelectConversationSummaryQuery<out T : Any>(
    public val conversation_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("conversation_summaries", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("conversation_summaries", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-338_102_345,
        """SELECT summary FROM conversation_summaries WHERE conversation_id = ?""", mapper, 1) {
      bindString(0, conversation_id)
    }

    override fun toString(): String = "RhodesDatabase.sq:selectConversationSummary"
  }
}
