/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Optional.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibMain/Main.h>
#include <unistd.h>

enum class Unit {
    Bytes,
    KiB,
    MiB,
    GiB,
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    size_t iterations_count = 10;
    size_t allocation_size = 100;
    auto unit = Unit::Bytes;
    StringView chosen_unit {};

    Core::ArgsParser args_parser;
    args_parser.add_option(chosen_unit, "Allocation's Size Unit in base 2 (B, KiB, MiB, GiB)", "unit", 'u', "unit");
    args_parser.add_option(iterations_count, "Number of seconds to sleep before freeing memory", "sleep-time", 'n', "seconds");
    args_parser.add_positional_argument(allocation_size, "Allocation Size", "size", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!chosen_unit.is_null()) {
        if (chosen_unit == "B"sv) {
            unit = Unit::Bytes;
        } else if (chosen_unit == "KiB"sv) {
            unit = Unit::KiB;
        } else if (chosen_unit == "MiB") {
            unit = Unit::MiB;
        } else if (chosen_unit == "GiB") {
            unit = Unit::GiB;
        } else {
            args_parser.print_usage(stderr, arguments.strings[0]);
            return 1;
        }
    }

    switch (unit) {
    case Unit::Bytes:
        break;
    case Unit::KiB:
        allocation_size *= KiB;
        break;
    case Unit::MiB:
        allocation_size *= MiB;
        break;
    case Unit::GiB:
        allocation_size *= GiB;
        break;
    }

    outln("allocating memory ({} bytes)...", allocation_size);
    auto timer = Core::ElapsedTimer::start_new();
    auto* ptr = reinterpret_cast<char*>(malloc(allocation_size));
    if (!ptr) {
        outln("failed.");
        return 1;
    }
    outln("done in {}ms", timer.elapsed_milliseconds());

    size_t pages_count = allocation_size / PAGE_SIZE;
    auto step = pages_count / 10;

    outln("writing one byte to each page of allocated memory...");
    timer.start();
    auto timer2 = Core::ElapsedTimer::start_new();
    for (size_t page_index = 0; page_index < pages_count; ++page_index) {
        ptr[page_index * PAGE_SIZE] = 1;

        if (page_index != 0 && (page_index % step) == 0) {
            auto ms = timer2.elapsed_milliseconds();
            if (ms == 0)
                ms = 1;

            auto bps = double(step * PAGE_SIZE) / (double(ms) / 1000);

            outln("step took {}ms ({}MiB/s)", ms, bps / MiB);

            timer2.start();
        }
    }
    outln("done in {}ms", timer.elapsed_milliseconds());

    outln("sleeping for {} seconds...", iterations_count);
    for (unsigned iteration_index = 0; iteration_index < iterations_count; iteration_index++) {
        outln("{}", iteration_index);
        sleep(1);
    }
    outln("done.");

    outln("freeing memory...");
    timer.start();
    free(ptr);
    outln("done in {}ms", timer.elapsed_milliseconds());

    return 0;
}
