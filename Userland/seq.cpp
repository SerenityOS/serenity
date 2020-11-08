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

#include <AK/StdLibExtras.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

const char* g_usage = R"(Usage:
    seq [-h|--help]
    seq LAST
    seq FIRST LAST
    seq FIRST INCREMENT LAST
)";

static void print_usage(FILE* stream)
{
    fprintf(stream, g_usage);
    return;
}

static double get_double(const char* name, const char* d_string, int* number_of_decimals)
{
    char* end;
    double d = strtod(d_string, &end);
    if (d == 0 && end == d_string) {
        fprintf(stderr, "%s: invalid argument \"%s\"\n", name, d_string);
        print_usage(stderr);
        exit(1);
    }
    if (char* dot = strchr(d_string, '.'))
        *number_of_decimals = strlen(dot + 1);
    else
        *number_of_decimals = 0;
    return d;
}

int main(int argc, const char* argv[])
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            print_usage(stdout);
            exit(0);
        }
    }

    double start = 1, step = 1, end = 1;
    int number_of_start_decimals = 0, number_of_step_decimals = 0, number_of_end_decimals = 0;
    switch (argc) {
    case 2:
        end = get_double(argv[0], argv[1], &number_of_end_decimals);
        break;
    case 3:
        start = get_double(argv[0], argv[1], &number_of_start_decimals);
        end = get_double(argv[0], argv[2], &number_of_end_decimals);
        break;
    case 4:
        start = get_double(argv[0], argv[1], &number_of_start_decimals);
        step = get_double(argv[0], argv[2], &number_of_step_decimals);
        end = get_double(argv[0], argv[3], &number_of_end_decimals);
        break;
    default:
        fprintf(stderr, "%s: unexpected number of arguments\n", argv[0]);
        print_usage(stderr);
        return 1;
    }

    if (step == 0) {
        fprintf(stderr, "%s: increment must not be 0\n", argv[0]);
        return 1;
    }

    if (__builtin_isnan(start) || __builtin_isnan(step) || __builtin_isnan(end)) {
        fprintf(stderr, "%s: start, step, and end must not be NaN\n", argv[0]);
        return 1;
    }

    int number_of_decimals = max(number_of_start_decimals, max(number_of_step_decimals, number_of_end_decimals));

    int n = (end - start) / step;
    double d = start;
    for (int i = 0; i <= n; ++i) {
        char buf[40];
        snprintf(buf, sizeof(buf), "%f", d);
        if (char* dot = strchr(buf, '.')) {
            if (number_of_decimals == 0)
                *dot = '\0';
            else if ((dot - buf) + 1 + number_of_decimals < (int)sizeof(buf))
                dot[1 + number_of_decimals] = '\0';
        }
        printf("%s\n", buf);
        d += step;
    }

    return 0;
}
