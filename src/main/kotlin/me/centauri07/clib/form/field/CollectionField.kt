package me.centauri07.clib.form.field

class CollectionField<T: FormField<*>>(private val clazz: Class<T>, name: String, required: Boolean): FormField<T>(name, required) {
    val collection = mutableListOf<T>()

    fun add(type: T) = collection.add(type)
}