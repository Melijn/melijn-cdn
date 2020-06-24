package me.melijn.cdn.internals.api


import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.header
import io.ktor.request.receiveMultipart
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.put
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import me.melijn.cdn.Container
import me.melijn.cdn.internals.encryption.Base58
import me.melijn.cdn.internals.utils.ByteUtils
import me.melijn.cdn.models.Image
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random


class WebServer(container: Container) {

    private val daoManager = container.daoManager
    private val imageCdnUrl = container.settings.imageCdnUrl
    private val imageDir = container.settings.imageDir
    private val jsonType = ContentType.parse("Application/JSON")

    private val server: NettyApplicationEngine = embeddedServer(Netty, container.settings.port) {
        routing {
            get("/img/{type}") {
                if (!authValidationFails(call)) return@get
                val type = call.parameters["type"] ?: return@get

                val ls = daoManager.imageWrapper.cache[type].await()
                if (ls.isEmpty()) {
                    call.respondText(
                        "{ \"error\": \"The supplied type: '$type' has no images\" }",
                        jsonType,
                        HttpStatusCode(418, "I'm a teapot")
                    )
                    return@get
                }
                val index = Random.nextInt(0, ls.size)
                val fileName = ls[index]
                call.respondText("{ \"url\": \"$imageCdnUrl$fileName\" }", jsonType)
            }

            put("/img/{type}") {
                if (!authValidationFails(call)) return@put
                val type = call.parameters["type"]?.toLowerCase() ?: return@put

                val name = Base58.encode(ByteUtils.longToBytes(System.currentTimeMillis()))

                val multipart = call.receiveMultipart()
                var file = "$name.jpg"

                var id: Long? = null
                multipart.forEachPart { part ->
                    if (part is PartData.FormItem) {
                        if (part.name == "userId") {
                            id = part.value.toLongOrNull() ?: return@forEachPart
                        }
                    } else if (part is PartData.FileItem) {
                        val fname = part.originalFileName ?: return@forEachPart

                        val ext = File(fname).extension
                        file = "$name.$ext"
                        val diskFile = File(imageDir, "$name.$ext")
                        part.streamProvider().use { input ->
                            diskFile.outputStream().buffered().use { output ->
                                input.copyToSuspend(output)
                            }
                        }

                        part.dispose()
                    }
                }

                id?.let { it1 ->
                    daoManager.imageWrapper.insert(
                        Image(type, file, it1, System.currentTimeMillis())
                    )

                } ?: return@put

                call.respondText("{ \"url\": \"$imageCdnUrl$file\" }", jsonType)
            }

            delete("/img/{type}/{filename}") {
                if (!authValidationFails(call)) return@delete
                val type = call.parameters["type"] ?: return@delete
                val filename = call.parameters["filename"] ?: return@delete

                daoManager.imageWrapper.delete(type, filename)
                val success= File(imageDir, filename).delete()


                call.respondText("{ \"deleted\": \"$success\" }", jsonType)
            }

            get("/img/amount/{type}") {
                if (!authValidationFails(call)) return@get
                val type = call.parameters["type"] ?: return@get

                val ls = daoManager.imageWrapper.cache[type].await()

                call.respondText("{ \"amount\": ${ls?.size ?: 0} }", jsonType)
            }
        }
    }

    init {
        server.start(true)
    }


    private suspend fun authValidationFails(ctx: ApplicationCall): Boolean {
        val userId = ctx.request.header("userId")?.toLongOrNull()
        val token = ctx.request.header("token")
        return userId == null || token == null || daoManager.userWrapper.validate(userId, token)
    }

    private suspend fun InputStream.copyToSuspend(
        out: OutputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        yieldSize: Int = 4 * 1024 * 1024,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Long {
        return withContext(dispatcher) {
            val buffer = ByteArray(bufferSize)
            var bytesCopied = 0L
            var bytesAfterYield = 0L
            while (true) {
                val bytes = read(buffer).takeIf { it >= 0 } ?: break
                out.write(buffer, 0, bytes)
                if (bytesAfterYield >= yieldSize) {
                    yield()
                    bytesAfterYield %= yieldSize
                }
                bytesCopied += bytes
                bytesAfterYield += bytes
            }
            return@withContext bytesCopied
        }
    }
}