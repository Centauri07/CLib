package me.centauri07.clib.form.field

import me.centauri07.clib.form.field.input.InputReaderRegistry
import java.lang.IllegalArgumentException

open class InputField<T>(clazz: Class<T>, name: String, required: Boolean = false): FormField<T>(name, required) {
    var value: T? = null
        private set

    private val reader = InputReaderRegistry.get(clazz)

    init {
        if (reader == null) throw IllegalArgumentException("There is no reader found for ${clazz.name}")
    }

    fun set(input: String): Result<T> {
        return reader!!.read(input, condition, failConditionMessage).also{
            if (it.isSuccess) value = it.getOrNull()
        }
    }
}