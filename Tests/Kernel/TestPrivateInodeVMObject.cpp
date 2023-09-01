/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

static u8* private_ptr = nullptr;

static void private_non_empty_inode_vmobject_sync_signal_handler(int)
{
    auto rc = msync(private_ptr, 0x1000, MS_ASYNC);
    EXPECT(rc == 0);
    rc = munmap(private_ptr, 0x1000);
    EXPECT(rc == 0);
    exit(0);
}

TEST_CASE(private_non_empty_inode_vmobject_sync)
{
    {
        struct sigaction new_action {
            { private_non_empty_inode_vmobject_sync_signal_handler }, 0, 0
        };
        int rc = sigaction(SIGBUS, &new_action, nullptr);
        VERIFY(rc == 0);
    }
    u8 buf[0x1000];
    memset(buf, 0, sizeof(buf));
    int fd = open("/tmp/private_non_empty_msync_test", O_RDWR | O_CREAT, 0644);
    VERIFY(fd >= 0);
    auto rc = write(fd, buf, sizeof(buf));
    VERIFY(rc == sizeof(buf));
    private_ptr = (u8*)mmap(nullptr, 0x2000, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, 0);
    EXPECT(private_ptr != MAP_FAILED);
    rc = msync(private_ptr, 0x2000, MS_ASYNC);
    EXPECT(rc == 0);
    private_ptr[0x1001] = 0x1;
    VERIFY_NOT_REACHED();
}
