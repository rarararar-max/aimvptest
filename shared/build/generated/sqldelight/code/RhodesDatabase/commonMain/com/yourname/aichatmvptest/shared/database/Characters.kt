package com.yourname.aichatmvptest.shared.database

import kotlin.Long
import kotlin.String

public data class Characters(
  public val id: String,
  public val name: String,
  public val title: String,
  public val description: String,
  public val persona_prompt: String,
  public val speaking_style: String,
  public val voice_style: String,
  public val animation_pack: String,
  public val proactive_level: Long,
  public val enabled: Long,
  public val created_at: Long,
)
