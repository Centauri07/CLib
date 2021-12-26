package me.centauri07.clib

import me.centauri07.clib.configuration.Configuration
import me.centauri07.clib.form.FormManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.io.File

abstract class DiscordBot(
    botName: String,
    token: String,
    vararg gatewayIntents: GatewayIntent,
    parentFile: File? = null
): ListenerAdapter() {
    open fun enable() {}
    open fun disable() {}

    val jda: JDA = JDABuilder.createDefault(token, gatewayIntents.toList()).build().awaitReady()

    val dataFolder: File = parentFile?.let { File(it, botName) } ?: File(File(this::class.java.protectionDomain.codeSource.location.path).parentFile, jda.selfUser.name)

    init {
        FormManager.register(jda)

        if (!dataFolder.parentFile.exists()) dataFolder.parentFile.mkdirs()
        if (!dataFolder.exists()) dataFolder.mkdir()
    }

    fun addEventListener(listener: ListenerAdapter) = jda.addEventListener(listener)
    fun addEventListeners(vararg listeners: ListenerAdapter) = jda.addEventListener(listeners)

    fun <T> createConfiguration(model: T, parent: File?, name: String) = Configuration(model, name, parent)
    fun <T> getConfiguration(name: String) = Configuration.get<T>(name)
}