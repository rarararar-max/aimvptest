package com.yourname.aichatmvptest.shared.database

import kotlin.Long
import kotlin.String

public data class Reminder_tasks(
  public val id: String,
  public val character_id: String,
  public val type: String,
  public val title: String,
  public val content: String,
  public val scheduled_at: Long,
  public val repeat_rule: String?,
  public val enabled: Long,
  public val created_at: Long,
)
