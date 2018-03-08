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
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.iutils.localization.Locale
import com.github.jonathanxd.iutils.localization.LocaleManager
import com.github.jonathanxd.iutils.localization.Locales
import com.github.jonathanxd.iutils.localization.MapLocaleManager
import com.github.jonathanxd.iutils.localization.json.JsonLocaleLoader
import com.github.jonathanxd.iutils.text.localizer.FastTextLocalizer
import com.github.jonathanxd.iutils.text.localizer.TextLocalizer
import com.github.jonathanxd.kwcommands.printer.Printers
import java.nio.file.Paths

/**
 * Global localizers and locales. You don't need to use them if you don't want to.
 */
object KLocale {
    val localeManager: LocaleManager = MapLocaleManager()
    val defaultLocale: Locale = Locales.create("en_us").also {
        it.load()
        localeManager.registerLocale(it)
    }
    val ptBr: Locale = Locales.create("pt_br").also {
        it.load()
        localeManager.registerLocale(it)
    }

    private fun Locale.load() {
        val path = Paths.get("kwcommands", "lang")
        val loader = JsonLocaleLoader.JSON_LOCALE_LOADER

        loader.loadFromResource(this, path, Printers::class.java.classLoader)
    }

    var localizer: TextLocalizer = FastTextLocalizer(localeManager, defaultLocale)
}