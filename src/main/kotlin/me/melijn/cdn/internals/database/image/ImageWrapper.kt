package me.melijn.cdn.internals.database.image

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.coroutines.future.await
import me.melijn.cdn.models.Image
import me.melijn.melijnbot.objects.threading.TaskManager
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ImageWrapper(private val taskManager: TaskManager, private val imageDao: ImageDao) {

    val cache = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build(loadingCacheFrom<String, List<String>> { key ->
            getImagesByType(key)
        })

    private val logger = LoggerFactory.getLogger(ImageWrapper::class.java)

    private fun getImagesByType(key: String): CompletableFuture<List<String>> {
        val future = CompletableFuture<List<String>>()
        taskManager.async {
            val list = imageDao.getImages(key)
            future.complete(list)
        }
        return future
    }

    suspend fun insert(image: Image) {
        val ls = cache[image.type].await().toMutableList()
        ls.add(image.name)
        cache.put(image.type, CompletableFuture.completedFuture(ls))

        imageDao.insert(image)
        logger.info("inserted image type: ${image.type}, name: ${image.name}, owner: ${image.ownerId}")
    }

    suspend fun delete(type: String, name: String) {
        val ls = cache[type].await().toMutableList()
        ls.remove(name)
        cache.put(type, CompletableFuture.completedFuture(ls))

        imageDao.delete(type, name)
        logger.info("deleted image type: $type, name: $name")
    }
}

//EPIC CODE DO NOT TOUCH
fun <K, V> loadingCacheFrom(function: (K) -> CompletableFuture<V>): CacheLoader<K, CompletableFuture<V>> {
    return CacheLoader.from { k ->
        if (k == null) throw IllegalArgumentException("BRO CRINGE")
        function.invoke(k)
    }
}