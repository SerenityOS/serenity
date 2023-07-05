/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/Loader.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <math.h>
#include <stdio.h>

constexpr size_t LOAD_CHUNK_SIZE = 128 * KiB;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath sendfd unix thread proc"));

    StringView path {};
    bool should_loop = false;
    bool show_sample_progress = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to audio file", "path");
    args_parser.add_option(should_loop, "Loop playback", "loop", 'l');
    args_parser.add_option(show_sample_progress, "Show playback progress in samples", "sample-progress", 's');
    args_parser.parse(arguments);

    // Note: We must determine the absolute path *before* beginning to raise the veil.
    auto absolute_path = TRY(FileSystem::absolute_path(path));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/audio", "rw"));
    TRY(Core::System::unveil(absolute_path, "r"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop loop;

    auto audio_client = TRY(Audio::ConnectionToServer::try_create());
    auto maybe_loader = Audio::Loader::create(path);
    if (maybe_loader.is_error()) {
        warnln("Failed to load audio file: {}", maybe_loader.error().description);
        return 1;
    }
    auto loader = maybe_loader.release_value();

    TRY(Core::System::pledge("stdio sendfd thread proc"));

    outln("\033[34;1m Playing\033[0m: {}", path);
    outln("\033[34;1m  Format\033[0m: {} {} Hz, {}-bit, {}",
        loader->format_name(),
        loader->sample_rate(),
        loader->bits_per_sample(),
        loader->num_channels() == 1 ? "Mono" : "Stereo");
    out("\033[34;1mProgress\033[0m: \033[s");

    audio_client->set_self_sample_rate(loader->sample_rate());

    auto print_playback_update = [&]() {
        out("\033[u");
        if (show_sample_progress) {
            out("{}/{}", audio_client->total_played_samples(), loader->total_samples());
        } else {
            auto playing_seconds = static_cast<int>(floor(static_cast<double>(audio_client->total_played_samples()) / static_cast<double>(loader->sample_rate())));
            auto playing_minutes = playing_seconds / 60;
            auto playing_seconds_of_minute = playing_seconds % 60;

            auto total_seconds = static_cast<int>(floor(static_cast<double>(loader->total_samples()) / static_cast<double>(loader->sample_rate())));
            auto total_minutes = total_seconds / 60;
            auto total_seconds_of_minute = total_seconds % 60;

            auto remaining_seconds = total_seconds - playing_seconds;
            auto remaining_minutes = remaining_seconds / 60;
            auto remaining_seconds_of_minute = remaining_seconds % 60;

            out("\033[1m{:02d}:{:02d}\033[0m [{}{:02d}:{:02d}] -- {:02d}:{:02d}",
                playing_minutes, playing_seconds_of_minute,
                remaining_seconds == 0 ? " " : "-",
                remaining_minutes, remaining_seconds_of_minute,
                total_minutes, total_seconds_of_minute);
        }
        fflush(stdout);
    };

    for (;;) {
        auto samples = loader->get_more_samples(LOAD_CHUNK_SIZE);
        if (!samples.is_error()) {
            if (samples.value().size() > 0) {
                print_playback_update();
                // We can read and enqueue more samples
                TRY(audio_client->async_enqueue(samples.release_value()));
            } else if (should_loop) {
                // We're done: now loop
                auto result = loader->reset();
                if (result.is_error()) {
                    outln();
                    outln("Error while resetting: {} (at {:x})", result.error().description, result.error().index);
                }
            } else if (samples.value().size() == 0 && audio_client->remaining_samples() == 0) {
                // We're done and the server is done
                break;
            }
            while (audio_client->remaining_samples() > LOAD_CHUNK_SIZE) {
                // The server has enough data for now
                print_playback_update();
                usleep(1'000'000 / 10);
            }
        } else {
            outln();
            outln("Error: {} (at {:x})", samples.error().description, samples.error().index);
            return 1;
        }
    }
    outln();
    return 0;
}
