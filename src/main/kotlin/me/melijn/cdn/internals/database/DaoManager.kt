package me.melijn.cdn.internals.database

import me.melijn.cdn.internals.database.image.ImageDao
import me.melijn.cdn.internals.database.image.ImageWrapper
import me.melijn.cdn.internals.database.user.UserDao
import me.melijn.cdn.internals.database.user.UserWrapper
import me.melijn.cdn.models.Settings
import me.melijn.melijnbot.objects.threading.TaskManager

class DaoManager(taskManager: TaskManager, dbSettings: Settings.Database) {

    val userWrapper: UserWrapper
    val imageWrapper: ImageWrapper

    var driverManager: DriverManager

    init {
        driverManager = DriverManager(dbSettings)

        userWrapper = UserWrapper(UserDao(driverManager))
        imageWrapper = ImageWrapper(taskManager, ImageDao(driverManager))

        //After registering wrappers
        driverManager.executeTableRegistration()
        for (func in afterTableFunctions) {
            func()
        }
    }

    companion object {
        val afterTableFunctions = mutableListOf<() -> Unit>()
    }
}