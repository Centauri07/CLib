package me.centauri07.clib.form.field

import net.dv8tion.jda.api.interactions.components.Button

abstract class FormField<T> (var name: String, var required: Boolean = false) {
    abstract var value: T?

    var condition: ((T) -> Boolean)? = null
        private set
    var acknowledgeAction: ((T) -> Unit)? = null
        private set
    var failConditionMessage: String? = null
        private set

    internal var isChosen = required
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

    fun onAcknowledge(action: (T) -> Unit) {
        acknowledgeAction = action
    }
}