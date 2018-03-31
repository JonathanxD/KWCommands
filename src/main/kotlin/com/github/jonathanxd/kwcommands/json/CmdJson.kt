/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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

import com.github.jonathanxd.kwcommands.reflect.util.get
import java.lang.reflect.AnnotatedElement
import kotlin.reflect.KClass

/**
 * Specifies a JSON String or a JSON Resource that specifies command properties.
 *
 * When on a `type` or `function`, the json should follow [Command JSON][https://github.com/JonathanxD/KWCommands/wiki/Default-Format#commands],
 * when on a field, the json should follow [Argument JSON][https://github.com/JonathanxD/KWCommands/wiki/Default-Format#arguments],
 * remember that these formats only applies to default [parser].
 *
 * ## Commands
 *
 *  ```
 * | Key             | Data type             | Description                                           |
 * | --------------- | --------------------- | ----------------------------------------------------- |
 * | name            | string                | Name of the command                                   |
 * | nameComponent?  | string                | Name component of the command                         |
 * | description²    | string                | Description of command (what it does)                 |
 * | alias?          | string[]              | Aliases to command                                    |
 * | aliasComponent? | string                | Aliases component                                     |
 * | locale?³        | string                | Locale of command                                     |
 * | arguments?      | object[]              | Arguments of command (see below)                      |
 * | requirements?   | object[]              | Requirements of command (see below)                   |
 * | requiredInfo?   | object[]              | Required information ids (see below)                  |
 * | handler?¹       | string                | Qualified name of command handler                     |
 * | subCommands?    | object[] or string[]  | Sub commands, either this format or resource location |
 * ```
 *
 * ## Arguments
 *
 *  ```
 * | Key             | Data type | Description                                                |
 * | --------------- | --------- | ---------------------------------------------------------- |
 * | type¹           | string    | Qualified name of argument type                            |
 * | id              | string    | Argument identification                                    |
 * | name?           | string    | Argument name (set to id if not specified)                 |
 * | nameComponent?  | string    | Name component of the argument                             |
 * | description?²   | string    | Argument description (since 1.1.5)                         |
 * | alias?          | string[]  | Aliases to argument                                        |
 * | aliasComponent? | string    | Aliases component                                          |
 * | locale?³        | string    | Locale of argument                                         |
 * | optional?       | boolean   | Whether the argument is optional or not (default: false)   |
 * | requirements?   | object[]  | Argument requirements (see below)                          |
 * | requiredInfo?   | object[]  | Required information ids (see below)                       |
 * | validator?¹     | string    | Qualified name of validator of argument input              |
 * | transformer?¹   | string    | Qualified name of transformer of argument input to object  |
 * | possibilities?¹ | string    | Qualified name of possibilities function of argument input |
 * | handler?¹       | string    | Qualified name of handler of argument value                |
 *  ```
 *
 *
 * ## Requirements
 *
 *  ```
 * | Key             | Data type | Description                                                |
 * | --------------- | --------- | ---------------------------------------------------------- |
 * | info            | object    | Required information id (see below)                        |
 * | tester¹         | string    | Qualified name of requirements tester                      |
 * | data            | string    | Required data                                              |
 *  ```
 *
 * ## Required information
 *
 *  ```
 * | Key             | Data type | Description                                                |
 * | --------------- | --------- | ---------------------------------------------------------- |
 * | id              | object    | Information id (see below)                                 |
 * | useProviders?   | boolean   | Whether provided information satisfies this requirement    |
 *  ```
 *
 * ## Information id
 *
 *  ```
 * | Key             | Data type | Description                                                |
 * | --------------- | --------- | ---------------------------------------------------------- |
 * | tags            | string[]  | Tags used to identify information                          |
 * | type¹           | string    | Qualified name of information id                           |
 *  ```
 *
 * ## Ref
 *
 * - `¹` - Qualified names are resolved to class, using `TypeResolver`.
 * - `²` - Text representation specified by [TextUtil.parse][com.github.jonathanxd.iutils.text.TextUtil.parse] may be used.
 * - `³` - Locale: When defined, nameComponent and description is resolved based on the locale (alias too for commands). The resolution is made through resolving localization of `PATH.name`, `PATH.description` and `PATH.alias` (if applicable), where `PATH` is the value defined for locale.
 * - `?` - Optional
 *
 * ## Example
 *
 * ```json
 * {
 *   "name": "register",
 *   "description": "Registers the user",
 *   "handler": "my.test.RegisterCommandHandler",
 *   "arguments": [
 *     {
 *       "id": "name",
 *       "type": "String"
 *     },
 *     {
 *       "id": "email",
 *       "type": "String",
 *       "validator": "my.test.EmailValidator"
 *     }
 *   ],
 *   "requiredInfo": [
 *     {
 *       "id": { "tags": ["player"], "type": "my.test.Player" }
 *     }
 *   ],
 *   "requirements": [
 *     {
 *       "id": { "tags": ["player"], "type": "my.test.Player" },
 *       "tester": "my.test.ReqTester",
 *       "data": "perm.register"
 *     }
 *   ]
 * }
 * ```
 *
 * You may also use ':'
 *
 * Obs: this annotation is converted to a [CmdJsonObj].
 *
 * @see JsonCommandParser
 * @property type Type of the json localization, may be a resource of the annotation enclosing class.
 * @property value Value of the type, if is a resource, the path to resource, if a string, the json string, if a class,
 * the full class name.
 * @property parser The parser of command json, this allows different json command stub to be implemented.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class CmdJson(
    val type: CmdJsonType = CmdJsonType.RESOURCE,
    val value: String,
    val parser: KClass<out JsonCommandParser> = DefaultJsonParser::class
)

/**
 * Specifies that the command json is supplied by [value].
 *
 * @see CmdJson
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class CmdJsonSupplied(val value: KClass<out () -> CmdJsonObj>)

/**
 * Type of the json.
 */
enum class CmdJsonType {
    /**
     * Resource of the class
     */
    RESOURCE,

    /**
     * Plain json string
     */
    STRING
}

/**
 * See [CmdJson].
 */
data class CmdJsonObj(
    val type: CmdJsonType,
    val value: String,
    val parser: Class<out JsonCommandParser>
)

/**
 * Creates a [CmdJsonObj] based on [CmdJson] annotation of [receiver][AnnotatedElement], or gets
 * it from supplier specified in [CmdJsonSupplied] annotation.
 */
fun AnnotatedElement.getCommandJsonObj(): CmdJsonObj? =
    this.getDeclaredAnnotation(CmdJson::class.java)?.toObj()
            ?: this.getDeclaredAnnotation(CmdJsonSupplied::class.java)?.value?.get()?.invoke()

/**
 * Creates [CmdJsonObj] from [receiver][CmdJson]
 */
fun CmdJson.toObj(): CmdJsonObj = CmdJsonObj(this.type, this.value, this.parser.java)

/**
 * Resolves json string from [CmdJsonObj] using resource of [localization] if needed.
 */
fun CmdJsonObj.resolveJsonString(localization: Class<*>): String =
    when (this.type) {
        CmdJsonType.RESOURCE -> localization.getResourceAsStream(this.value).readBytes().toString(
            Charsets.UTF_8
        )
        CmdJsonType.STRING -> this.value
    }