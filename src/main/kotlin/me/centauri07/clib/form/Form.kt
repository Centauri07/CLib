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
    val ungroupedFormFields = mutableListOf<FormField<*>>()

    var idle = false

    private var timer: ScheduledFuture<*>? = null

    val submitButton = Button.success("session-submit", "✅ Submit")
    val cancelButton = Button.danger("session-cancel", "❌ Cancel")

    var message: Message? = null

    init {
        for (field in model.fields!!) {
            addField(field)
        }

        startTimer()
    }

    fun getUnacknowledgedField(): FormField<*>? =
        ungroupedFormFields.stream().filter { !it.isAcknowledged }.findFirst().orElse(null)


    internal fun fireEvent(message: Message, button: Button? = null) {
        if (idle) return

        if (message.isFromType(ChannelType.TEXT)) message.delete().queue()

        var field = getUnacknowledgedField() ?: run {
            FormManager.setAcknowledged(member.idLong)

            val builder = EmbedUtility.success(
                "Session Finished",
                "Please click ✅ to submit the form, and react with ❌ to cancel."
            )

            for (field in ungroupedFormFields) {
                if (field.isChosen) builder.addField(field.name, field.value.toString(), false)
            }

            MessageUtility.editOrSendEmbedMessage(
                this.message, formChannel, builder.build()
            ).setActionRow(submitButton, cancelButton).queue {
                this.message = it
            }

            return
        }

        if (!field.required && !field.isChosen) {
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

            val builder = EmbedUtility.success(
                "Session Finished",
                "Please click ✅ to submit the form, and react with ❌ to cancel."
            )

            for (formField in ungroupedFormFields) {
                if (formField.isChosen) builder.addField(formField.name, formField.value.toString(), false)
            }

            MessageUtility.editOrSendEmbedMessage(
                this.message, formChannel, builder.build()
            ).setActionRow(submitButton, cancelButton).queue {
                this.message = it
            }

            return
        }

        if (!field.required && !field.isChosen) {
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
            val builder = EmbedUtility.success(
                "Session Finished",
                "Please click ✅ to submit the form, and react with ❌ to cancel."
            )

            for (field in ungroupedFormFields) {
                if (field.isChosen) builder.addField(field.name, field.value.toString(), false)
            }

            FormManager.setAcknowledged(member.idLong)

            MessageUtility.editOrSendEmbedMessage(message, formChannel,
                builder.build()
            ).setActionRow(submitButton, cancelButton).queue {
                this.message = it
            }

            return
        }

        if (!field.required && !field.isChosen) {
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
        timer?.cancel(true)

        timer = Executors.newSingleThreadScheduledExecutor().schedule(
            {
                model.onSessionExpire(this)
            }, 3, TimeUnit.MINUTES
        )
    }

    fun stopTimer() {
        timer?.cancel(true)
    }

    fun resetTimer() {
        stopTimer()
        startTimer()
    }

    private fun addField(field: FormField<*>) {
        if (field !is GroupFormField) {
            ungroupedFormFields.add(field)
        } else {
            field.fields?.forEach {
                ungroupedFormFields.add(it)
            }
        }
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