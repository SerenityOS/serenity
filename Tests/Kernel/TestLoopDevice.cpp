/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/StringView.h>
#include <Kernel/API/Ioctl.h>
#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>

static int open_loop_device(int loop_device_index)
{
    auto loop_device_path = ByteString::formatted("/dev/loop/{}", loop_device_index);
    return open(loop_device_path.view().characters_without_null_termination(), O_RDONLY);
}

TEST_CASE(create_attach_and_destory_loop_device)
{
    constexpr char const* test_path = "/tmp/create_attach_and_destory_loop_device_test";

    int devctl_fd = open("/dev/devctl", O_RDONLY);
    VERIFY(devctl_fd >= 0);
    auto cleanup_devctl_fd_guard = ScopeGuard([&] {
        close(devctl_fd);
    });

    u8 buf[0x1000];
    memset(buf, 0, sizeof(buf));
    int fd = open(test_path, O_RDWR | O_CREAT, 0644);
    VERIFY(fd >= 0);
    auto cleanup_fd_guard = ScopeGuard([&] {
        close(fd);
        unlink(test_path);
    });
    auto rc = write(fd, buf, sizeof(buf));
    VERIFY(rc == sizeof(buf));

    int value = fd;
    auto create_result = ioctl(devctl_fd, DEVCTL_CREATE_LOOP_DEVICE, &value);
    EXPECT_EQ(create_result, 0);

    auto loop_device_index = value;
    auto loop_device_fd_or_error = open_loop_device(loop_device_index);
    EXPECT(loop_device_fd_or_error >= 0);
    auto cleanup_loop_device_fd_guard = ScopeGuard([&] {
        close(loop_device_fd_or_error);
    });

    auto destroy_result = ioctl(devctl_fd, DEVCTL_DESTROY_LOOP_DEVICE, &loop_device_index);
    EXPECT_EQ(destroy_result, 0);
}
