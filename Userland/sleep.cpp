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

#include <LibCore/ArgsParser.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

static void handle_sigint(int)
{
}

int main(int argc, char** argv)
{
    double fractional_seconds;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(fractional_seconds, "Number of seconds to sleep for (accepts fractions)", "num-seconds");
    args_parser.parse(argc, argv);

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    useconds_t usecs = 0;

    // We overflow microseconds... lets split the wait.
    static const auto OVERFLOW_VALUE = UINTMAX_MAX / (1000 * 1000);
    if (fractional_seconds > OVERFLOW_VALUE) {
        perror("Number of seconds too large for sleep");
        usecs = 0;
    }

    usecs = fractional_seconds * (1000 * 1000);

    int remaining = usleep(usecs);
    if (remaining) {
        printf("Sleep interrupted with %d seconds remaining.\n", remaining / (1000 * 1000));
        return EXIT_FAILURE;
    }
    return EXIT_SUCCESS;
}
