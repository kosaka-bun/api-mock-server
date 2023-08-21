package de.honoka.server.apimock

import de.honoka.sdk.util.code.ThrowsRunnable
import de.honoka.sdk.util.system.gui.ConsoleWindow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiMockServerApplication

fun main(args: Array<String>) {
    ConsoleWindow.Builder.of().apply {
        windowName = "API Mock Server"
        screenZoomScale = 1.25
        onExit = ThrowsRunnable {}
    }.build()
    runApplication<ApiMockServerApplication>(*args)
}