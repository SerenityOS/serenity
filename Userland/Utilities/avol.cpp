/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/Buffer.h>
#include <LibAudio/ClientConnection.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    Core::EventLoop loop;
    auto audio_client = Audio::ClientConnection::construct();

    bool mute = false;
    bool unmute = false;
    // FIXME: What is a good way to have an optional int argument?
    const char* volume = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(mute, "Mute volume", "mute", 'm');
    args_parser.add_option(unmute, "Unmute volume", "unmute", 'M');
    args_parser.add_positional_argument(volume, "Volume to set", "volume", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!mute && !unmute && !volume) {
        auto volume = audio_client->get_main_mix_volume();
        outln("Volume: {}", volume);
        return 0;
    }
    if (!(mute ^ unmute ^ (volume != nullptr))) {
        warnln("Only one of mute, unmute or volume must be used");
        return 1;
    }
    if (mute) {
        audio_client->set_muted(true);
        outln("Muted.");
    } else if (unmute) {
        audio_client->set_muted(false);
        outln("Unmuted.");
    } else {
        auto new_volume = atoi(volume);
        audio_client->set_main_mix_volume(new_volume);
    }
    return 0;
}
