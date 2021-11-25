package me.centauri07.clib.form

abstract class FormModel {
    abstract fun onSessionFinish(form: Form<*>)
    open fun onSessionExpire(form: Form<*>) { }
}