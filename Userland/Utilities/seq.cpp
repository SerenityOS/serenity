/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

char const* const g_usage = R"(Usage:
    seq [-h|--help]
    seq LAST
    seq FIRST LAST
    seq FIRST INCREMENT LAST
)";

static void print_usage(FILE* stream)
{
    fputs(g_usage, stream);
}

static double get_double(char const* name, StringView d_string, size_t* number_of_decimals)
{
    auto d = d_string.to_number<double>();
    if (!d.has_value()) {
        warnln("{}: invalid argument \"{}\"", name, d_string);
        print_usage(stderr);
        exit(1);
    }

    if (auto dot = d_string.find('.'); dot.has_value())
        *number_of_decimals = d_string.length() - *dot - 1;
    else
        *number_of_decimals = 0;

    return *d;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView separator = "\n"sv;
    StringView terminator = ""sv;
    Vector<StringView> parameters;

    Core::ArgsParser args_parser;
    args_parser.add_option(separator, "Characters to print after each number (default: \\n)", "separator", 's', "separator");
    args_parser.add_option(terminator, "Characters to print at the end of the sequence", "terminator", 't', "terminator");
    args_parser.add_positional_argument(parameters, "1 to 3 parameters, interpreted as LAST, FIRST LAST, or FIRST INCREMENT LAST", "parameters");
    args_parser.parse(arguments);

    double start = 1;
    double step = 1;
    double end = 1;
    size_t number_of_start_decimals = 0;
    size_t number_of_step_decimals = 0;
    size_t number_of_end_decimals = 0;
    switch (parameters.size()) {
    case 1:
        end = get_double(arguments.argv[0], parameters[0], &number_of_end_decimals);
        break;
    case 2:
        start = get_double(arguments.argv[0], parameters[0], &number_of_start_decimals);
        end = get_double(arguments.argv[0], parameters[1], &number_of_end_decimals);
        break;
    case 3:
        start = get_double(arguments.argv[0], parameters[0], &number_of_start_decimals);
        step = get_double(arguments.argv[0], parameters[1], &number_of_step_decimals);
        end = get_double(arguments.argv[0], parameters[2], &number_of_end_decimals);
        break;
    default:
        warnln("{}: unexpected number of arguments", arguments.argv[0]);
        print_usage(stderr);
        return 1;
    }

    if (step == 0) {
        warnln("{}: increment must not be 0", arguments.argv[0]);
        return 1;
    }

    if (__builtin_isnan(start) || __builtin_isnan(step) || __builtin_isnan(end)) {
        warnln("{}: start, step, and end must not be NaN", arguments.argv[0]);
        return 1;
    }

    size_t number_of_decimals = max(number_of_start_decimals, max(number_of_step_decimals, number_of_end_decimals));

    int n = static_cast<int>((end - start) / step);
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
        out("{}{}", buf, separator);
        d += step;
    }

    if (!terminator.is_empty())
        out(terminator);

    return 0;
}
