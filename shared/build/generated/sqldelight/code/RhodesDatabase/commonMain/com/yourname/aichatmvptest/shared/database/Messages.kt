package com.yourname.aichatmvptest.shared.database

import kotlin.Long
import kotlin.String

public data class Messages(
  public val id: String,
  public val conversation_id: String,
  public val sender_type: String,
  public val sender_id: String,
  public val message_type: String,
  public val content_json: String,
  public val status: String,
  public val created_at: Long,
  public val updated_at: Long,
)
