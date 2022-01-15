/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static Optional<double> get_double(const char* d_string, int* number_of_decimals)
{
    char* end;
    double d = strtod(d_string, &end);
    if (isnan(d) || d_string == end)
        return {};

    if (const char* dot = strchr(d_string, '.'))
        *number_of_decimals = strlen(dot + 1);
    else
        *number_of_decimals = 0;
    return d;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio", nullptr));
    TRY(Core::System::unveil(nullptr, nullptr));

    int number_of_decimals_arg_one = 0;
    int number_of_decimals_arg_two = 0;
    int number_of_decimals_arg_three = 0;
    double start = 1;
    double step = 1;
    double end = 1;

    auto args_parser = Core::ArgsParser();
    args_parser.set_general_help("Print a sequence of numbers.");
    args_parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "When one argument",
        .name = "LAST",
        .accept_value = [&](char const* input) {
            auto opt = get_double(input, &number_of_decimals_arg_one);
            if (opt.has_value()) {
                end = opt.value_or(0.0);
                return true;
            }
            return false;
        },
    });
    args_parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "When two arguments",
        .name = "FIRST LAST",
        .accept_value = [&](char const* input) {
            auto opt = get_double(input, &number_of_decimals_arg_two);
            if (opt.has_value()) {
                auto value = opt.value_or(0.0);
                start = end;
                end = value;
                return true;
            }
            return false;
        },
    });
    args_parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "When three arguments",
        .name = "FIRST INCREMENT LAST",
        .accept_value = [&](char const* input) {
            auto opt = get_double(input, &number_of_decimals_arg_three);
            if (opt.has_value()) {
                auto value = opt.value_or(0.0);
                step = end;
                end = value;
                return true;
            }
            return false;
        },
    });
    args_parser.parse(arguments);

    if (step == 0) {
        warnln("{}: INCREMENT must not be 0", arguments.argv[0]);
        args_parser.print_usage(stderr, arguments.argv[0]);
        return 1;
    }

    int number_of_decimals = max(number_of_decimals_arg_one, max(number_of_decimals_arg_two, number_of_decimals_arg_three));
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
