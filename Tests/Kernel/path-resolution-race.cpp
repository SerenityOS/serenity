/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <sys/stat.h>
#include <unistd.h>

int main()
{
    if (!fork()) {
        for (;;) {
            mkdir("/tmp/x", 0666);
            rmdir("/tmp/x");
        }
    }
    for (;;) {
        chdir("/tmp/x");
    }
}
