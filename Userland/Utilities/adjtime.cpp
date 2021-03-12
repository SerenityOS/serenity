/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
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
#include <math.h>
#include <sys/time.h>
#include <unistd.h>

int main(int argc, char** argv)
{
#ifdef __serenity__
    if (pledge("stdio settime", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    Core::ArgsParser args_parser;
    double delta = __builtin_nan("");
    args_parser.add_option(delta, "Adjust system time by this many seconds", "set", 's', "delta_seconds");
    args_parser.parse(argc, argv);

    if (__builtin_isnan(delta)) {
#ifdef __serenity__
        if (pledge("stdio", nullptr) < 0) {
            perror("pledge");
            return 1;
        }
#endif
    } else {
        long delta_us = static_cast<long>(round(delta * 1'000'000));
        timeval delta_timeval;
        delta_timeval.tv_sec = delta_us / 1'000'000;
        delta_timeval.tv_usec = delta_us % 1'000'000;
        if (delta_timeval.tv_usec < 0) {
            delta_timeval.tv_sec--;
            delta_timeval.tv_usec += 1'000'000;
        }
        if (adjtime(&delta_timeval, nullptr) < 0) {
            perror("adjtime set");
            return 1;
        }
    }

    timeval remaining_delta_timeval;
    if (adjtime(nullptr, &remaining_delta_timeval) < 0) {
        perror("adjtime get");
        return 1;
    }
    double remaining_delta = remaining_delta_timeval.tv_sec + remaining_delta_timeval.tv_usec / 1'000'000.0;
    printf("%f\n", remaining_delta);

    return 0;
}
