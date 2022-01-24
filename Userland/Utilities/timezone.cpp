/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Try.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibTimeZone/TimeZone.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));
    TRY(Core::System::unveil("/etc/timezone", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView time_zone;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(time_zone, "The time zone to set", "time-zone", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (time_zone.is_empty()) {
        outln("{}", TimeZone::current_time_zone());
        return 0;
    }

    TRY(TimeZone::change_time_zone(time_zone));
    return 0;
}
