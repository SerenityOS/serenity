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

#include <AK/NumberFormat.h>
#include <AK/SharedBuffer.h>
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    const char* url_str = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(url_str, "URL to download from", "url");
    args_parser.parse(argc, argv);

    URL url(url_str);
    if (!url.is_valid()) {
        fprintf(stderr, "'%s' is not a valid URL\n", url_str);
        return 1;
    }

    Core::EventLoop loop;
    auto protocol_client = Protocol::Client::construct();

    auto download = protocol_client->start_download(url.to_string());
    if (!download) {
        fprintf(stderr, "Failed to start download for '%s'\n", url_str);
        return 1;
    }
    u32 previous_downloaded_size { 0 };
    timeval prev_time, current_time, time_diff;
    gettimeofday(&prev_time, nullptr);

    download->on_progress = [&](Optional<u32> maybe_total_size, u32 downloaded_size) {
        fprintf(stderr, "\r\033[2K");
        if (maybe_total_size.has_value()) {
            fprintf(stderr, "\033]9;%d;%d;\033\\", downloaded_size, maybe_total_size.value());
            fprintf(stderr, "Download progress: %s / %s", human_readable_size(downloaded_size).characters(), human_readable_size(maybe_total_size.value()).characters());
        } else {
            fprintf(stderr, "Download progress: %s / ???", human_readable_size(downloaded_size).characters());
        }

        gettimeofday(&current_time, nullptr);
        timersub(&current_time, &prev_time, &time_diff);

        auto time_diff_ms = time_diff.tv_sec * 1000 + time_diff.tv_usec / 1000;
        auto size_diff = downloaded_size - previous_downloaded_size;

        fprintf(stderr, " at %s/s", human_readable_size(((float)size_diff / (float)time_diff_ms) * 1000).characters());

        previous_downloaded_size = downloaded_size;
        prev_time = current_time;
    };
    download->on_finish = [&](bool success, auto& payload, auto, auto&, auto) {
        fprintf(stderr, "\033]9;-1;\033\\");
        fprintf(stderr, "\n");
        if (success)
            write(STDOUT_FILENO, payload.data(), payload.size());
        else
            fprintf(stderr, "Download failed :(\n");
        loop.quit(0);
    };
    dbgprintf("started download with id %d\n", download->id());

    return loop.exec();
}
