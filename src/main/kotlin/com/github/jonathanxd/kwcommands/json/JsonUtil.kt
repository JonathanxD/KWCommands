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

import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.Arguments
import com.github.jonathanxd.kwcommands.argument.StaticListArguments
import com.github.jonathanxd.kwcommands.command.Handler
import org.json.simple.JSONArray
import org.json.simple.JSONObject

inline fun <reified T> Any?.asJSON(keyRef: String): T =
    when (this) {
        !is T -> throw IllegalArgumentException(
            "Input value for $keyRef must be of type ${T::class.java.canonicalName}, " +
                    "but ${if (this == null) "null" else this::class.java.canonicalName} was found."
        )
        else -> this
    }


@Suppress("NOTHING_TO_INLINE")
inline fun resolveCommandHandler(input: String, typeResolver: TypeResolver): Handler? =
    typeResolver.resolveCommandHandler(input)
            ?: typeResolver.resolve(input)?.typeClass?.let { typeResolver.getSingletonInstance(it) as Handler }

@Suppress("NOTHING_TO_INLINE")
inline fun resolveArguments(input: String, typeResolver: TypeResolver): Arguments? =
    typeResolver.resolveArguments(input)
            ?: typeResolver.resolve(input)?.typeClass?.let { typeResolver.getSingletonInstance(it) as Arguments }

@Suppress("NOTHING_TO_INLINE")
inline fun resolveArgumentHandler(input: String, typeResolver: TypeResolver): ArgumentHandler<*>? =
    typeResolver.resolveArgumentHandler(input)
            ?: typeResolver.resolve(input)?.typeClass?.let { typeResolver.getSingletonInstance(it) as ArgumentHandler<*> }

@Suppress("NOTHING_TO_INLINE")
inline fun JSONObject.getCommandHandler(
    key: String,
    jsonCommandParser: JsonCommandParser
): Handler? =
    this.getAs<String>(key)?.let { jsonCommandParser.parseCommandHandler(it) }

@Suppress("NOTHING_TO_INLINE")
inline fun JSONObject.getArguments(
    key: String,
    jsonCommandParser: JsonCommandParser
): Arguments? =
    this[key].let {
        when (it) {
            is String -> jsonCommandParser.parseArguments(it)
            is JSONArray -> StaticListArguments(this.getAsArrayOfObj(key) {
                jsonCommandParser.parseArgument(
                    it
                )
            })
            else -> null
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun JSONObject.getArgumentHandler(
    key: String,
    jsonCommandParser: JsonCommandParser
): ArgumentHandler<*>? =
    this.getAs<String>(key)?.let { jsonCommandParser.parseArgumentHandler(it) }

inline fun <reified T> JSONObject.getAsSingleton(key: String, typeResolver: TypeResolver): T? =
    this.getAs<String>(key)?.let {
        typeResolver.resolve(it)?.typeClass?.let { typeResolver.getSingletonInstance(it) as T }
    }

inline fun <reified T> JSONObject.getAsSingletonReq(key: String, typeResolver: TypeResolver): T =
    this.getRequired<String>(key).let {
        (typeResolver.resolve(it)
                ?: throw IllegalArgumentException("Cannot resolve type $it for $key in json."))
            .typeClass.let { typeResolver.getSingletonInstance(it) as T }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun JSONObject.getAsArrayOfStr(key: String): List<String> =
    this.getAs<JSONArray>(key)?.map {
        it.asJSON<String>(key)
    }.orEmpty()

inline fun <reified T> JSONObject.getAsArrayOfObj(
    key: String,
    mapper: (obj: JSONObject) -> T
): List<T> =
    this.getAs<JSONArray>(key)?.map {
        mapper(it.asJSON(key))
    }.orEmpty()

inline fun <reified T> JSONObject.getAs(key: String): T? =
    this[key].let {
        if (it != null && it !is T)
            throw IllegalArgumentException(
                "Input for $key is invalid, expected ${T::class.java.canonicalName} " +
                        "but ${it::class.java.canonicalName} was found."
            )
        it as T?
    }

inline fun <reified T> JSONObject.getRequired(key: String): T =
    this.getRequiredValue(key).let {
        if (it !is T)
            throw IllegalArgumentException(
                "Input for $key is invalid, expected ${T::class.java.canonicalName} " +
                        "but found ${it::class.java.canonicalName} was found."
            )
        it
    }


fun JSONObject.getRequiredValue(key: String): Any =
    this[key] ?: throw IllegalArgumentException("Key $key is required in command json.")

