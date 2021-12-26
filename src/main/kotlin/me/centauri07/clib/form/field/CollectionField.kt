package me.centauri07.clib.form.field

class CollectionField<T: FormField<*>>(private val clazz: Class<T>, name: String, required: Boolean): FormField<T>(name, required) {
    var values: MutableList<T>? = null
    override var value: T? = null

    fun add(type: T) = values?.add(type) ?: run { values = mutableListOf<T>().also { it.add(type) } }
}