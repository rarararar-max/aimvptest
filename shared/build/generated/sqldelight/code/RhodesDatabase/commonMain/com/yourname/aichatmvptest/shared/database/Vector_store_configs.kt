package com.yourname.aichatmvptest.shared.database

import kotlin.Long
import kotlin.String

public data class Vector_store_configs(
  public val id: String,
  public val provider: String,
  public val base_url: String,
  public val api_key_encrypted: String,
  public val collection_name: String,
  public val namespace: String?,
  public val enabled: Long,
  public val updated_at: Long,
)
