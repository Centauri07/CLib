package me.centauri07.clib.form

import me.centauri07.clib.form.field.GroupFormField

abstract class FormModel: GroupFormField("General", true) {
    open fun onSessionFinish(form: Form<*>) { }
    open fun onSessionExpire(form: Form<*>) { }
}