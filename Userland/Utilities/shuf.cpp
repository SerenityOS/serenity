/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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

    // Fisher-Yates shuffle
    String tmp;
    for (size_t i = lines.size() - 1; i >= 1; --i) {
        size_t j = arc4random_uniform(i + 1);
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
