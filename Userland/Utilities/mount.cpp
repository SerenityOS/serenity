/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Optional.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
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

static bool is_source_none(const char* source)
{
    return !strcmp("none", source);
}

static int get_source_fd(const char* source)
{
    if (is_source_none(source))
        return -1;
    int fd = open(source, O_RDWR);
    if (fd < 0)
        fd = open(source, O_RDONLY);
    if (fd < 0) {
        int saved_errno = errno;
        auto message = String::formatted("Failed to open: {}\n", source);
        errno = saved_errno;
        perror(message.characters());
    }
    return fd;
}

static bool mount_all()
{
    // Mount all filesystems listed in /etc/fstab.
    dbgln("Mounting all filesystems...");

    auto fstab = Core::File::construct("/etc/fstab");
    if (!fstab->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", fstab->name(), fstab->error_string());
        return false;
    }

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

        const char* mountpoint = parts[1].characters();
        const char* fstype = parts[2].characters();
        int flags = parts.size() >= 4 ? parse_options(parts[3]) : 0;

        if (strcmp(mountpoint, "/") == 0) {
            dbgln("Skipping mounting root");
            continue;
        }

        const char* filename = parts[0].characters();

        int fd = get_source_fd(filename);

        dbgln("Mounting {} ({}) on {}", filename, fstype, mountpoint);

        int rc = mount(fd, mountpoint, fstype, flags);
        if (rc != 0) {
            warnln("Failed to mount {} (FD: {}) ({}) on {}: {}", filename, fd, fstype, mountpoint, strerror(errno));
            all_ok = false;
            continue;
        }
    }

    return all_ok;
}

static bool print_mounts()
{
    // Output info about currently mounted filesystems.
    auto df = Core::File::construct("/proc/df");
    if (!df->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", df->name(), df->error_string());
        return false;
    }

    auto content = df->read_all();
    auto json = JsonValue::from_string(content).release_value_but_fixme_should_propagate_errors();

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

    return true;
}

int main(int argc, char** argv)
{
    const char* source = nullptr;
    const char* mountpoint = nullptr;
    const char* fs_type = nullptr;
    const char* options = nullptr;
    bool should_mount_all = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(source, "Source path", "source", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(mountpoint, "Mount point", "mountpoint", Core::ArgsParser::Required::No);
    args_parser.add_option(fs_type, "File system type", nullptr, 't', "fstype");
    args_parser.add_option(options, "Mount options", nullptr, 'o', "options");
    args_parser.add_option(should_mount_all, "Mount all file systems listed in /etc/fstab", nullptr, 'a');
    args_parser.parse(argc, argv);

    if (should_mount_all) {
        return mount_all() ? 0 : 1;
    }

    if (!source && !mountpoint)
        return print_mounts() ? 0 : 1;

    if (source && mountpoint) {
        if (!fs_type)
            fs_type = "ext2";
        int flags = options ? parse_options(options) : 0;

        int fd = get_source_fd(source);

        if (mount(fd, mountpoint, fs_type, flags) < 0) {
            perror("mount");
            return 1;
        }
        return 0;
    }

    args_parser.print_usage(stderr, argv[0]);
    return 1;
}
