package com.yourname.aichatmvptest.shared.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Memory_items(
  public val id: String,
  public val character_id: String,
  public val type: String,
  public val content: String,
  public val importance: Double,
  public val keywords_json: String,
  public val embedding_json: String,
  public val remote_vector_id: String?,
  public val source_message_id: String?,
  public val created_at: Long,
  public val updated_at: Long,
)
