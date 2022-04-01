/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath cpath"));
    TRY(Core::System::unveil("/var/run/utmp", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    pid_t pid = 0;
    bool flag_create = false;
    bool flag_delete = false;
    char const* tty_name = nullptr;
    char const* from = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_create, "Create entry", "create", 'c');
    args_parser.add_option(flag_delete, "Delete entry", "delete", 'd');
    args_parser.add_option(pid, "PID", "PID", 'p', "PID");
    args_parser.add_option(from, "From", "from", 'f', "From");
    args_parser.add_positional_argument(tty_name, "TTY name", "tty");
    args_parser.parse(arguments);

    if (flag_create && flag_delete) {
        warnln("-c and -d are mutually exclusive");
        return 1;
    }

    dbgln("Updating utmp from UID={} GID={} EGID={} PID={}", getuid(), getgid(), getegid(), pid);

    auto file = TRY(Core::File::open("/var/run/utmp", Core::OpenMode::ReadWrite));

    auto file_contents = file->read_all();
    auto previous_json = TRY(JsonValue::from_string(file_contents));

    JsonObject json;

    if (!file_contents.is_empty()) {
        if (!previous_json.is_object()) {
            dbgln("Error: Could not parse JSON");
        } else {
            json = previous_json.as_object();
        }
    }

    if (flag_create) {
        JsonObject entry;
        entry.set("pid", pid);
        entry.set("uid", getuid());
        entry.set("from", from);
        entry.set("login_at", time(nullptr));
        json.set(tty_name, move(entry));
    } else {
        VERIFY(flag_delete);
        dbgln("Removing {} from utmp", tty_name);
        json.remove(tty_name);
    }

    if (!file->seek(0)) {
        dbgln("Seek failed");
        return 1;
    }

    if (!file->truncate(0)) {
        dbgln("Truncation failed");
        return 1;
    }

    if (!file->write(json.to_string())) {
        dbgln("Write failed");
        return 1;
    }

    return 0;
}
