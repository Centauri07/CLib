package me.centauri07.clib.form.listeners

import me.centauri07.clib.form.Form
import me.centauri07.clib.form.FormManager
import me.centauri07.clib.form.field.ButtonField
import me.centauri07.clib.util.ButtonUtility
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class FormListener: ListenerAdapter() {
    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        if (event.author.isBot) return

        if (FormManager.hasForm(event.author.idLong)) {
            val form: Form<*> = FormManager.getForm(event.author.idLong) ?: return
            if (form.formChannel.id != event.channel.id) return
            form.fireEvent(event.message)
        }
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val form: Form<*> = FormManager.getForm(event.author.idLong) ?: return
        if (form.formChannel.id != event.channel.id) return
        form.fireEvent(event.message)
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        val form: Form<*> = FormManager.getForm(event.user.idLong) ?: return
        if (form.formChannel.id != event.channel.id) return
        val field = form.getUnacknowledgedField()
        if (field is ButtonField && field.messageId == event.messageIdLong) {
            event.message?.let {
                event.deferEdit().setActionRows(ButtonUtility.disableButton(it)).queue()
                form.fireEvent(it, event.button)
            } ?: return
        }
    }
}