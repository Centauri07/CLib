package me.centauri07.clib.configuration

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class Configuration<T>(var model: T, parent: File? = null, name: String) {
    companion object {
        private val configurations = mutableListOf<Configuration<*>>()
    }

    private val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    val file = File(parent ?: File(this::class.java.protectionDomain.codeSource.location.path).parentFile, "$name.json")

    init {
        if (!file.parentFile.exists()) file.parentFile.mkdirs()

        if (!file.exists()) {
            file.createNewFile()
            create()
        }

        load()

        configurations.add(this)
    }

    fun create() {
        model = model!!::class.java.getDeclaredConstructor().newInstance()

        save()
    }

    fun load() {
        val reader = FileReader(file)
        model = gson.fromJson(reader, model!!::class.java)
        reader.close()
    }

    fun save() {
        val writer = FileWriter(file)
        gson.toJson(model, writer)
        writer.close()
    }
}