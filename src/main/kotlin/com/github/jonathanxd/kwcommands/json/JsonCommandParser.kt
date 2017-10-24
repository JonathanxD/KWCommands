/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 JonathanxD
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.kwcommands.json

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentBuilder
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandBuilder
import com.github.jonathanxd.kwcommands.command.CommandName
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.requirement.RequirementBuilder
import com.github.jonathanxd.kwcommands.util.PossibilitiesFunc
import com.github.jonathanxd.kwcommands.util.Transformer
import com.github.jonathanxd.kwcommands.util.Validator
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

/**
 * Parser of JSON Commands
 */
interface JsonCommandParser {
    /**
     * Resolver used to resolve classes and objects from input json values.
     */
    val typeResolver: TypeResolver

    /**
     * Factory that can be used to create another instance of the same parser type.
     */
    val factory: JsonCommandParserFactory

    /**
     * Parse command from [jsonObject] and specifies [superCommand] as the super command of
     * created [Command]
     */
    fun parseCommand(jsonObject: JSONObject, superCommand: Command?): Command

    /**
     * Parses argument from [jsonObject].
     */
    fun parseArgument(jsonObject: JSONObject): Argument<*>

    /**
     * Parses [Information id][Information.Id] from [jsonObject]
     */
    fun parseId(jsonObject: JSONObject): Information.Id<*>

    /**
     * Parses [Information] from [jsonObject]
     */
    fun parseInfo(jsonObject: JSONObject): Information<*>

    /**
     * Parses [RequiredInformation] from [jsonObject]
     */
    fun parseReqInfo(jsonObject: JSONObject): RequiredInformation

    /**
     * Parses [Requirement] from [jsonObject].
     */
    fun parseReq(jsonObject: JSONObject): Requirement<*, *>

    /**
     * Parses [Command] from [jsonObject].
     */
    fun parseCommand(jsonObject: JSONObject): Command = this.parseCommand(jsonObject, null)

    /**
     * Parses [Command] from [json text][json].
     */
    fun parseCommand(json: String): Command = this.parseCommand(json, null)

    /**
     * Parses [Command] from [json text][json] and use [superCommand] as super command of parsed [Command].
     */
    fun parseCommand(json: String, superCommand: Command?): Command =
            this.parseCommand(JSONParser().parse(json) as JSONObject, superCommand)

    /**
     * Parses [Argument] from [json text][json].
     */
    fun parseArgument(json: String): Argument<*> = this.parseArgument(JSONParser().parse(json) as JSONObject)

    /**
     * Parses [Information id][Information.Id] from [json text][json].
     */
    fun parseId(json: String): Information.Id<*> = this.parseId(JSONParser().parse(json) as JSONObject)

    /**
     * Parses [Information] from [json text][json].
     */
    fun parseInfo(json: String): Information<*> = this.parseInfo(JSONParser().parse(json) as JSONObject)

    /**
     * Parses [RequiredInformation] from [json text][json].
     */
    fun parseReqInfo(json: String): RequiredInformation = this.parseReqInfo(JSONParser().parse(json) as JSONObject)

    /**
     * Parses [Requirement] from [json text][json].
     */
    fun parseReq(json: String): Requirement<*, *> = this.parseReq(JSONParser().parse(json) as JSONObject)

    /**
     * Parses [Command handler][Handler] from input text [input].
     */
    fun parseCommandHandler(input: String): Handler?

    /**
     * Parses [Argumemt handler][ArgumentHandler] from input text [input].
     */
    fun parseArgumentHandler(input: String): ArgumentHandler<*>?
}

/**
 * Factory of [JsonCommandParser].
 */
interface JsonCommandParserFactory {
    /**
     * Creates a [JsonCommandParser], the returned instance should use [typeResolver] to work
     * well with Reflection System.
     */
    fun create(typeResolver: TypeResolver): JsonCommandParser
}

/**
 * [JsonCommandParserFactory] of [DefaultJsonParser]
 */
object DefaultJsonParserFactory : JsonCommandParserFactory {
    override fun create(typeResolver: TypeResolver): JsonCommandParser =
            DefaultJsonParser(typeResolver)

}

/**
 * Default command json parser, see format [here][https://github.com/JonathanxD/KWCommands/wiki/Default-Format]
 */
class DefaultJsonParser(override val typeResolver: TypeResolver) : JsonCommandParser {
    private val parser = JSONParser()

    override val factory: JsonCommandParserFactory
        get() = DefaultJsonParserFactory

    override fun parseCommand(jsonObject: JSONObject, superCommand: Command?): Command =
            CommandBuilder()
                    .parent(superCommand)
                    .name(CommandName.name(jsonObject.getRequired("name")))
                    .description(jsonObject.getRequired("description"))
                    .addAlias(jsonObject.getAs<JSONArray>("alias")?.map { CommandName.name(it as String) }.orEmpty())
                    .handler(jsonObject.getCommandHandler("handler", this))
                    .addArguments(jsonObject.getAsArrayOfObj("arguments") { this.parseArgument(it) })
                    .addRequirements(jsonObject.getAsArrayOfObj("requirements") { this.parseReq(it) })
                    .addRequiredInfo(jsonObject.getAsArrayOfObj("requiredInfo") { this.parseReqInfo(it) })
                    .build()
                    .also { sup ->
                        sup.addSubCommands(jsonObject.getAsArrayOfObj("subcommands") { this.parseCommand(it, sup) })
                    }

    override fun parseArgument(jsonObject: JSONObject): Argument<*> {
        val type = this.typeResolver.resolve(jsonObject.getRequired("type"))

        return ArgumentBuilder<Any?>()
                .type(type as TypeInfo<out Any?>)
                .id(jsonObject.getRequired<String>("id"))
                .name(jsonObject.getAs("name") ?: jsonObject.getRequired("id"))
                .optional(jsonObject.getAs("optional") ?: false)
                .validator(jsonObject.getAsSingleton<Validator>("validator", this.typeResolver)
                        ?: this.typeResolver.resolveValidator(type))
                .transformer(jsonObject.getAsSingleton<Transformer<Any?>>("transformer", this.typeResolver)
                        ?: this.typeResolver.resolveTransformer(type))
                .possibilities(jsonObject.getAsSingleton<PossibilitiesFunc>("possibilities", this.typeResolver)
                        ?: this.typeResolver.resolvePossibilitiesFunc(type))
                .defaultValue(this.typeResolver.resolveDefaultValue(type))
                .handler(jsonObject.getArgumentHandler("handler", this))
                .addRequirements(jsonObject.getAsArrayOfObj("requirements") { this.parseReq(it) })
                .addRequiredInfo(jsonObject.getAsArrayOfObj("requiredInfo") { this.parseReqInfo(it) })
                .build()

    }

    override fun parseReq(jsonObject: JSONObject): Requirement<*, *> =
            RequirementBuilder<Any?, String>()
                    .type(TypeInfo.of(String::class.java))
                    .subject(this.parseId(jsonObject.getRequired<JSONObject>("info")))
                    .tester(jsonObject.getAsSingletonReq("tester", this.typeResolver))
                    .required(jsonObject.getRequired("data"))
                    .build()

    override fun parseReqInfo(jsonObject: JSONObject): RequiredInformation =
            RequiredInformation(
                    id = this.parseId(jsonObject.getRequired<JSONObject>("id")),
                    useProviders = jsonObject.getAs<Boolean>("useProviders") ?: true
            )

    override fun parseId(jsonObject: JSONObject): Information.Id<*> =
            Information.Id(
                    tags = jsonObject.getAsArrayOfStr("tags").toTypedArray(),
                    type = this.typeResolver.resolve(jsonObject.getRequired("type")) as TypeInfo<out Any?>
            )

    override fun parseInfo(jsonObject: JSONObject): Information<*> = Information(
            this.parseId(jsonObject.getRequired<JSONObject>("id")),
            jsonObject.getAsSingletonReq<() -> Any?>("provider", this.typeResolver).invoke(),
            jsonObject.getRequired<String>("description")
    )

    override fun parseCommand(json: String): Command =
            this.parseCommand(this.parser.parse(json) as JSONObject)

    override fun parseArgument(json: String): Argument<*> =
            this.parseArgument(this.parser.parse(json) as JSONObject)

    override fun parseId(json: String): Information.Id<*> =
            this.parseId(this.parser.parse(json) as JSONObject)

    override fun parseInfo(json: String): Information<*> =
            this.parseInfo(this.parser.parse(json) as JSONObject)

    override fun parseReq(json: String): Requirement<*, *> =
            this.parseReq(this.parser.parse(json) as JSONObject)

    override fun parseCommandHandler(input: String): Handler? =
            resolveCommandHandler(input, this.typeResolver)

    override fun parseArgumentHandler(input: String): ArgumentHandler<*>? =
            resolveArgumentHandler(input, this.typeResolver)

}
