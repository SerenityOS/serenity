/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath chown", nullptr));

    if (arguments.strings.size() < 3) {
        warnln("usage: chown <uid[:gid]> <path>");
        return 1;
    }

    uid_t new_uid = -1;
    gid_t new_gid = -1;

    auto parts = arguments.strings[1].split_view(':', true);
    if (parts.is_empty()) {
        warnln("Empty uid/gid spec");
        return 1;
    }
    if (parts[0].is_empty() || (parts.size() == 2 && parts[1].is_empty()) || parts.size() > 2) {
        warnln("Invalid uid/gid spec");
        return 1;
    }

    auto number = parts[0].to_uint();
    if (number.has_value()) {
        new_uid = number.value();
    } else {
        auto passwd = TRY(Core::System::getpwnam(parts[0]));
        new_uid = passwd.pw_uid;
    }

    if (parts.size() == 2) {
        auto number = parts[1].to_uint();
        if (number.has_value()) {
            new_gid = number.value();
        } else {
            auto group = TRY(Core::System::getgrnam(parts[1]));
            new_gid = group.gr_gid;
        }
    }

    TRY(Core::System::chown(arguments.strings[2], new_uid, new_gid));

    return 0;
}
