/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio", nullptr) > 0) {
        perror("pledge");
        return 1;
    }

    Vector<String> lines;

    char* buffer = nullptr;
    for (;;) {
        size_t n = 0;
        errno = 0;
        ssize_t buflen = getline(&buffer, &n, stdin);
        if (buflen == -1 && errno != 0) {
            perror("getline");
            exit(1);
        }
        if (buflen == -1)
            break;
        lines.append({ buffer, AK::ShouldChomp::Chomp });
    }
    free(buffer);

    if (lines.is_empty())
        return 0;

    // Fisher-Yates shuffle
    String tmp;
    for (size_t i = lines.size() - 1; i >= 1; --i) {
        size_t j = get_random_uniform(i + 1);
        // Swap i and j
        if (i == j)
            continue;
        tmp = move(lines[j]);
        lines[j] = move(lines[i]);
        lines[i] = move(tmp);
    }

    for (auto& line : lines) {
        fputs(line.characters(), stdout);
        fputc('\n', stdout);
    }

    return 0;
}
