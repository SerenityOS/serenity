/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/Loader.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <math.h>
#include <stdio.h>

// The Kernel has issues with very large anonymous buffers.
// FIXME: This appears to be fine for now, but it's really a hack.
constexpr size_t LOAD_CHUNK_SIZE = 128 * KiB;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath sendfd unix"));

    const char* path = nullptr;
    bool should_loop = false;
    bool show_sample_progress = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to audio file", "path");
    args_parser.add_option(should_loop, "Loop playback", "loop", 'l');
    args_parser.add_option(show_sample_progress, "Show playback progress in samples", "sample-progress", 's');
    args_parser.parse(arguments);

    TRY(Core::System::unveil(Core::File::absolute_path(path), "r"));
    TRY(Core::System::unveil("/tmp/portal/audio", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop loop;

    auto audio_client = TRY(Audio::ClientConnection::try_create());
    auto maybe_loader = Audio::Loader::create(path);
    if (maybe_loader.is_error()) {
        warnln("Failed to load audio file: {}", maybe_loader.error().description);
        return 1;
    }
    auto loader = maybe_loader.release_value();

    TRY(Core::System::pledge("stdio sendfd"));

    outln("\033[34;1m Playing\033[0m: {}", path);
    outln("\033[34;1m  Format\033[0m: {} {} Hz, {}-bit, {}",
        loader->format_name(),
        loader->sample_rate(),
        loader->bits_per_sample(),
        loader->num_channels() == 1 ? "Mono" : "Stereo");
    out("\033[34;1mProgress\033[0m: \033[s");

    auto resampler = Audio::ResampleHelper<double>(loader->sample_rate(), audio_client->get_sample_rate());

    // If we're downsampling, we need to appropriately load more samples at once.
    size_t const load_size = static_cast<size_t>(LOAD_CHUNK_SIZE * static_cast<double>(loader->sample_rate()) / static_cast<double>(audio_client->get_sample_rate()));
    // We assume that the loader can load samples at at least 2x speed (testing confirms 9x-12x for FLAC, 14x for WAV).
    // Therefore, when the server-side buffer can only play as long as the time it takes us to load a chunk,
    // we give it new data.
    int const min_buffer_size = load_size / 2;

    for (;;) {
        auto samples = loader->get_more_samples(load_size);
        if (!samples.is_error()) {
            if (samples.value()->sample_count() > 0) {
                // We can read and enqueue more samples
                out("\033[u");
                if (show_sample_progress) {
                    out("{}/{}", loader->loaded_samples(), loader->total_samples());
                } else {
                    auto playing_seconds = static_cast<int>(floor(static_cast<double>(loader->loaded_samples()) / static_cast<double>(loader->sample_rate())));
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
                resampler.reset();
                auto resampled_samples = TRY(Audio::resample_buffer(resampler, *samples.value()));
                audio_client->async_enqueue(*resampled_samples);
            } else if (should_loop) {
                // We're done: now loop
                auto result = loader->reset();
                if (result.is_error()) {
                    outln();
                    outln("Error while resetting: {} (at {:x})", result.error().description, result.error().index);
                }
            } else if (samples.value()->sample_count() == 0 && audio_client->get_remaining_samples() == 0) {
                // We're done and the server is done
                break;
            }
            while (audio_client->get_remaining_samples() > min_buffer_size) {
                // The server has enough data for now
                sleep(1);
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
