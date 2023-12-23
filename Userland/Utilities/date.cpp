/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <time.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio settime rpath"));

    bool print_unix_date = false;
    bool print_iso_8601 = false;
    bool print_rfc_3339 = false;
    bool print_rfc_5322 = false;
    StringView set_date;
    StringView format_string;

    Core::ArgsParser args_parser;
    args_parser.add_option(set_date, "Set system date and time", "set", 's', "date");
    args_parser.add_option(print_unix_date, "Print date as Unix timestamp", "unix", 'u');
    args_parser.add_option(print_iso_8601, "Print date in ISO 8601 format", "iso-8601", 'i');
    args_parser.add_option(print_rfc_3339, "Print date in RFC 3339 format", "rfc-3339", 'r');
    args_parser.add_option(print_rfc_5322, "Print date in RFC 5322 format", "rfc-5322", 'R');
    args_parser.add_positional_argument(format_string, "Custom format to print the date in", "format-string", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!set_date.is_empty()) {
        auto number = set_date.to_number<unsigned>();

        if (!number.has_value()) {
            warnln("date: Invalid timestamp value");
            return 1;
        }

        timespec ts = { number.value(), 0 };
        TRY(Core::System::clock_settime(CLOCK_REALTIME, &ts));

        return 0;
    }

    if (print_unix_date + print_iso_8601 + print_rfc_3339 + print_rfc_5322 + !format_string.is_null() > 1) {
        warnln("date: Multiple output formats specified");
        return 1;
    }

    auto date = Core::DateTime::now();
    if (!format_string.is_null()) {
        // FIXME: If the string argument does not start with a '+' sign, POSIX says
        //        we should parse that as a date, and set the system time to it.
        if (format_string.length() == 0 || format_string[0] != '+') {
            warnln("date: Format string must start with '+'");
            return 1;
        }
        outln("{}", date.to_byte_string(format_string.substring_view(1)));
    } else if (print_unix_date) {
        outln("{}", date.timestamp());
    } else if (print_iso_8601) {
        outln("{}", date.to_byte_string("%Y-%m-%dT%H:%M:%S%:z"sv));
    } else if (print_rfc_5322) {
        outln("{}", date.to_byte_string("%a, %d %b %Y %H:%M:%S %z"sv));
    } else if (print_rfc_3339) {
        outln("{}", date.to_byte_string("%Y-%m-%d %H:%M:%S%:z"sv));
    } else {
        outln("{}", date.to_byte_string("%Y-%m-%d %H:%M:%S %Z"sv));
    }
    return 0;
}
