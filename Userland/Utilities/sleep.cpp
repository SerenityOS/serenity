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

#include <LibCore/ArgsParser.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

static volatile bool g_interrupted;
static void handle_sigint(int)
{
    g_interrupted = true;
}

int main(int argc, char** argv)
{
    double secs;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(secs, "Number of seconds to sleep for", "num-seconds");
    args_parser.parse(argc, argv);

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    if (pledge("stdio sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    double whole_seconds = static_cast<time_t>(secs);
    double fraction = secs - whole_seconds;

    timespec requested_sleep {
        .tv_sec = static_cast<time_t>(whole_seconds),
        .tv_nsec = static_cast<long>(fraction * (double)1000000000),
    };

    timespec remaining_sleep {};

sleep_again:
    if (clock_nanosleep(CLOCK_MONOTONIC, 0, &requested_sleep, &remaining_sleep) < 0) {
        if (errno != EINTR) {
            perror("clock_nanosleep");
            return 1;
        }
    }

    if (remaining_sleep.tv_sec || remaining_sleep.tv_nsec) {
        if (!g_interrupted) {
            // If not interrupted with SIGINT, just go back to sleep.
            requested_sleep = remaining_sleep;
            goto sleep_again;
        }
        outln("Sleep interrupted with {}.{} seconds remaining.", remaining_sleep.tv_sec, remaining_sleep.tv_nsec);
    }

    signal(SIGINT, SIG_DFL);
    if (g_interrupted)
        raise(SIGINT);

    return 0;
}
