/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/ElapsedTimer.h>
#include <string.h>
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

int main(int argc, char** argv)
{
    int count = 50;
    auto unit = Unit::MiB;

    if (argc >= 2) {
        auto number = String(argv[1]).to_uint();
        if (!number.has_value()) {
            usage();
        }
        count = number.value();
    }

    if (argc >= 3) {
        if (strcmp(argv[2], "B") == 0)
            unit = Unit::Bytes;
        else if (strcmp(argv[2], "KiB") == 0)
            unit = Unit::KiB;
        else if (strcmp(argv[2], "MiB") == 0)
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

    Core::ElapsedTimer timer;

    outln("allocating memory ({} bytes)...", count);
    timer.start();
    char* ptr = (char*)malloc(count);
    if (!ptr) {
        outln("failed.");
        return 1;
    }
    outln("done in {}ms", timer.elapsed());

    auto pages = count / PAGE_SIZE;
    auto step = pages / 10;

    Core::ElapsedTimer timer2;

    outln("writing one byte to each page of allocated memory...");
    timer.start();
    timer2.start();
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
