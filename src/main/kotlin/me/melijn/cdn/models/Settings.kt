package me.melijn.cdn.models

data class Settings(
    val port: Int,
    val logDays: Int,
    val database: Database,
    val imageDir: String,
    val imageCdnUrl: String
) {

    data class Database(
        var database: String,
        var password: String,
        var user: String,
        var host: String,
        var port: Int
    )

    companion object {
        val instance: Settings = Settings(
            env("PORT", "6969").toInt(),
            env("LOG_DAYS", "31").toInt(),
            Database(
                env("DB_NAME", "melijn_cdn"),
                env("DB_PASS", "/"),
                env("DB_USER", "/"),
                env("DB_HOST", "localhost"),
                env("DB_PORT", "5432").toInt()
            ),
            env("DIR_IMAGE", "/var/www/cdn/img/"),
            env("CDN_URL_IMAGE", "https://cdn.melijn.com/img/")
        )

        private fun env(key: String, defaultValue: String): String {
            val value = System.getenv(key) ?: defaultValue
            return if (value.isEmpty()) {
                defaultValue
            } else {
                value
            }
        }
    }
}