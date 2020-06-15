package me.melijn.cdn.internals.database.user

import me.melijn.cdn.internals.database.Dao
import me.melijn.cdn.internals.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "users"
    override val tableStructure: String = "username varchar(64), userId bigint, token varchar(128)"
    override val primaryKey: String = "token"
    override val uniqueKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }

    suspend fun contains(userId: Long, token: String): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT username FROM $table WHERE token = ? AND userId = ?", {rs ->
            it.resume(rs.next())
        }, token, userId)
    }
}