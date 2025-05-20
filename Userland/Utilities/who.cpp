/*
 * Copyright (c) 2025, Edward <edward.banner@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

#include <pwd.h>
#include <time.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/var/run/utmp", "r"));
    TRY(Core::System::unveil("/etc", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::File::open("/var/run/utmp"sv, Core::File::OpenMode::Read));
    auto contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(contents));

    json.as_object().for_each_member([&](auto& key, auto& value) {
        auto const& obj = value.as_object();
        auto uid = obj.get_i32("uid"sv).value();
        auto* passwd = getpwuid(uid);
        auto user_name = passwd->pw_name;
        auto login_at = obj.get_i32("login_at"sv).value();
        auto time = static_cast<time_t>(login_at);
        auto* tm = localtime(&time);
        char timebuf[32];
        strftime(timebuf, sizeof(timebuf), "%b %d %H:%M", tm);
        outln("{}\t{}\t{}", user_name, key, timebuf);
    });

    return 0;
}
