/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Karol Kosek <krkk@serenityos.org>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool pretty_output = false;
    bool output_since = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(pretty_output, "Output only the uptime, in human-readable format", "pretty", 'p');
    args_parser.add_option(output_since, "Show when the system is up since, in yyyy-mm-dd HH:MM:SS format", "since", 's');
    args_parser.parse(arguments);

    TRY(Core::System::unveil("/sys/kernel/uptime"sv, "r"sv));
    TRY(Core::System::unveil("/var/run/utmp"sv, "r"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::File::open("/sys/kernel/uptime"sv, Core::File::OpenMode::Read));

    Array<u8, BUFSIZ> buffer;
    auto read_buffer = TRY(file->read_some(buffer));
    auto maybe_seconds = AK::StringUtils::convert_to_uint(StringView(read_buffer));
    if (!maybe_seconds.has_value())
        return Error::from_string_literal("Couldn't convert to number");
    auto seconds = maybe_seconds.release_value();

    if (output_since) {
        auto since_timestamp = Core::DateTime::now().timestamp() - seconds;
        auto since_time = TRY(Core::DateTime::from_timestamp(since_timestamp).to_string());
        outln("{}", since_time);
    } else if (pretty_output) {
        outln("Up {}", human_readable_time(seconds));
    } else {
        auto current_time = TRY(Core::DateTime::now().to_string());
        // FIXME: To match Linux and the BSDs, we should also include
        // some load averages, but these don't seem to be available yet.

        auto utmp_file = TRY(Core::File::open("/var/run/utmp"sv, Core::File::OpenMode::Read));
        auto utmp_contents = TRY(utmp_file->read_until_eof());
        auto json = TRY(AK::JsonValue::from_string(utmp_contents));
        auto object = json.as_object();
        auto user_count = object.size();

        if (user_count == 1) {
            outln("{} up {}, {} user", current_time, human_readable_digital_time(seconds), user_count);
        } else {
            outln("{} up {}, {} users", current_time, human_readable_digital_time(seconds), user_count);
        }
    }

    return 0;
}
