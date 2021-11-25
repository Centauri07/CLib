package me.centauri07.clib.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ComponentLayout
import net.dv8tion.jda.api.requests.restaction.MessageAction
import java.awt.Color
import java.time.Instant

object EmbedUtility {
    fun main(title: String? = null, description: String? = null, color: Color): EmbedBuilder =
        EmbedBuilder().apply {
            setColor(color)
            setTitle(title)
            setDescription(description)
            setTimestamp(Instant.now())
        }


    fun success(title: String? = null, description: String? = null): EmbedBuilder =
        main(title, description, color = Color.GREEN)

    fun danger(title: String? = null, description: String? = null): EmbedBuilder =
        main(title, description, color = Color.RED)

}

object ButtonUtility {
    fun disableButton(message: Message): List<ActionRow> {
        val components: List<ActionRow> = ArrayList<ActionRow>(message.actionRows)

        for (button in message.buttons) {
            ComponentLayout.updateComponent(components, button.id!!, button.asDisabled())
        }

        return components
    }
}

object MessageUtility {
    fun editOrSendEmbedMessage(message: Message?, channel: MessageChannel, messageEmbed: MessageEmbed): MessageAction {
        message?.let {
            if (it.isFromType(ChannelType.PRIVATE)) return channel.sendMessageEmbeds(messageEmbed)
        }

        return message?.editMessageEmbeds(messageEmbed) ?: channel.sendMessageEmbeds(messageEmbed)
    }
}