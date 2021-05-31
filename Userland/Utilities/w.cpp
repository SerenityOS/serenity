/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <pwd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <time.h>

int main()
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/dev", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/var/run/utmp", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto file_or_error = Core::File::open("/var/run/utmp", Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Error: {}", file_or_error.error());
        return 1;
    }
    auto& file = *file_or_error.value();
    auto json = JsonValue::from_string(file.read_all());
    if (!json.has_value() || !json.value().is_object()) {
        warnln("Error: Could not parse /var/run/utmp");
        return 1;
    }

    auto process_statistics = Core::ProcessStatisticsReader::get_all();
    if (!process_statistics.has_value()) {
        warnln("Error: Could not get process statistics");
        return 1;
    }

    auto now = time(nullptr);

    outln("\033[1m{:10} {:12} {:16} {:6} {}\033[0m", "USER", "TTY", "LOGIN@", "IDLE", "WHAT");
    json.value().as_object().for_each_member([&](auto& tty, auto& value) {
        const JsonObject& entry = value.as_object();
        auto uid = entry.get("uid").to_u32();
        [[maybe_unused]] auto pid = entry.get("pid").to_i32();

        auto login_time = Core::DateTime::from_timestamp(entry.get("login_at").to_number<time_t>());
        auto login_at = login_time.to_string("%b%d %H:%M:%S");

        auto* pw = getpwuid(uid);
        String username;
        if (pw)
            username = pw->pw_name;
        else
            username = String::number(uid);

        StringBuilder builder;
        String idle_string = "n/a";
        struct stat st;
        if (stat(tty.characters(), &st) == 0) {
            auto idle_time = now - st.st_mtime;
            if (idle_time >= 0) {
                builder.appendff("{}s", idle_time);
                idle_string = builder.to_string();
            }
        }

        String what = "n/a";

        for (auto& process : process_statistics.value()) {
            if (process.tty == tty && process.pid == process.pgid)
                what = process.name;
        }

        outln("{:10} {:12} {:16} {:6} {}", username, tty, login_at, idle_string, what);
    });
    return 0;
}
