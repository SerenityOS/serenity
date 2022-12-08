/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/DateTime.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <pwd.h>
#include <sys/stat.h>
#include <time.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/dev", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/var/run/utmp", "r"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::Stream::File::open("/var/run/utmp"sv, Core::Stream::OpenMode::Read));
    auto file_contents = TRY(file->read_all());
    auto json = TRY(JsonValue::from_string(file_contents));
    if (!json.is_object()) {
        warnln("Error: Could not parse /var/run/utmp");
        return 1;
    }

    auto process_statistics = TRY(Core::ProcessStatisticsReader::get_all());

    auto now = time(nullptr);

    outln("\033[1m{:10} {:12} {:16} {:6} {}\033[0m", "USER", "TTY", "LOGIN@", "IDLE", "WHAT");
    json.as_object().for_each_member([&](auto& tty, auto& value) {
        const JsonObject& entry = value.as_object();
        auto uid = entry.get("uid"sv).to_u32();
        [[maybe_unused]] auto pid = entry.get("pid"sv).to_i32();

        auto login_time = Core::DateTime::from_timestamp(entry.get("login_at"sv).to_number<time_t>());
        auto login_at = login_time.to_deprecated_string("%b%d %H:%M:%S"sv);

        auto* pw = getpwuid(uid);
        DeprecatedString username;
        if (pw)
            username = pw->pw_name;
        else
            username = DeprecatedString::number(uid);

        StringBuilder builder;
        DeprecatedString idle_string = "n/a";
        struct stat st;
        if (stat(tty.characters(), &st) == 0) {
            auto idle_time = now - st.st_mtime;
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
