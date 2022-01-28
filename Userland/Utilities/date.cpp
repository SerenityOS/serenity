/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <time.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio settime rpath", nullptr));

    bool print_unix_date = false;
    bool print_iso_8601 = false;
    bool print_rfc_3339 = false;
    bool print_rfc_5322 = false;
    const char* set_date = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(set_date, "Set system date and time", "set", 's', "date");
    args_parser.add_option(print_unix_date, "Print date as Unix timestamp", "unix", 'u');
    args_parser.add_option(print_iso_8601, "Print date in ISO 8601 format", "iso-8601", 'i');
    args_parser.add_option(print_rfc_3339, "Print date in RFC 3339 format", "rfc-3339", 'r');
    args_parser.add_option(print_rfc_5322, "Print date in RFC 5322 format", "rfc-5322", 'R');
    args_parser.parse(arguments);

    if (set_date != nullptr) {
        auto number = String(set_date).to_uint();

        if (!number.has_value()) {
            warnln("date: Invalid timestamp value");
            return 1;
        }

        timespec ts = { number.value(), 0 };
        TRY(Core::System::clock_settime(CLOCK_REALTIME, &ts));

        return 0;
    }

    // FIXME: this should be improved and will need to be cleaned up
    // when additional output formats and formatting is supported
    if (print_unix_date && print_iso_8601 && print_rfc_3339 && print_rfc_5322) {
        warnln("date: multiple output formats specified");
        return 1;
    }

    auto date = Core::DateTime::now();
    if (print_unix_date) {
        outln("{}", date.timestamp());
    } else if (print_iso_8601) {
        outln("{}", date.to_string("%Y-%m-%dT%H:%M:%S%:z"));
    } else if (print_rfc_5322) {
        outln("{}", date.to_string("%a, %d %b %Y %H:%M:%S %z"));
    } else if (print_rfc_3339) {
        outln("{}", date.to_string("%Y-%m-%d %H:%M:%S%:z"));
    } else {
        outln("{}", date.to_string("%Y-%m-%d %H:%M:%S %Z"));
    }
    return 0;
}
