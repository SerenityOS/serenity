/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio", nullptr) > 0) {
        perror("pledge");
        return 1;
    }

    Vector<String> lines;

    for (;;) {
        char* buffer = nullptr;
        ssize_t buflen = 0;
        size_t n;
        errno = 0;
        buflen = getline(&buffer, &n, stdin);
        if (buflen == -1 && errno != 0) {
            perror("getline");
            exit(1);
        }
        if (buflen == -1)
            break;
        lines.append({ buffer, AK::ShouldChomp::Chomp });
    }

    quick_sort(lines, [](auto& a, auto& b) {
        return strcmp(a.characters(), b.characters()) < 0;
    });

    for (auto& line : lines) {
        fputs(line.characters(), stdout);
        fputc('\n', stdout);
    }

    return 0;
}
