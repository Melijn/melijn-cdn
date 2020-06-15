package me.melijn.cdn

import me.melijn.cdn.internals.database.DaoManager
import me.melijn.cdn.models.Settings
import me.melijn.melijnbot.objects.threading.TaskManager

class Container {

    val taskManager = TaskManager()
    val settings = Settings.instance

    val daoManager: DaoManager = DaoManager(taskManager, settings.database)
}