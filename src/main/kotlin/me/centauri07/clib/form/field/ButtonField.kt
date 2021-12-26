package me.centauri07.clib.form.field

import net.dv8tion.jda.api.interactions.components.Button

class ButtonField(name: String, required: Boolean = false): FormField<ButtonChoice>(name, required) {
    var messageId: Long = 0

    internal var buttonList = mutableListOf<Button>()
        private set

    fun of(vararg buttonChoices: ButtonChoice): ButtonField {
        buttonList = buttonChoices.map { Button.primary("FBF-${it.id}", it.name) }.toMutableList()
        return this
    }

    fun add(vararg buttonChoices: ButtonChoice): ButtonField {
        buttonList.addAll(buttonChoices.map { Button.primary("FBF-${it.id}", it.name) }.toMutableList())
        return this
    }

    fun setChosenButton(button: Button): Boolean  {
        val buttonChoice = ButtonChoice(button.id!!.removeRange(0, 4), button.label)

        condition?.let {
            if (!it(buttonChoice))
            return false
        }

        this.value = buttonChoice
        return true
    }

    override var value: ButtonChoice? = null
}

data class ButtonChoice(val id: String, val name: String)