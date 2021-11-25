/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/ElapsedTimer.h>
#include <LibMain/Main.h>
#include <unistd.h>

static void usage()
{
    warnln("usage: allocate [number [unit (B/KiB/MiB)]]");
    exit(1);
}

enum class Unit {
    Bytes,
    KiB,
    MiB,
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int count = 50;
    auto unit = Unit::MiB;

    if (arguments.argc >= 2) {
        auto number = arguments.strings[1].to_uint();
        if (!number.has_value()) {
            usage();
        }
        count = number.value();
    }

    if (arguments.argc >= 3) {
        if (arguments.strings[2] == "B")
            unit = Unit::Bytes;
        else if (arguments.strings[2] == "KiB")
            unit = Unit::KiB;
        else if (arguments.strings[2] == "MiB")
            unit = Unit::MiB;
        else
            usage();
    }

    switch (unit) {
    case Unit::Bytes:
        break;
    case Unit::KiB:
        count *= KiB;
        break;
    case Unit::MiB:
        count *= MiB;
        break;
    }

    outln("allocating memory ({} bytes)...", count);
    auto timer = Core::ElapsedTimer::start_new();
    char* ptr = (char*)malloc(count);
    if (!ptr) {
        outln("failed.");
        return 1;
    }
    outln("done in {}ms", timer.elapsed());

    auto pages = count / PAGE_SIZE;
    auto step = pages / 10;

    outln("writing one byte to each page of allocated memory...");
    timer.start();
    auto timer2 = Core::ElapsedTimer::start_new();
    for (int i = 0; i < pages; ++i) {
        ptr[i * PAGE_SIZE] = 1;

        if (i != 0 && (i % step) == 0) {
            auto ms = timer2.elapsed();
            if (ms == 0)
                ms = 1;

            auto bps = double(step * PAGE_SIZE) / (double(ms) / 1000);

            outln("step took {}ms ({}MiB/s)", ms, bps / MiB);

            timer2.start();
        }
    }
    outln("done in {}ms", timer.elapsed());

    outln("sleeping for ten seconds...");
    for (int i = 0; i < 10; i++) {
        outln("{}", i);
        sleep(1);
    }
    outln("done.");

    outln("freeing memory...");
    timer.start();
    free(ptr);
    outln("done in {}ms", timer.elapsed());

    return 0;
}
