package com.melodify.desktop
fun main() {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.jpg")
    println("Stream is: " + stream)
    val iconPng = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
    println("icon.png is: " + iconPng)
}
