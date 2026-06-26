package com.yourname.aichatmvptest.shared.database

import kotlin.Long
import kotlin.String

public data class Model_configs(
  public val id: String,
  public val provider: String,
  public val model_type: String,
  public val base_url: String,
  public val api_key_encrypted: String,
  public val model_name: String,
  public val enabled: Long,
  public val updated_at: Long,
)
