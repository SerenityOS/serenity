/*
 * Copyright (c) 2021, David Isaksson <davidisaksson93@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

static void fetch_ioctl(int fd, int request)
{
    size_t value;
    if (ioctl(fd, request, &value) < 0) {
        perror("ioctl");
        exit(1);
    }
    outln("{}", value);
}

int main(int argc, char** argv)
{
    if (unveil("/dev", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* device = nullptr;

    bool flag_get_disk_size = false;
    bool flag_get_block_size = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Call block device ioctls");
    args_parser.add_option(flag_get_disk_size, "Get size in bytes", "size", 's');
    args_parser.add_option(flag_get_block_size, "Get block size in bytes", "block-size", 'b');
    args_parser.add_positional_argument(device, "Device to query", "device");
    args_parser.parse(argc, argv);

    int fd = open(device, O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    if (flag_get_disk_size) {
        fetch_ioctl(fd, STORAGE_DEVICE_GET_SIZE);
    }
    if (flag_get_block_size) {
        fetch_ioctl(fd, STORAGE_DEVICE_GET_BLOCK_SIZE);
    }

    return 0;
}
