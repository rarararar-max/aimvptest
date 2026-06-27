package com.yourname.aichatmvptest.shared.database

import kotlin.Long
import kotlin.String

public data class Conversation_summaries(
  public val conversation_id: String,
  public val summary: String,
  public val message_count: Long,
  public val updated_at: Long,
)
