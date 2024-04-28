/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

static bool volatile g_interrupted = false;
static void handle_sigint(int)
{
    g_interrupted = true;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    double secs;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(secs, "Number of seconds to sleep for", "num-seconds");
    args_parser.parse(arguments);

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    TRY(Core::System::pledge("stdio sigaction"));

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

    TRY(Core::System::signal(SIGINT, SIG_DFL));
    if (g_interrupted)
        raise(SIGINT);

    return 0;
}
