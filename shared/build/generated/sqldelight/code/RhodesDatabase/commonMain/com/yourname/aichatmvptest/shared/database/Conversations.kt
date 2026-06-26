package com.yourname.aichatmvptest.shared.database

import kotlin.Long
import kotlin.String

public data class Conversations(
  public val id: String,
  public val character_id: String,
  public val title: String,
  public val last_message_id: String?,
  public val unread_count: Long,
  public val pinned: Long,
  public val muted: Long,
  public val updated_at: Long,
  public val created_at: Long,
)
