/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

const char* const g_usage = R"(Usage:
    seq [-h|--help]
    seq LAST
    seq FIRST LAST
    seq FIRST INCREMENT LAST
)";

static void print_usage(FILE* stream)
{
    fputs(g_usage, stream);
    return;
}

static double get_double(const char* name, const char* d_string, int* number_of_decimals)
{
    char* end;
    double d = strtod(d_string, &end);
    if (d == 0 && end == d_string) {
        warnln("{}: invalid argument \"{}\"", name, d_string);
        print_usage(stderr);
        exit(1);
    }
    if (const char* dot = strchr(d_string, '.'))
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
        warnln("{}: unexpected number of arguments", argv[0]);
        print_usage(stderr);
        return 1;
    }

    if (step == 0) {
        warnln("{}: increment must not be 0", argv[0]);
        return 1;
    }

    if (__builtin_isnan(start) || __builtin_isnan(step) || __builtin_isnan(end)) {
        warnln("{}: start, step, and end must not be NaN", argv[0]);
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
        outln("{}", buf);
        d += step;
    }

    return 0;
}
