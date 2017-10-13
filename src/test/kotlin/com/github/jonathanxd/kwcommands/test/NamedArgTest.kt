package com.github.jonathanxd.kwcommands.test

import com.github.jonathanxd.kwcommands.dsl.argument
import com.github.jonathanxd.kwcommands.dsl.booleanArg
import com.github.jonathanxd.kwcommands.dsl.command
import com.github.jonathanxd.kwcommands.dsl.stringArg
import com.github.jonathanxd.kwcommands.exception.CommandException
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.processor.Processors
import org.junit.Test

class NamedArgTest {

    @Test
    fun test() {
        val printer = CommonPrinter(::println)
        val handler = CommonHelpInfoHandler()

        val cmd = command {
            stringName { "example" }
            arguments {
                +stringArg {
                    id { "directory" }
                    name { "directory" }
                }
                +booleanArg {
                    id { "recursive" }
                    name { "recursive" }
                }
            }
        }

        val infoManager = InformationManagerImpl()
        val manager = CommandManagerImpl()
        val processor = Processors.createCommonProcessor(manager)

        manager.registerCommand(cmd, this)

        try {
            processor.processAndHandle(listOf("example", "--recursive", "x"), this, infoManager)
        } catch (ex: CommandException) {
            handler.handleCommandException(ex, printer)
        }

    }
}