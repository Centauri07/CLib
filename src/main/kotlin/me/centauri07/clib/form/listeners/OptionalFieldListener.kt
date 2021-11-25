package me.centauri07.clib.form.listeners

import me.centauri07.clib.form.Form
import me.centauri07.clib.form.FormManager
import me.centauri07.clib.form.field.FormField
import me.centauri07.clib.util.ButtonUtility
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class OptionalFieldListener: ListenerAdapter() {
    override fun onButtonClick(event: ButtonClickEvent) {
        val form: Form<*> = FormManager.getForm(event.user.idLong) ?: return
        val formField: FormField<*> = form.getUnacknowledgedField() ?: return

        if (form.idle) {
            if (!formField.required && !formField.chosen) {
                if (event.button == formField.yes) {
                    event.deferEdit().setActionRows(ButtonUtility.disableButton(event.message!!)).queue()
                    form.resetTimer()
                    formField.chosen = true
                    form.idle = false
                    form.startSession()
                } else if (event.button == formField.no) {
                    event.deferEdit().setActionRows(ButtonUtility.disableButton(event.message!!)).queue()
                    form.resetTimer()
                    formField.isAcknowledged = true
                    form.idle = false
                    form.startSession()
                }
            }
        }
    }
}