/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Error.h>
#include <Kernel/API/FileSystem/MountSpecificFlags.h>
#include <Kernel/API/Ioctl.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static ErrorOr<void> set_mount_flag(ByteString key, u64 value, int mount_fd)
{
    MountSpecificFlag flag;
    flag.key_string_length = key.bytes().size();
    flag.key_string_addr = key.bytes().data();
    flag.value_type = MountSpecificFlag::ValueType::UnsignedInteger;
    flag.value_length = 8;
    flag.value_addr = &value;

    return Core::System::ioctl(mount_fd, MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG, &flag);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView fd_string;
    StringView target;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Mount a FUSE-based filesystem");
    args_parser.add_positional_argument(fd_string, "File descriptor to mount", "fd");
    args_parser.add_positional_argument(target, "Path to mount location", "target");
    args_parser.parse(arguments);

    if (fd_string.is_empty())
        return Error::from_string_literal("No file descriptor passed");

    if (target.is_empty())
        return Error::from_string_literal("No target passed");

    auto maybe_fd = fd_string.to_number<int>();
    if (!maybe_fd.has_value())
        return Error::from_string_literal("Invalid file descriptor passed");

    int fd = maybe_fd.release_value();

    int mount_fd = TRY(Core::System::fsopen("FUSE"sv, 0));

    TRY(set_mount_flag("fd", fd, mount_fd));
    TRY(set_mount_flag("rootmode", 40000, mount_fd));

    TRY(Core::System::fsmount({}, mount_fd, -1, target));

    return 0;
}
