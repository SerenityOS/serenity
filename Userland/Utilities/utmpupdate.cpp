/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/var/run/utmp", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    pid_t pid = 0;
    bool flag_create = false;
    bool flag_delete = false;
    const char* tty_name = nullptr;
    const char* from = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_create, "Create entry", "create", 'c');
    args_parser.add_option(flag_delete, "Delete entry", "delete", 'd');
    args_parser.add_option(pid, "PID", "PID", 'p', "PID");
    args_parser.add_option(from, "From", "from", 'f', "From");
    args_parser.add_positional_argument(tty_name, "TTY name", "tty");

    args_parser.parse(argc, argv);

    if (flag_create && flag_delete) {
        warnln("-c and -d are mutually exclusive");
        return 1;
    }

    dbgln("Updating utmp from UID={} GID={} EGID={} PID={}", getuid(), getgid(), getegid(), pid);

    auto file_or_error = Core::File::open("/var/run/utmp", Core::OpenMode::ReadWrite);
    if (file_or_error.is_error()) {
        dbgln("Error: {}", file_or_error.error());
        return 1;
    }

    auto& file = *file_or_error.value();

    auto file_contents = file.read_all();
    auto previous_json = JsonValue::from_string(file_contents);

    JsonObject json;

    if (!file_contents.is_empty()) {
        if (!previous_json.has_value() || !previous_json.value().is_object()) {
            dbgln("Error: Could not parse JSON");
        } else {
            json = previous_json.value().as_object();
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

    if (!file.seek(0)) {
        dbgln("Seek failed");
        return 1;
    }

    if (!file.truncate(0)) {
        dbgln("Truncation failed");
        return 1;
    }

    if (!file.write(json.to_string())) {
        dbgln("Write failed");
        return 1;
    }

    return 0;
}
