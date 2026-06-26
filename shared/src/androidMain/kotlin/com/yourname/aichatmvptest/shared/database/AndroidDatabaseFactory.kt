package com.yourname.aichatmvptest.shared.database

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidDatabaseFactory(
    private val context: Context,
) {
    fun createDatabase(): RhodesDatabase {
        val driver = AndroidSqliteDriver(
            schema = RhodesDatabase.Schema,
            context = context,
            name = "rhodes.db",
        )
        return RhodesDatabase(driver)
    }
}
