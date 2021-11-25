package me.centauri07.clib.form.field

import net.dv8tion.jda.api.interactions.components.Button

abstract class FormField<T> (var name: String, var required: Boolean = false) {
    var condition: ((T) -> Boolean)? = null
    var failConditionMessage: String? = null

    internal var chosen = required
    internal var isAcknowledged = false

    internal var yes: Button? = null
        private set
    internal var no: Button? = null
        private set

    init {
        if (!required) {
            yes = Button.success("yes", "✅ Yes")
            no = Button.danger("no", "❌ No")
        }
    }

    fun continueIf(condition: (T) -> Boolean, message: String) {
        this.condition = condition
        this.failConditionMessage = message
    }
}