/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/ClientConnection.h>
#include <LibAudio/Loader.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibMain/Main.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    const char* path = nullptr;
    bool should_loop = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to audio file", "path");
    args_parser.add_option(should_loop, "Loop playback", "loop", 'l');
    args_parser.parse(arguments);

    Core::EventLoop loop;

    auto audio_client = Audio::ClientConnection::construct();
    NonnullRefPtr<Audio::Loader> loader = Audio::Loader::create(path);
    if (loader->has_error()) {
        warnln("Failed to load audio file: {}", loader->error_string());
        return 1;
    }

    outln("\033[34;1m Playing\033[0m: {}", path);
    outln("\033[34;1m  Format\033[0m: {} Hz, {}-bit, {}",
        loader->sample_rate(),
        loader->bits_per_sample(),
        loader->num_channels() == 1 ? "Mono" : "Stereo");
    out("\033[34;1mProgress\033[0m: \033[s");

    auto resampler = Audio::ResampleHelper<double>(loader->sample_rate(), audio_client->get_sample_rate());

    for (;;) {
        auto samples = loader->get_more_samples();
        if (samples) {
            out("\033[u");
            out("{}/{}", loader->loaded_samples(), loader->total_samples());
            fflush(stdout);
            resampler.reset();
            samples = Audio::resample_buffer(resampler, *samples);
            audio_client->enqueue(*samples);
        } else if (loader->has_error()) {
            outln();
            outln("Error: {}", loader->error_string());
            break;
        } else if (should_loop) {
            loader->reset();
        } else if (audio_client->get_remaining_samples()) {
            sleep(1);
        } else {
            break;
        }
    }
    outln();
    return 0;
}
