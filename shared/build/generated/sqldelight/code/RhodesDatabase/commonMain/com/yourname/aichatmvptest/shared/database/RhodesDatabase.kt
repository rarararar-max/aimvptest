package com.yourname.aichatmvptest.shared.database

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.yourname.aichatmvptest.shared.database.shared.newInstance
import com.yourname.aichatmvptest.shared.database.shared.schema
import kotlin.Unit

public interface RhodesDatabase : Transacter {
  public val rhodesDatabaseQueries: RhodesDatabaseQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = RhodesDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): RhodesDatabase =
        RhodesDatabase::class.newInstance(driver)
  }
}
