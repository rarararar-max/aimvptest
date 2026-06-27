package com.yourname.aichatmvptest.shared.database.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.yourname.aichatmvptest.shared.database.RhodesDatabase
import com.yourname.aichatmvptest.shared.database.RhodesDatabaseQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<RhodesDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = RhodesDatabaseImpl.Schema

internal fun KClass<RhodesDatabase>.newInstance(driver: SqlDriver): RhodesDatabase =
    RhodesDatabaseImpl(driver)

private class RhodesDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), RhodesDatabase {
  override val rhodesDatabaseQueries: RhodesDatabaseQueries = RhodesDatabaseQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 3

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE characters (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    name TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    description TEXT NOT NULL,
          |    persona_prompt TEXT NOT NULL,
          |    speaking_style TEXT NOT NULL,
          |    voice_style TEXT NOT NULL,
          |    animation_pack TEXT NOT NULL,
          |    proactive_level INTEGER NOT NULL,
          |    enabled INTEGER NOT NULL DEFAULT 1,
          |    created_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE conversations (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    character_id TEXT NOT NULL REFERENCES characters(id),
          |    title TEXT NOT NULL,
          |    last_message_id TEXT,
          |    unread_count INTEGER NOT NULL DEFAULT 0,
          |    pinned INTEGER NOT NULL DEFAULT 0,
          |    muted INTEGER NOT NULL DEFAULT 0,
          |    updated_at INTEGER NOT NULL,
          |    created_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE messages (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    conversation_id TEXT NOT NULL REFERENCES conversations(id),
          |    sender_type TEXT NOT NULL,
          |    sender_id TEXT NOT NULL,
          |    message_type TEXT NOT NULL,
          |    content_json TEXT NOT NULL,
          |    status TEXT NOT NULL,
          |    created_at INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE memory_items (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    character_id TEXT NOT NULL REFERENCES characters(id),
          |    type TEXT NOT NULL,
          |    content TEXT NOT NULL,
          |    importance REAL NOT NULL,
          |    keywords_json TEXT NOT NULL DEFAULT '[]',
          |    embedding_json TEXT NOT NULL DEFAULT '[]',
          |    remote_vector_id TEXT,
          |    source_message_id TEXT,
          |    created_at INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE conversation_summaries (
          |    conversation_id TEXT NOT NULL PRIMARY KEY REFERENCES conversations(id),
          |    summary TEXT NOT NULL,
          |    message_count INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE model_configs (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    provider TEXT NOT NULL,
          |    model_type TEXT NOT NULL,
          |    base_url TEXT NOT NULL,
          |    api_key_encrypted TEXT NOT NULL,
          |    model_name TEXT NOT NULL,
          |    enabled INTEGER NOT NULL DEFAULT 1,
          |    updated_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE vector_store_configs (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    provider TEXT NOT NULL,
          |    base_url TEXT NOT NULL,
          |    api_key_encrypted TEXT NOT NULL,
          |    collection_name TEXT NOT NULL,
          |    namespace TEXT,
          |    enabled INTEGER NOT NULL DEFAULT 0,
          |    updated_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE reminder_tasks (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    character_id TEXT NOT NULL REFERENCES characters(id),
          |    type TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    content TEXT NOT NULL,
          |    scheduled_at INTEGER NOT NULL,
          |    repeat_rule TEXT,
          |    enabled INTEGER NOT NULL DEFAULT 1,
          |    created_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    private fun migrateInternal(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
    ): QueryResult.Value<Unit> {
      if (oldVersion <= 1 && newVersion > 1) {
        driver.execute(null,
            "ALTER TABLE memory_items ADD COLUMN embedding_json TEXT NOT NULL DEFAULT '[]'", 0)
      }
      if (oldVersion <= 2 && newVersion > 2) {
        driver.execute(null, """
            |CREATE TABLE IF NOT EXISTS conversation_summaries (
            |    conversation_id TEXT NOT NULL PRIMARY KEY REFERENCES conversations(id),
            |    summary TEXT NOT NULL,
            |    message_count INTEGER NOT NULL,
            |    updated_at INTEGER NOT NULL
            |)
            """.trimMargin(), 0)
      }
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> {
      var lastVersion = oldVersion

      callbacks.filter { it.afterVersion in oldVersion until newVersion }
      .sortedBy { it.afterVersion }
      .forEach { callback ->
        migrateInternal(driver, oldVersion = lastVersion, newVersion = callback.afterVersion + 1)
        callback.block(driver)
        lastVersion = callback.afterVersion + 1
      }

      if (lastVersion < newVersion) {
        migrateInternal(driver, lastVersion, newVersion)
      }
      return QueryResult.Unit
    }
  }
}
