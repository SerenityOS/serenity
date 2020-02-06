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

#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <LibAudio/AWavLoader.h>
#include <LibCore/CEventLoop.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    Core::EventLoop loop;
    if (argc < 2) {
        fprintf(stderr, "Need a WAV to play\n");
        return 1;
    }

    auto audio_client = Audio::ClientConnection::construct();
    audio_client->handshake();
    Audio::WavLoader loader(argv[1]);

    printf("\033[34;1m Playing\033[0m: %s\n", argv[1]);
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
