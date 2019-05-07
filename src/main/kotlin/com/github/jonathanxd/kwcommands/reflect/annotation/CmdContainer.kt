package com.github.jonathanxd.kwcommands.reflect.annotation

/**
 * Provides [CommandContainer] of running command.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class CmdContainer