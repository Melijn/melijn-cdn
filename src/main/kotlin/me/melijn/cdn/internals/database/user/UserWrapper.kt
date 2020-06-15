package me.melijn.cdn.internals.database.user

class UserWrapper(private val userDao: UserDao) {

    suspend fun validate(userId: Long, token: String): Boolean {
        return userDao.contains(userId, token)
    }
}