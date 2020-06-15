package me.melijn.cdn

import me.melijn.cdn.internals.api.WebServer

class CDN {

    init {
        val container = Container()
        WebServer(container)
    }
}

fun main() {
    CDN()
}