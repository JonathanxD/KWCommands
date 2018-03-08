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
package com.github.jonathanxd.kwcommands.test;

import com.github.jonathanxd.iutils.reflection.Invokables;
import com.github.jonathanxd.iutils.reflection.Links;
import com.github.jonathanxd.iutils.text.Text;
import com.github.jonathanxd.iutils.type.TypeInfo;
import com.github.jonathanxd.kwcommands.argument.Argument;
import com.github.jonathanxd.kwcommands.argument.StaticListArguments;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.information.Information;
import com.github.jonathanxd.kwcommands.information.InformationProvider;
import com.github.jonathanxd.kwcommands.information.RequiredInformation;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.information.InformationProviders;
import com.github.jonathanxd.kwcommands.information.InformationProvidersImpl;
import com.github.jonathanxd.kwcommands.manager.InstanceProvider;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.processor.Processors;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.annotation.CmdHandler;
import com.github.jonathanxd.kwcommands.reflect.annotation.Info;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;
import com.github.jonathanxd.kwcommands.util.CommonArgTypesKt;

import org.junit.Test;

import kotlin.Unit;

public class Basic {

    private static final Information.Id<Speaker> SPEAKER_INFO_ID =
            new Information.Id<Speaker>(TypeInfo.of(Speaker.class), new String[]{"speaker"});

    @Test
    public void command() {
        CommandProcessor processor = Processors.createCommonProcessor();
        Command command = Command.builder()
                .name("play")
                .description(Text.of("Play a music on instrument"))
                .handler((commandContainer, informationProviders, resultHandler) -> {
                    Information<Speaker> speakerInfo = informationProviders.findOrEmpty(SPEAKER_INFO_ID);
                    if (!speakerInfo.isEmpty()) {
                        speakerInfo.getValue().speak("...");
                    }
                    return Unit.INSTANCE;
                })
                .addRequiredInfo(new RequiredInformation(SPEAKER_INFO_ID))
                .arguments(new StaticListArguments(Argument.<Music>builder()
                        .name("music")
                        .argumentType(CommonArgTypesKt.enumArgumentType(Music.class))
                        .handler((argumentContainer, commandContainer, informationProviders, resultHandler) -> {
                            Information<Speaker> speakerInfo = informationProviders.findOrEmpty(SPEAKER_INFO_ID);

                            if (!speakerInfo.isEmpty()) {
                                speakerInfo.getValue().speak(String.format("Playing %s", argumentContainer.getValue().name()));
                            }

                            return Unit.INSTANCE;
                        })
                        .build()))
                .build();

        processor.getParser().getCommandManager().registerCommand(command, this);

        InformationProviders manager = new InformationProvidersImpl();

        manager.registerInformationProvider(InformationProvider.Companion.safeFor(TypeInfo.of(Speaker.class),
                (id, imanager) -> {
                    if (id.getType().equals(TypeInfo.of(Speaker.class)))
                        return new Information<>(id, (Speaker) System.out::println, null);
                    return null;
                }));

        processor.parseAndDispatch("play A", null, manager);

    }

    @Test
    public void reflect() {
        CommandManager commandManager = new CommandManagerImpl();
        CommandProcessor processor = Processors.createCommonProcessor(commandManager);
        ReflectionEnvironment reflection = new ReflectionEnvironment(commandManager);

        // Using Link and Invokable to avoid try-catch in lambda
        InstanceProvider f = aClass ->
                Links.ofInvokable(Invokables.fromConstructor(aClass.getConstructors()[0])).invoke();

        reflection.registerCommands(reflection.fromClass(Play.class, f, this), this);

        InformationProviders informationProviders = new InformationProvidersImpl();

        informationProviders.registerInformation(SPEAKER_INFO_ID, (Speaker) System.out::println, null);

        processor.parseAndDispatch("play a c", this, informationProviders);
    }


    public enum Instrument {
        A, B, C
    }


    public enum Music {
        A,
        B,
        C
    }

    public interface Speaker {
        void speak(String s);
    }

    @Cmd(name = "play", description = "Play a music on a instrument")
    public static class Play {

        @CmdHandler
        public void play(@Arg("music") Music music,
                         @Arg("instrument") Instrument instrument,
                         @Info Speaker speaker) {
            speaker.speak(String.format("Playing %s on %s", music.name().toLowerCase(), instrument.name().toLowerCase()));
        }

    }
}
