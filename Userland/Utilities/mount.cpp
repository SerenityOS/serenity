/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

static int parse_options(StringView options)
{
    int flags = 0;
    Vector<StringView> parts = options.split_view(',');
    for (auto& part : parts) {
        if (part == "defaults")
            continue;
        else if (part == "nodev")
            flags |= MS_NODEV;
        else if (part == "noexec")
            flags |= MS_NOEXEC;
        else if (part == "nosuid")
            flags |= MS_NOSUID;
        else if (part == "bind")
            flags |= MS_BIND;
        else if (part == "ro")
            flags |= MS_RDONLY;
        else if (part == "remount")
            flags |= MS_REMOUNT;
        else
            warnln("Ignoring invalid option: {}", part);
    }
    return flags;
}

static bool is_source_none(StringView source)
{
    return source == "none"sv;
}

static int get_source_fd(StringView source)
{
    if (is_source_none(source))
        return -1;
    auto fd_or_error = Core::System::open(source, O_RDWR);
    if (fd_or_error.is_error())
        fd_or_error = Core::System::open(source, O_RDONLY);
    if (fd_or_error.is_error()) {
        int saved_errno = errno;
        auto message = String::formatted("Failed to open: {}\n", source);
        errno = saved_errno;
        perror(message.characters());
    }
    return fd_or_error.release_value();
}

static ErrorOr<void> mount_all()
{
    // Mount all filesystems listed in /etc/fstab.
    dbgln("Mounting all filesystems...");

    auto fstab = TRY(Core::File::open("/etc/fstab", Core::OpenMode::ReadOnly));

    bool all_ok = true;
    while (fstab->can_read_line()) {
        auto line = fstab->read_line();

        // Skip comments and blank lines.
        if (line.is_empty() || line.starts_with("#"))
            continue;

        Vector<String> parts = line.split('\t');
        if (parts.size() < 3) {
            warnln("Invalid fstab entry: {}", line);
            all_ok = false;
            continue;
        }

        auto mountpoint = parts[1];
        auto fstype = parts[2];
        int flags = parts.size() >= 4 ? parse_options(parts[3]) : 0;

        if (mountpoint == "/") {
            dbgln("Skipping mounting root");
            continue;
        }

        auto filename = parts[0];

        int fd = get_source_fd(filename);

        dbgln("Mounting {} ({}) on {}", filename, fstype, mountpoint);

        auto error_or_void = Core::System::mount(fd, mountpoint, fstype, flags);
        if (error_or_void.is_error()) {
            warnln("Failed to mount {} (FD: {}) ({}) on {}: {}", filename, fd, fstype, mountpoint, strerror(errno));
            all_ok = false;
            continue;
        }
    }

    if (all_ok)
        return {};
    else
        return Error::from_string_literal("One or more errors occurred. Please verify earlier output.");
}

static ErrorOr<void> print_mounts()
{
    // Output info about currently mounted filesystems.
    auto df = TRY(Core::File::open("/proc/df", Core::OpenMode::ReadOnly));

    auto content = df->read_all();
    auto json = TRY(JsonValue::from_string(content));

    json.as_array().for_each([](auto& value) {
        auto& fs_object = value.as_object();
        auto class_name = fs_object.get("class_name").to_string();
        auto mount_point = fs_object.get("mount_point").to_string();
        auto source = fs_object.get("source").as_string_or("none");
        auto readonly = fs_object.get("readonly").to_bool();
        auto mount_flags = fs_object.get("mount_flags").to_int();

        out("{} on {} type {} (", source, mount_point, class_name);

        if (readonly || mount_flags & MS_RDONLY)
            out("ro");
        else
            out("rw");

        if (mount_flags & MS_NODEV)
            out(",nodev");
        if (mount_flags & MS_NOEXEC)
            out(",noexec");
        if (mount_flags & MS_NOSUID)
            out(",nosuid");
        if (mount_flags & MS_BIND)
            out(",bind");

        outln(")");
    });

    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView source;
    StringView mountpoint;
    StringView fs_type;
    StringView options;
    bool should_mount_all = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(source, "Source path", "source", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(mountpoint, "Mount point", "mountpoint", Core::ArgsParser::Required::No);
    args_parser.add_option(fs_type, "File system type", nullptr, 't', "fstype");
    args_parser.add_option(options, "Mount options", nullptr, 'o', "options");
    args_parser.add_option(should_mount_all, "Mount all file systems listed in /etc/fstab", nullptr, 'a');
    args_parser.parse(arguments);

    if (should_mount_all) {
        TRY(mount_all());
        return 0;
    }

    if (source.is_empty() && mountpoint.is_empty()) {
        TRY(print_mounts());
        return 0;
    }

    if (!source.is_empty() && !mountpoint.is_empty()) {
        if (fs_type.is_empty())
            fs_type = "ext2";
        int flags = !options.is_empty() ? parse_options(options) : 0;

        int fd = get_source_fd(source);

        TRY(Core::System::mount(fd, mountpoint, fs_type, flags));

        return 0;
    }

    args_parser.print_usage(stderr, arguments.argv[0]);

    return 1;
}
