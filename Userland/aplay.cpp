/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibAudio/Buffer.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/WavLoader.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    const char* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to WAV file", "path");
    args_parser.parse(argc, argv);

    Core::EventLoop loop;

    auto audio_client = Audio::ClientConnection::construct();
    audio_client->handshake();
    Audio::WavLoader loader(path);
    if (loader.has_error()) {
        fprintf(stderr, "Failed to load WAV file: %s\n", loader.error_string());
        return 1;
    }

    printf("\033[34;1m Playing\033[0m: %s\n", path);
    printf("\033[34;1m  Format\033[0m: %u Hz, %u-bit, %s\n",
        loader.sample_rate(),
        loader.bits_per_sample(),
        loader.num_channels() == 1 ? "Mono" : "Stereo");
    printf("\033[34;1mProgress\033[0m: \033[s");
    for (;;) {
        auto samples = loader.get_more_samples();
        if (samples) {
            printf("\033[u");
            printf("%d/%d", loader.loaded_samples(), loader.total_samples());
            fflush(stdout);
            audio_client->enqueue(*samples);
        } else if (audio_client->get_remaining_samples()) {
            sleep(1);
        } else {
            break;
        }
    }
    printf("\n");
    return 0;
}
