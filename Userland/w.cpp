/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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

    auto file_or_error = Core::File::open("/var/run/utmp", Core::IODevice::ReadOnly);
    if (file_or_error.is_error()) {
        warn() << "Error: " << file_or_error.error();
        return 1;
    }
    auto& file = *file_or_error.value();
    auto json = JsonValue::from_string(file.read_all());
    if (!json.has_value() || !json.value().is_object()) {
        warn() << "Error: Could not parse /var/run/utmp";
        return 1;
    }

    auto process_statistics = Core::ProcessStatisticsReader::get_all();

    auto now = time(nullptr);

    printf("\033[1m%-10s %-12s %-16s %-6s %s\033[0m\n",
        "USER", "TTY", "LOGIN@", "IDLE", "WHAT");
    json.value().as_object().for_each_member([&](auto& tty, auto& value) {
        const JsonObject& entry = value.as_object();
        auto uid = entry.get("uid").to_u32();
        auto pid = entry.get("pid").to_i32();
        (void)pid;

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
                builder.appendf("%ds", idle_time);
                idle_string = builder.to_string();
            }
        }

        String what = "n/a";

        for (auto& it : process_statistics) {
            if (it.value.tty == tty && it.value.pid == it.value.pgid)
                what = it.value.name;
        }

        printf("%-10s %-12s %-16s %-6s %s\n",
            username.characters(),
            tty.characters(),
            login_at.characters(),
            idle_string.characters(),
            what.characters());
    });
    return 0;
}
