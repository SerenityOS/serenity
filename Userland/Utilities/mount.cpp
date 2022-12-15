/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringUtils.h>
#include <Kernel/API/FileSystem/MountSpecificFlags.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Stream.h>
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
        else if (part == "wxallowed")
            flags |= MS_WXALLOWED;
        else if (part == "axallowed")
            flags |= MS_AXALLOWED;
        else if (part == "noregular")
            flags |= MS_NOREGULAR;
        else
            warnln("Ignoring invalid option: {}", part);
    }
    return flags;
}

static bool is_source_none(StringView source)
{
    return source == "none"sv;
}

static ErrorOr<int> get_source_fd(StringView source)
{
    if (is_source_none(source))
        return -1;
    auto fd_or_error = Core::System::open(source, O_RDWR);
    if (fd_or_error.is_error())
        fd_or_error = Core::System::open(source, O_RDONLY);
    return fd_or_error;
}

static bool mount_by_line(DeprecatedString const& line)
{
    // Skip comments and blank lines.
    if (line.is_empty() || line.starts_with('#'))
        return true;

    Vector<DeprecatedString> parts = line.split('\t');
    if (parts.size() < 3) {
        warnln("Invalid fstab entry: {}", line);
        return false;
    }

    auto mountpoint = parts[1];
    auto fstype = parts[2];
    int flags = parts.size() >= 4 ? parse_options(parts[3]) : 0;

    if (mountpoint == "/") {
        dbgln("Skipping mounting root");
        return true;
    }

    auto filename = parts[0];

    auto fd_or_error = get_source_fd(filename);
    if (fd_or_error.is_error()) {
        outln("{}", fd_or_error.release_error());
        return false;
    }
    auto const fd = fd_or_error.release_value();

    dbgln("Mounting {} ({}) on {}", filename, fstype, mountpoint);

    auto error_or_void = Core::System::mount(fd, mountpoint, fstype, flags);
    if (error_or_void.is_error()) {
        warnln("Failed to mount {} (FD: {}) ({}) on {}: {}", filename, fd, fstype, mountpoint, error_or_void.error());
        return false;
    }

    return true;
}

static ErrorOr<void> mount_all()
{
    // Mount all filesystems listed in /etc/fstab.
    dbgln("Mounting all filesystems...");
    Array<u8, PAGE_SIZE> buffer;

    bool all_ok = true;
    auto process_fstab_entries = [&](StringView path) -> ErrorOr<void> {
        auto file_unbuffered = TRY(Core::Stream::File::open(path, Core::Stream::OpenMode::Read));
        auto file = TRY(Core::Stream::BufferedFile::create(move(file_unbuffered)));

        while (TRY(file->can_read_line())) {
            auto line = TRY(file->read_line(buffer));

            if (!mount_by_line(line))
                all_ok = false;
        }
        return {};
    };

    if (auto result = process_fstab_entries("/etc/fstab"sv); result.is_error())
        dbgln("Failed to read '/etc/fstab': {}", result.error());

    auto fstab_directory_iterator = Core::DirIterator("/etc/fstab.d", Core::DirIterator::SkipDots);

    if (fstab_directory_iterator.has_error() && fstab_directory_iterator.error() != ENOENT) {
        dbgln("Failed to open /etc/fstab.d: {}", fstab_directory_iterator.error_string());
    } else if (!fstab_directory_iterator.has_error()) {
        while (fstab_directory_iterator.has_next()) {
            auto path = fstab_directory_iterator.next_full_path();
            if (auto result = process_fstab_entries(path); result.is_error())
                dbgln("Failed to read '{}': {}", path, result.error());
        }
    }

    if (all_ok)
        return {};

    return Error::from_string_literal("One or more errors occurred. Please verify earlier output.");
}

static ErrorOr<void> print_mounts()
{
    // Output info about currently mounted filesystems.
    auto df = TRY(Core::Stream::File::open("/sys/kernel/df"sv, Core::Stream::OpenMode::Read));

    auto content = TRY(df->read_until_eof());
    auto json = TRY(JsonValue::from_string(content));

    json.as_array().for_each([](auto& value) {
        auto& fs_object = value.as_object();
        auto class_name = fs_object.get("class_name"sv).to_deprecated_string();
        auto mount_point = fs_object.get("mount_point"sv).to_deprecated_string();
        auto source = fs_object.get("source"sv).as_string_or("none");
        auto readonly = fs_object.get("readonly"sv).to_bool();
        auto mount_flags = fs_object.get("mount_flags"sv).to_int();

        out("{} on {} type {} (", source, mount_point, class_name);

        if (readonly || mount_flags & MS_RDONLY)
            out("ro");
        else
            out("rw");

        if (mount_flags & MS_NODEV)
            out(",nodev");
        if (mount_flags & MS_NOREGULAR)
            out(",noregular");
        if (mount_flags & MS_NOEXEC)
            out(",noexec");
        if (mount_flags & MS_NOSUID)
            out(",nosuid");
        if (mount_flags & MS_BIND)
            out(",bind");
        if (mount_flags & MS_WXALLOWED)
            out(",wxallowed");
        if (mount_flags & MS_AXALLOWED)
            out(",axallowed");

        outln(")");
    });

    return {};
}

static ErrorOr<void> apply_file_system_specific_options(int mount_fd, Vector<String> const& filesystem_specific_options)
{
    for (auto& option : filesystem_specific_options) {
        auto key_value_option = TRY(option.split_limit('=', 2));
        mount_specific_flag flag;
        flag.key_string_length = key_value_option[0].bytes().size();
        flag.key_string_addr = key_value_option[0].bytes().data();
        if (key_value_option.size() > 1 && !key_value_option[1].is_empty()) {
            auto possible_int_value = AK::StringUtils::convert_to_int<i64>(key_value_option[1]);
            auto possible_uint_value = AK::StringUtils::convert_to_uint<u64>(key_value_option[1]);
            if (possible_uint_value.has_value() && possible_int_value.has_value()) {
                flag.value_type = mount_specific_flag::ValueType::UnsignedInteger;
                flag.value_length = 8;
                u64 value = possible_uint_value.value();
                flag.value_addr = &value;
                TRY(Core::System::ioctl(mount_fd, MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG, &flag));
            } else if (possible_int_value.has_value()) {
                VERIFY(!possible_uint_value.has_value());
                flag.value_type = mount_specific_flag::ValueType::SignedInteger;
                flag.value_length = 8;
                u64 value = possible_int_value.value();
                flag.value_addr = &value;
                TRY(Core::System::ioctl(mount_fd, MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG, &flag));
            } else {
                flag.value_type = mount_specific_flag::ValueType::ASCIIString;
                flag.value_addr = key_value_option[1].bytes().data();
                flag.value_length = key_value_option[1].bytes().size();
                TRY(Core::System::ioctl(mount_fd, MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG, &flag));
            }
            continue;
        }
        flag.value_type = mount_specific_flag::ValueType::None;
        flag.value_addr = nullptr;
        flag.value_length = 0;
        TRY(Core::System::ioctl(mount_fd, MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG, &flag));
    }
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView source;
    StringView mountpoint;
    StringView fs_type;
    StringView options;
    Vector<String> filesystem_specific_options;
    bool should_mount_all = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(source, "Source path", "source", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(mountpoint, "Mount point", "mountpoint", Core::ArgsParser::Required::No);
    args_parser.add_option(fs_type, "File system type", nullptr, 't', "fstype");
    args_parser.add_option(options, "Mount options", nullptr, 'o', "options");
    args_parser.add_option(filesystem_specific_options, "Mount filesystem-specific options", "fs-specific-option", 'O', "filesystem-specific options");
    args_parser.add_option(should_mount_all, "Mount all file systems listed in /etc/fstab and /etc/fstab.d/*", nullptr, 'a');
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
            fs_type = "ext2"sv;
        int flags = !options.is_empty() ? parse_options(options) : 0;

        int const source_fd = TRY(get_source_fd(source));

        if (flags & MS_REMOUNT) {
            TRY(Core::System::remount(mountpoint, flags));
            return 0;
        }
        if (flags & MS_BIND) {
            TRY(Core::System::bindmount(source_fd, mountpoint, fs_type, flags));
            return 0;
        }
        int mount_fd = TRY(Core::System::fsopen(fs_type, flags));
        TRY(Core::System::ioctl(mount_fd, MOUNT_IOCTL_SET_FILE_SOURCE, source_fd));
        if (!filesystem_specific_options.is_empty())
            TRY(apply_file_system_specific_options(mount_fd, filesystem_specific_options));
        TRY(Core::System::ioctl(mount_fd, MOUNT_IOCTL_CREATE_FILESYSTEM, 0));
        TRY(Core::System::fsmount(mount_fd, mountpoint));
        return 0;
    }

    args_parser.print_usage(stderr, arguments.argv[0]);

    return 1;
}
