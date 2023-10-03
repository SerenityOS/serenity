/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
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
    bool list_time_zones = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(list_time_zones, "List all available time zones", "list-time-zones", 'l');
    args_parser.add_positional_argument(time_zone, "The time zone to set", "time-zone", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (list_time_zones) {
        for (auto time_zone : TimeZone::all_time_zones())
            outln("{}", time_zone.name);
        return 0;
    }

    if (time_zone.is_empty()) {
        outln("{}", TimeZone::system_time_zone());
        return 0;
    }

    TRY(TimeZone::change_time_zone(time_zone));
    return 0;
}
