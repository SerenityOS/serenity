/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <AK/Time.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/sysmacros.h>

static ErrorOr<String> tty_stat_to_pseudo_name(struct stat const& tty_stat)
{
    int tty_device_major = major(tty_stat.st_rdev);
    int tty_device_minor = minor(tty_stat.st_rdev);

    if (tty_device_major == 201) {
        return String::formatted("pts:{}", tty_device_minor);
    }

    if (tty_device_major == 4) {
        return String::formatted("tty:{}", tty_device_minor);
    }

    return Error::from_string_literal("Unknown TTY device type");
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/dev", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/var/run/utmp", "r"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool hide_header = false;
    StringView username_to_filter_by;

    Core::ArgsParser args_parser;
    args_parser.add_option(hide_header, "Don't show the header", "no-header", 'h');
    args_parser.add_positional_argument(username_to_filter_by, "Only show information about the specified user", "user", Core::ArgsParser::Required::No);
    args_parser.parse(args);

    auto file = TRY(Core::File::open("/var/run/utmp"sv, Core::File::OpenMode::Read));
    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));
    if (!json.is_object()) {
        warnln("Error: Could not parse /var/run/utmp");
        return 1;
    }

    auto process_statistics = TRY(Core::ProcessStatisticsReader::get_all());

    auto now = UnixDateTime::now().seconds_since_epoch();

    if (!hide_header)
        outln("\033[1m{:10} {:12} {:16} {:6} {}\033[0m", "USER", "TTY", "LOGIN@", "IDLE", "WHAT");

    TRY(json.as_object().try_for_each_member([&](ByteString const& tty, auto& value) -> ErrorOr<void> {
        JsonObject const& entry = value.as_object();
        auto uid = entry.get_u32("uid"sv).value_or(0);
        [[maybe_unused]] auto pid = entry.get_i32("pid"sv).value_or(0);

        auto login_time = Core::DateTime::from_timestamp(entry.get_integer<time_t>("login_at"sv).value_or(0));
        auto login_at = TRY(login_time.to_string("%b%d %H:%M:%S"sv));

        auto maybe_account = Core::Account::from_uid(uid, Core::Account::Read::PasswdOnly);
        String username;
        if (!maybe_account.is_error())
            username = TRY(String::from_byte_string(maybe_account.release_value().username()));
        else
            username = TRY(String::formatted("{}", uid));

        if (!username_to_filter_by.is_empty() && username_to_filter_by != username)
            return {};

        StringBuilder builder;
        String idle_string = "n/a"_string;
        String what = "n/a"_string;
        StringView tty_display_name = tty;
        auto maybe_stat = Core::System::stat(tty);
        if (!maybe_stat.is_error()) {
            auto stat = maybe_stat.release_value();
            auto idle_time = now - stat.st_mtime;
            if (idle_time >= 0) {
                builder.appendff("{}s", idle_time);
                idle_string = TRY(builder.to_string());
            }

            auto tty_pseudo_name = TRY(tty_stat_to_pseudo_name(stat));
            tty_display_name = tty_pseudo_name;
            for (auto& process : process_statistics.processes) {
                if (tty_pseudo_name == process.tty.view() && process.pid == process.pgid) {
                    what = TRY(String::from_byte_string(process.name));
                    break;
                }
            }
        }

        outln("{:10} {:12} {:16} {:6} {}", username, tty_display_name, login_at, idle_string, what);
        return {};
    }));
    return 0;
}
