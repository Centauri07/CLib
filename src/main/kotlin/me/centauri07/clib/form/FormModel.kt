package me.centauri07.clib.form

abstract class FormModel {
    open fun onSessionFinish(form: Form<*>) { }
    open fun onSessionExpire(form: Form<*>) { }
}