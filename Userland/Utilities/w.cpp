/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Time.h>
#include <LibCore/Account.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/dev", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/var/run/utmp", "r"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::File::open("/var/run/utmp"sv, Core::File::OpenMode::Read));
    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));
    if (!json.is_object()) {
        warnln("Error: Could not parse /var/run/utmp");
        return 1;
    }

    auto process_statistics = TRY(Core::ProcessStatisticsReader::get_all());

    auto now = Time::now_realtime().to_seconds();

    outln("\033[1m{:10} {:12} {:16} {:6} {}\033[0m", "USER", "TTY", "LOGIN@", "IDLE", "WHAT");
    json.as_object().for_each_member([&](auto& tty, auto& value) {
        const JsonObject& entry = value.as_object();
        auto uid = entry.get_u32("uid"sv).value_or(0);
        [[maybe_unused]] auto pid = entry.get_i32("pid"sv).value_or(0);

        auto login_time = Core::DateTime::from_timestamp(entry.get_integer<time_t>("login_at"sv).value_or(0));
        auto login_at = login_time.to_deprecated_string("%b%d %H:%M:%S"sv);

        auto maybe_account = Core::Account::from_uid(uid, Core::Account::Read::PasswdOnly);
        DeprecatedString username;
        if (!maybe_account.is_error())
            username = maybe_account.release_value().username();
        else
            username = DeprecatedString::number(uid);

        StringBuilder builder;
        DeprecatedString idle_string = "n/a";
        auto maybe_stat = Core::System::stat(tty);
        if (!maybe_stat.is_error()) {
            auto stat = maybe_stat.release_value();
            auto idle_time = now - stat.st_mtime;
            if (idle_time >= 0) {
                builder.appendff("{}s", idle_time);
                idle_string = builder.to_deprecated_string();
            }
        }

        DeprecatedString what = "n/a";

        for (auto& process : process_statistics.processes) {
            if (process.tty == tty && process.pid == process.pgid)
                what = process.name;
        }

        outln("{:10} {:12} {:16} {:6} {}", username, tty, login_at, idle_string, what);
    });
    return 0;
}
