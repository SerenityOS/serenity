/*
 * Copyright (c) 2021, David Isaksson <davidisaksson93@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <sys/ioctl.h>

static ErrorOr<void> fetch_ioctl(int fd, int request)
{
    u64 value;
    TRY(Core::System::ioctl(fd, request, &value));
    outln("{}", value);
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::unveil("/dev", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio rpath"));

    StringView device;

    bool flag_get_disk_size = false;
    bool flag_get_block_size = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Call block device ioctls");
    args_parser.add_option(flag_get_disk_size, "Get size in bytes", "size", 's');
    args_parser.add_option(flag_get_block_size, "Get block size in bytes", "block-size", 'b');
    args_parser.add_positional_argument(device, "Device to query", "device");
    args_parser.parse(arguments);

    int fd = TRY(Core::System::open(device, O_RDONLY));

    if (flag_get_disk_size) {
        TRY(fetch_ioctl(fd, STORAGE_DEVICE_GET_SIZE));
    }
    if (flag_get_block_size) {
        TRY(fetch_ioctl(fd, STORAGE_DEVICE_GET_BLOCK_SIZE));
    }

    return 0;
}
