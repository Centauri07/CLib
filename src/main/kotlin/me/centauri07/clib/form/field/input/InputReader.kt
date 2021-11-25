package me.centauri07.clib.form.field.input

import me.centauri07.clib.form.field.input.readers.*

abstract class InputReader<T>(val clazz: Class<T>) {
    abstract val converter: (String) -> T?

    fun read(input: String, condition: ((T) -> Boolean)? = null, message: String?): Result<T> {
        return try {
            val result = converter(input) ?: run {
                return Result.failure(IllegalArgumentException("The input that you've entered doesn't follow the required format."))
            }

            condition?.let { if (!condition(result))
                return Result.failure(IllegalStateException(message)) }

            return Result.success(result)
        } catch (throwable: Throwable) {
            Result.failure(Throwable("The input that you've entered doesn't follow the required format.", throwable))
        }
    }
}

object InputReaderRegistry {

    private val readers = mutableMapOf<Class<*>, InputReader<*>>()

    init {
        add(StringReader())
        add(ColorReader())

        add(ByteReader())
        add(ShortReader())
        add(IntReader())
        add(LongReader())
        add(FloatReader())
        add(DoubleReader())

        add(BooleanReader())
        add(CharReader())

        add(URLReader())
    }

    fun add(reader: InputReader<*>) = readers.put(reader.clazz, reader)
    fun remove(clazz: Class<*>) = readers.remove(clazz)

    fun has(clazz: Class<*>) = get(clazz) != null
    fun <T> get(clazz: Class<T>): InputReader<T>? = readers[clazz] as InputReader<T>?

    inline fun <reified T> ifPresent(func: (InputReader<T>) -> Unit) {
        get(T::class.java)?.let { func(it) }
    }
}