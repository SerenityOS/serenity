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
size_t const buf_len = 0x1000;

static void private_non_empty_inode_vmobject_sync_signal_handler(int)
{
    auto rc = msync(private_ptr, buf_len, MS_ASYNC);
    EXPECT(rc == 0);
    rc = munmap(private_ptr, buf_len);
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
    int mmap_len = buf_len * 2;
    u8 buf[buf_len];
    memset(buf, 0, sizeof(buf));
    int fd = open("/tmp/private_non_empty_msync_test", O_RDWR | O_CREAT, 0644);
    VERIFY(fd >= 0);
    auto rc = write(fd, buf, sizeof(buf));
    VERIFY(rc == sizeof(buf));
    private_ptr = (u8*)mmap(nullptr, mmap_len, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, 0);
    EXPECT(private_ptr != MAP_FAILED);

    // test that writes to the private mmap are not visible to read()
    u8 old_val = private_ptr[0];
    private_ptr[0] = old_val + 1;
    rc = msync(private_ptr, mmap_len, MS_SYNC);
    VERIFY(rc == 0);
    rc = lseek(fd, 0, SEEK_SET);
    VERIFY(rc == 0);
    u8 read_byte = 0;
    rc = read(fd, &read_byte, 1);
    VERIFY(rc == 1);
    EXPECT(read_byte == old_val);

    // test that writes between the file length (buf_len) and mmap_len cause a SIGBUS
    rc = msync(private_ptr, mmap_len, MS_ASYNC);
    EXPECT(rc == 0);
    private_ptr[buf_len + 1] = 0x1;
    VERIFY_NOT_REACHED();
}
