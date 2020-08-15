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

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/ElapsedTimer.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

static void usage(void)
{
    printf("usage: allocate [number [unit (B/KiB/MiB)]]\n");
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

    printf("allocating memory (%d bytes)...\n", count);
    timer.start();
    char* ptr = (char*)malloc(count);
    if (!ptr) {
        printf("failed.\n");
        return 1;
    }
    printf("done in %dms\n", timer.elapsed());

    auto pages = count / PAGE_SIZE;
    auto step = pages / 10;

    Core::ElapsedTimer timer2;

    printf("writing one byte to each page of allocated memory...\n");
    timer.start();
    timer2.start();
    for (int i = 0; i < pages; ++i) {
        ptr[i * PAGE_SIZE] = 1;

        if (i != 0 && (i % step) == 0) {
            auto ms = timer2.elapsed();
            if (ms == 0)
                ms = 1;

            auto bps = double(step * PAGE_SIZE) / (double(ms) / 1000);

            printf("step took %dms (%fMiB/s)\n", ms, bps / MiB);

            timer2.start();
        }
    }
    printf("done in %dms\n", timer.elapsed());

    printf("sleeping for ten seconds...\n");
    for (int i = 0; i < 10; i++) {
        printf("%d\n", i);
        sleep(1);
    }
    printf("done.\n");

    printf("freeing memory...\n");
    timer.start();
    free(ptr);
    printf("done in %dms\n", timer.elapsed());

    return 0;
}
