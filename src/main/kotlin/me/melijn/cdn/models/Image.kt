package me.melijn.cdn.models

data class Image(
    val type: String,   // cat, dog, penguin
    val name: String,        // filename.png
    val ownerId: Long,       // discord id
    val createdTime: Long    // millis
)