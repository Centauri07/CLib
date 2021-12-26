package me.centauri07.clib.form.field

open class GroupFormField(name: String, required: Boolean = false): FormField<GroupFormField>(name, required) {
    var fields: MutableList<FormField<*>>? = null

    fun add(vararg fields: FormField<*>): GroupFormField {
        this.fields?.addAll(fields) ?: run { this.fields = mutableListOf(*fields) }
        return this
    }

    fun <T: FormField<*>> add(field: T): T {
        this.fields?.add(field) ?: run { this.fields = mutableListOf(field) }
        return field
    }

    fun remove(name: String) {
        fields?.removeIf { it.name == name }
    }

    override var value: GroupFormField? = this
}