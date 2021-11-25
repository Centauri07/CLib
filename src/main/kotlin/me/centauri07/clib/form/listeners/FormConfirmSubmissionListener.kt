package me.centauri07.clib.form.listeners

import me.centauri07.clib.form.Form
import me.centauri07.clib.form.FormManager
import me.centauri07.clib.util.ButtonUtility
import me.centauri07.clib.util.EmbedUtility
import me.centauri07.clib.util.MessageUtility
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class FormConfirmSubmissionListener: ListenerAdapter() {
    override fun onButtonClick(event: ButtonClickEvent) {
        val acknowledgedForm: Form<*> = FormManager.getAcknowledgedForm(event.user.idLong) ?: return
        if (event.button == acknowledgedForm.submitButton) {
            event.deferEdit().setActionRows(ButtonUtility.disableButton(event.message!!)).queue()
            (acknowledgedForm.model).onSessionFinish(acknowledgedForm)
            MessageUtility.editOrSendEmbedMessage(acknowledgedForm.message, acknowledgedForm.formChannel,
                EmbedUtility.success(
                    "Session Success",
                    "Session has been finished successfully."
                ).build()
            ).queue {
                acknowledgedForm.message = it
            }
            acknowledgedForm.stopTimer()
            FormManager.removeAcknowledgedForm(acknowledgedForm.member.idLong)
        } else if (event.button == acknowledgedForm.cancelButton) {
            event.deferEdit().setActionRows(ButtonUtility.disableButton(event.message!!)).queue()
            MessageUtility.editOrSendEmbedMessage(acknowledgedForm.message, acknowledgedForm.formChannel,
                EmbedUtility.danger(
                    "Successfully Canceled",
                    "Session has been canceled successfully."
                ).build()
            ).queue {
                acknowledgedForm.message = it
            }
            acknowledgedForm.stopTimer()
            FormManager.removeAcknowledgedForm(acknowledgedForm.member.idLong)
        }
    }
}