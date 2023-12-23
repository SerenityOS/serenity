/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2021, David Isaksson <davidisaksson93@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibAudio/ConnectionToManagerServer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <math.h>
#include <stdio.h>
#include <sys/ioctl.h>

enum AudioVariable : u32 {
    Volume,
    Mute,
    SampleRate
};

// asctl: audio server control utility
ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop loop;
    auto audio_client = TRY(Audio::ConnectionToManagerServer::try_create());

    StringView command;
    Vector<StringView> command_arguments;
    bool human_mode = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Send control signals to the audio server and hardware.");
    args_parser.add_option(human_mode, "Print human-readable output", "human-readable", 'h');
    args_parser.add_positional_argument(command, "Command, either (g)et or (s)et\n\n\tThe get command accepts a list of variables to print.\n\tThey are printed in the given order.\n\tIf no value is specified, all are printed.\n\n\tThe set command accepts a any number of variables\n\tfollowed by the value they should be set to.\n\n\tPossible variables are (v)olume, (m)ute, sample(r)ate.\n", "command");
    args_parser.add_positional_argument(command_arguments, "Arguments for the command", "args", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio rpath wpath recvfd thread"));

    if (command.equals_ignoring_ascii_case("get"sv) || command == "g") {
        // Get variables
        Vector<AudioVariable> values_to_print;
        if (command_arguments.is_empty()) {
            values_to_print.append(AudioVariable::Volume);
            values_to_print.append(AudioVariable::Mute);
            values_to_print.append(AudioVariable::SampleRate);
        } else {
            for (auto& variable : command_arguments) {
                if (variable.is_one_of("v"sv, "volume"sv))
                    values_to_print.append(AudioVariable::Volume);
                else if (variable.is_one_of("m"sv, "mute"sv))
                    values_to_print.append(AudioVariable::Mute);
                else if (variable.is_one_of("r"sv, "samplerate"sv))
                    values_to_print.append(AudioVariable::SampleRate);
                else {
                    warnln("Error: Unrecognized variable {}", variable);
                    return 1;
                }
            }
        }

        for (auto to_print : values_to_print) {
            switch (to_print) {
            case AudioVariable::Volume: {
                auto volume = lround(audio_client->get_main_mix_volume() * 100);
                if (human_mode)
                    outln("Volume: {}%", volume);
                else
                    out("{} ", volume);
                break;
            }
            case AudioVariable::Mute: {
                bool muted = audio_client->is_main_mix_muted();
                if (human_mode)
                    outln("Muted: {}", muted ? "Yes" : "No");
                else
                    out("{} ", muted ? 1 : 0);
                break;
            }
            case AudioVariable::SampleRate: {
                u32 sample_rate = audio_client->get_device_sample_rate();
                if (human_mode)
                    outln("Sample rate: {:5d} Hz", sample_rate);
                else
                    out("{} ", sample_rate);
                break;
            }
            }
        }
        if (!human_mode)
            outln();
    } else if (command.equals_ignoring_ascii_case("set"sv) || command == "s") {
        // Set variables
        HashMap<AudioVariable, Variant<int, bool>> values_to_set;
        for (size_t i = 0; i < command_arguments.size(); ++i) {
            if (i == command_arguments.size() - 1) {
                warnln("Error: value missing for last variable");
                return 1;
            }
            auto& variable = command_arguments[i];
            if (variable.is_one_of("v"sv, "volume"sv)) {
                auto volume = command_arguments[++i].to_number<int>();
                if (!volume.has_value()) {
                    warnln("Error: {} is not an integer volume", command_arguments[i - 1]);
                    return 1;
                }
                if (volume.value() < 0 || volume.value() > 100) {
                    warnln("Error: {} is not between 0 and 100", command_arguments[i - 1]);
                    return 1;
                }
                values_to_set.set(AudioVariable::Volume, volume.value());
            } else if (variable.is_one_of("m"sv, "mute"sv)) {
                auto& mute_text = command_arguments[++i];
                bool mute;
                if (mute_text.equals_ignoring_ascii_case("true"sv) || mute_text == "1") {
                    mute = true;
                } else if (mute_text.equals_ignoring_ascii_case("false"sv) || mute_text == "0") {
                    mute = false;
                } else {
                    warnln("Error: {} is not one of {{0, 1, true, false}}", mute_text);
                    return 1;
                }
                values_to_set.set(AudioVariable::Mute, mute);
            } else if (variable.is_one_of("r"sv, "samplerate"sv)) {
                auto sample_rate = command_arguments[++i].to_number<int>();
                if (!sample_rate.has_value()) {
                    warnln("Error: {} is not an integer sample rate", command_arguments[i - 1]);
                    return 1;
                }
                values_to_set.set(AudioVariable::SampleRate, sample_rate.value());
            } else {
                warnln("Error: Unrecognized variable {}", command_arguments[i]);
                return 1;
            }
        }

        for (auto to_set : values_to_set) {
            switch (to_set.key) {
            case AudioVariable::Volume: {
                int& volume = to_set.value.get<int>();
                audio_client->set_main_mix_volume(static_cast<double>(volume) / 100);
                break;
            }
            case AudioVariable::Mute: {
                bool& mute = to_set.value.get<bool>();
                audio_client->set_main_mix_muted(mute);
                break;
            }
            case AudioVariable::SampleRate: {
                int& sample_rate = to_set.value.get<int>();
                audio_client->set_device_sample_rate(sample_rate);
                break;
            }
            }
        }
    }

    return 0;
}
