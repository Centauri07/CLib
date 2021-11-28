package me.centauri07.clib.form

import me.centauri07.clib.form.field.ButtonField
import me.centauri07.clib.form.field.FormField
import me.centauri07.clib.form.field.GroupFormField
import me.centauri07.clib.form.field.InputField
import me.centauri07.clib.form.listeners.FormConfirmSubmissionListener
import me.centauri07.clib.form.listeners.FormListener
import me.centauri07.clib.form.listeners.OptionalFieldListener
import me.centauri07.clib.util.EmbedUtility
import me.centauri07.clib.util.MessageUtility
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Form<T: FormModel>(val model: T, val formChannel: MessageChannel, val member: Member) {
    private val groupFormField = GroupFormField("General", true)

    var idle = false

    private lateinit var timer: ScheduledFuture<*>

    val submitButton = Button.success("session-submit", "✅ Submit")
    val cancelButton = Button.danger("session-cancel", "❌ Cancel")

    var message: Message? = null

    init {
        model::class.java.declaredFields.forEach {
            it.isAccessible = true

            if (FormField::class.java.isAssignableFrom(it.type)) groupFormField.add(it.get(model) as FormField<*>)

            startTimer()
        }
    }

    fun getUnacknowledgedField(): FormField<*>? {
        var field = groupFormField.fields?.stream()?.filter { !it.isAcknowledged }?.findFirst()?.orElse(null)

        if (field != null) if (!field.required && !field.chosen) return field

        while (field != null && field is GroupFormField) {
            val childFields = field.fields?.filter { !it.isAcknowledged }?.toMutableList()

            if (childFields == null || childFields.isEmpty()) {
                field.isAcknowledged = true
                field = groupFormField.fields?.stream()?.filter { !it.isAcknowledged }?.findFirst()?.orElse(null)
            } else field = childFields[0]

            if (field != null) if (!field.required && !field.chosen) return field
        }

        return field
    }

    internal fun fireEvent(message: Message, button: Button? = null) {
        if (idle) return

        if (message.isFromType(ChannelType.GROUP)) message.delete().queue()

        var field = getUnacknowledgedField() ?: run {
            FormManager.setAcknowledged(member.idLong)

            MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                EmbedUtility.success(
                    "Session Finished",
                    "Please click ✅ to submit the form, and react with ❌ to cancel."
                ).build()
            ).setActionRow(submitButton, cancelButton).queue {
                this.message = it
            }

            return
        }

        if (!field.required && !field.chosen) {
            MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                EmbedUtility.success(null, "Do you want to enter ${field.name}?").build()
            ).setActionRow(field.yes, field.no).queue {
                this.message = it
            }

            idle = true

            return
        }

        when (field) {
            is InputField<*> -> {
                val result = field.set(message.contentRaw)

                if (result.isFailure) {
                    MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                        EmbedUtility.danger(
                            "Wrong Input",
                            result.exceptionOrNull()?.message
                        ).build()
                    ).queue {
                        this.message = it

                        idle = true

                        Executors.newSingleThreadScheduledExecutor().schedule(
                            {
                                idle = false

                                MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                                    EmbedUtility.success(
                                        null,
                                        "Enter - " + field.name
                                    ).build()
                                ).queue { msg ->
                                    this.message = msg
                                }
                            }, 5, TimeUnit.SECONDS
                        )
                    }

                    return
                }
            }

            is ButtonField -> {
                if (field.messageId == message.idLong)
                    if (!field.setChosenButton(button!!)) {
                        MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                            EmbedUtility.danger(
                                "You cannot pick that!",
                                field.failConditionMessage
                            ).build()
                        ).queue {
                            this.message = it

                            idle = true

                            Executors.newSingleThreadScheduledExecutor().schedule(
                                {
                                    idle = false

                                    MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                                        EmbedUtility.success(
                                            null,
                                            "${field.name} - Please select an option."
                                        ).build()
                                    ).setActionRow((field as ButtonField).buttonList).queue { msg ->
                                        (field as ButtonField).messageId = msg.idLong
                                        this.message = msg
                                    }

                                }, 5, TimeUnit.SECONDS
                            )
                        }

                        return
                    }
            }
        }

        field.isAcknowledged = true

        field = getUnacknowledgedField() ?: run {
            FormManager.setAcknowledged(member.idLong)

            MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                EmbedUtility.success(
                    "Session Finished",
                    "Please click ✅ to submit the form, and react with ❌ to cancel."
                ).also {
                    // TODO add the answers as a field
                }.build()
            ).setActionRow(submitButton, cancelButton).queue {
                this.message = it
            }

            return
        }

        if (!field.required && !field.chosen) {
            MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                EmbedUtility.success(null, "Do you want to enter ${field.name}?").build()
            ).setActionRow(field.yes, field.no).queue {
                this.message = it
            }

            idle = true

            return
        }

        when (field) {
            is InputField<*> -> {
                MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                    EmbedUtility.success(
                        null,
                        "Enter - " + field.name
                    ).build()
                ).queue {
                    this.message = it
                }
            }

            is ButtonField -> {
                val buttonChunked = field.buttonList.chunked(5)

                val actionRows = mutableListOf<ActionRow>()

                buttonChunked.forEach {
                    actionRows.add(ActionRow.of(it))
                }

                MessageUtility.editOrSendEmbedMessage(this.message, formChannel,
                    EmbedUtility.success(
                        null,
                        "${field.name} - Please select an option."
                    ).build()
                ).setActionRows(actionRows).queue {
                    field.messageId = it.idLong
                    this.message = it
                }
            }
        }
    }

    internal fun startSession() {
        if (idle) return

        val field = getUnacknowledgedField() ?: run {
            FormManager.setAcknowledged(member.idLong)

            MessageUtility.editOrSendEmbedMessage(message, formChannel,
                EmbedUtility.success(
                    "Session Finished",
                    "Please click ✅ to submit the form, and react with ❌ to cancel."
                ).build()
            ).setActionRow(submitButton, cancelButton).queue {
                this.message = it
            }

            return
        }

        if (!field.required && !field.chosen) {
            MessageUtility.editOrSendEmbedMessage(message, formChannel,
                EmbedUtility.success(null, "Do you want to enter ${field.name}?").build()
            ).setActionRow(field.yes, field.no).queue {
                this.message = it
            }

            idle = true

            return
        }

        when (field) {
            is InputField<*> -> {
                MessageUtility.editOrSendEmbedMessage(message, formChannel,
                    EmbedUtility.success(
                        null,
                        "Enter - " + field.name
                    ).build()
                ).queue {
                    this.message = it
                }
            }

            is ButtonField -> {
                val buttonChunked = field.buttonList.chunked(5)

                val actionRows = mutableListOf<ActionRow>()

                buttonChunked.forEach {
                    actionRows.add(ActionRow.of(it))
                }

                MessageUtility.editOrSendEmbedMessage(message, formChannel,
                    EmbedUtility.success(
                        null,
                        "${field.name} - Please select an option."
                    ).build()
                ).setActionRows(actionRows).queue {
                    field.messageId = it.idLong
                    this.message = it
                }
            }
        }
    }

    fun startTimer() {
        timer = Executors.newSingleThreadScheduledExecutor().schedule(
            {
                model.onSessionExpire(this)
            }, 3, TimeUnit.SECONDS
        )
    }

    fun stopTimer() {
        timer.cancel(true)
    }

    fun resetTimer() {
        stopTimer()
        startTimer()
    }
}

object FormManager {
    fun register(jda: JDA) {
        jda.addEventListener(
            FormListener(),
            FormConfirmSubmissionListener(),
            OptionalFieldListener()
        )
    }

    private val FORMS = mutableListOf<Form<*>>()

    fun <T: FormModel> createForm(model: T, formChannel: MessageChannel, member: Member): Form<T>? {
        if (!hasForm(member.idLong)) {
            FORMS.add(Form(model, formChannel, member))
            val form = getForm(member.idLong)
            form?.startSession()
            return form as Form<T>
        }

        return null
    }
    fun removeSession(id: Long) = FORMS.removeIf { it.member.idLong == id }

    fun getForm(id: Long): Form<*>? = FORMS.stream().filter { it.member.idLong == id }.findFirst().orElse(null)
    fun hasForm(id: Long) = getForm(id) != null

    private val ACKNOWLEDGED_FORMS = mutableListOf<Form<*>>()

    fun setAcknowledged(id: Long) {
        if (hasForm(id) && !hasAcknowledgedForm(id)) {
            val form: Form<*> = getForm(id) ?: return
            FORMS.remove(form)
            ACKNOWLEDGED_FORMS.add(form)
        }
    }

    fun removeAcknowledgedForm(id: Long) = ACKNOWLEDGED_FORMS.removeIf { it.member.idLong == id }

    fun getAcknowledgedForm(id: Long): Form<*>? = ACKNOWLEDGED_FORMS.stream().filter { it.member.idLong == id }.findFirst().orElse(null)
    fun hasAcknowledgedForm(id: Long) = getAcknowledgedForm(id) != null
}