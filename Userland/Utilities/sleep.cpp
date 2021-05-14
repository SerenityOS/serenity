/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <errno.h>
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
