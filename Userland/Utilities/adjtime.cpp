/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2022, Matthias Zimmerman <matthias291999@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <math.h>
#include <sys/time.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio settime"));

    Core::ArgsParser args_parser;
    Optional<double> delta;
    args_parser.add_option(delta, "Adjust system time by this many seconds", "set", 's', "delta_seconds");
    args_parser.parse(arguments);

    if (delta.has_value()) {
        long delta_us = lround(*delta * 1'000'000);
        timeval delta_timeval;
        delta_timeval.tv_sec = delta_us / 1'000'000;
        delta_timeval.tv_usec = delta_us % 1'000'000;
        if (delta_timeval.tv_usec < 0) {
            delta_timeval.tv_sec--;
            delta_timeval.tv_usec += 1'000'000;
        }
        TRY(Core::System::adjtime(&delta_timeval, nullptr));
    }

    TRY(Core::System::pledge("stdio"));

    timeval remaining_delta_timeval;
    TRY(Core::System::adjtime(nullptr, &remaining_delta_timeval));
    double remaining_delta = remaining_delta_timeval.tv_sec + remaining_delta_timeval.tv_usec / 1'000'000.0;
    outln("{}", remaining_delta);

    return 0;
}
