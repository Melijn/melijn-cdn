package me.melijn.cdn.internals.database.image

import me.melijn.cdn.internals.database.Dao
import me.melijn.cdn.internals.database.DriverManager
import me.melijn.cdn.models.Image
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ImageDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "images"
    override val tableStructure: String = "type varchar(32), name varchar(64), ownerId bigint, timeAdded bigint"
    override val primaryKey: String = "type, name"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun insert(image: Image) {
        driverManager.executeUpdate(
            "INSERT INTO $table (type, name, ownerId, timeAdded) VALUES (?, ?, ?, ?)",
            image.type, image.name, image.ownerId, image.createdTime
        )
    }

    suspend fun delete(type: String, name: String) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE type = ? AND name = ?",
            type, name
        )
    }

    suspend fun getImages(type: String): List<String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE type = ?", { rs ->
            val ls = mutableListOf<String>()
            while (rs.next()) {
                ls.add(rs.getString("name"))
            }
            it.resume(ls)
        }, type)
    }
}