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
package com.github.jonathanxd.kwcommands

import com.github.jonathanxd.iutils.annotation.Named
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.iutils.text.dynamic.DynamicGenerator
import com.github.jonathanxd.iutils.text.dynamic.Section

interface TextsStub {
    @Section("error", "command_arguments_missing")
    fun getArgumentCommandsMissingText(@Named(COMMAND_NAME) commandName: String): TextComponent

    @Section("error", "command_provided_arguments")
    fun getProvidedArgumentsText(): TextComponent

    @Section("error", "command_missing_arguments")
    fun getMissingArgumentText(): TextComponent

    @Section("error", "command_not_found")
    fun getMissingCommandText(@Named(COMMAND_NAME) commandName: String): TextComponent

    @Section("error", "malformed_input")
    fun getMalformedInputText(@Named(INPUT_TYPE) inputType: TextComponent): TextComponent

    @Section("info", "processed_commands")
    fun getProcessedCommandsText(): TextComponent

    @Section("info", "command_specification")
    fun getCommandSpecificationText(): TextComponent

    @Section("info", "available_commands")
    fun getAvailableCommandsText(): TextComponent

    @Section("info", "parsed_arguments")
    fun getParsedArgumentsText(): TextComponent

    @Section("error", "invalid_inputs")
    fun getInvalidInputsText(): TextComponent

    @Section("info", "command")
    fun getCommandText(): TextComponent

    @Section("info", "argument")
    fun getArgumentText(): TextComponent

    @Section("info", "message")
    fun getMessageText(): TextComponent

    @Section("info", "input")
    fun getInputText(): TextComponent

    @Section("info", "input_type")
    fun getInputTypeText(): TextComponent

    @Section("info", "valid_input_types")
    fun getValidInputTypesText(): TextComponent

    @Section("info", "parser")
    fun getParserText(): TextComponent

    @Section("info", "argument_type")
    fun getArgumentTypeText(): TextComponent

    @Section("info", "true")
    fun getTrueText(): TextComponent

    @Section("info", "false")
    fun getFalseText(): TextComponent

    @Section("info", "range")
    fun getInputRangeText(@Named(RANGE_START) start: String, @Named(RANGE_END) end: String): TextComponent

    @Section("info", "argument_possibilities")
    fun getArgumentPossibilitiesText(): TextComponent

    @Section("info", "req", "continuation")
    fun getReqContinuation(@Named(CONTAINER_STRING) containerString: TextComponent): TextComponent

    @Section("error", "argument_not_found")
    fun getArgumentNotFoundText(@Named(INPUT) input: String,
                                @Named(COMMAND_NAME) commandName: String): TextComponent

    @Section("error", "no_input_for_argument")
    fun getNoInputForArgumentText(@Named(ARGUMENT_NAME) argumentName: String,
                                  @Named(COMMAND_NAME) commandName: String): TextComponent

    @Section("error", "invalid_input_value")
    fun getInvalidInputValueText(@Named(INPUT) input: String,
                                 @Named(ARGUMENT_NAME) argumentName: String,
                                 @Named(COMMAND_NAME) commandName: String): TextComponent

    @Section("error", "unsatisfied_requirement")
    fun getUnsatisfiedRequirementText(@Named(RESULT_STRING) resultString: TextComponent): TextComponent

    @Section("error", "missing_information")
    fun getMissingInformationText(@Named(RESULT_STRING) resultString: TextComponent): TextComponent

    @Section("error", "information_identification")
    fun getInformationIdentificationText(): TextComponent

    @Section("info", "type")
    fun getTypeText(): TextComponent

    @Section("info", "tags")
    fun getTagsText(): TextComponent

    @Section("info", "present")
    fun getPresentText(): TextComponent

    @Section("info", "required")
    fun getRequiredText(): TextComponent

    @Section("info", "tester")
    fun getTesterText(): TextComponent

    @Section("info", "reason")
    fun getReasonText(): TextComponent

    @Section("info", "include_provided")
    fun getIncludeProvidedText(): TextComponent

    @Section("info", "key")
    fun getKeyText(): TextComponent

    @Section("info", "value")
    fun getValueText(): TextComponent

    @Section("info", "expected_tokens")
    fun getExpectedTokensText(): TextComponent

    @Section("info", "found_token")
    fun getFoundTokenText(): TextComponent

    @Section("info", "parsed_map")
    fun getParsedMapText(): TextComponent

    @Section("info", "parsed_list")
    fun getParsedListText(): TextComponent

    @Section("help", "header", "1")
    fun header1(): TextComponent
    @Section("help", "header", "2")
    fun header2(): TextComponent
    @Section("help", "header", "3")
    fun header3(): TextComponent
    @Section("help", "header", "4")
    fun header4(): TextComponent
    @Section("help", "header", "5")
    fun header5(): TextComponent
    @Section("help", "header", "6")
    fun header6(): TextComponent
    @Section("help", "header", "7")
    fun header7(): TextComponent
    @Section("help", "header", "8")
    fun header8(): TextComponent
    @Section("help", "header", "9")
    fun header9(): TextComponent
    @Section("help", "header", "10")
    fun header10(): TextComponent
    @Section("help", "header", "11")
    fun header11(): TextComponent
    @Section("help", "header", "12")
    fun header12(): TextComponent

    @Section("help", "footer", "1")
    fun footer1(): TextComponent
    @Section("help", "footer", "2")
    fun footer2(): TextComponent

    @Section("help", "argument_description")
    fun getArgumentDescriptionText(): TextComponent

    @Section("help", "requires_value")
    fun getRequiresValueText(@Named(REQUIRED_VALUE) value: String,
                            @Named(SUBJECT_TYPE) subjectType: String,
                            @Named(SUBJECT_TAGS) subjectTags: String,
                            @Named(TESTER) tester: TextComponent): TextComponent

    @Section("help", "requires_info")
    fun getRequiresInfoText(@Named(INFORMATION_ID) id: String,
                            @Named(INFORMATION_TYPE) infoType: String): TextComponent

    @Section("help", "no_commands")
    fun getNoCommandsText(): TextComponent

    @Section("type", "single")
    fun getSingleTypeText(): TextComponent

    @Section("type", "list")
    fun getListTypeText(): TextComponent

    @Section("type", "map")
    fun getMapTypeText(): TextComponent

    @Section("type", "empty")
    fun getEmptyTypeText(): TextComponent

    @Section("info", "requirements")
    fun getRequirementsText(): TextComponent
}

val Texts = DynamicGenerator.generate(TextsStub::class.java)

private const val INPUT_TYPE = "input_type"
private const val COMMAND_NAME = "command_name"
private const val ARGUMENT_NAME = "argument_name"
private const val INPUT = "input"
private const val RANGE_START = "range_start"
private const val RANGE_END = "range_end"
private const val CONTAINER_STRING = "container_string"
private const val RESULT_STRING = "result_string"
private const val REQUIRED_VALUE = "required_value"
private const val SUBJECT_TYPE = "subject_type"
private const val SUBJECT_TAGS = "subject_tags"
private const val TESTER = "tester"
private const val INFORMATION_ID = "information_id"
private const val INFORMATION_TYPE = "information_type"