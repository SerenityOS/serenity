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

static u8* first_mmap = nullptr;
static u8* second_mmap = nullptr;
size_t const buf_len = 0x1000;

static void shared_non_empty_inode_vmobject_sync_signal_handler(int)
{
    auto rc = msync(first_mmap, buf_len, MS_ASYNC);
    EXPECT(rc == 0);
    rc = munmap(first_mmap, buf_len);
    EXPECT(rc == 0);

    rc = msync(second_mmap, buf_len, MS_ASYNC);
    EXPECT(rc == 0);
    rc = munmap(second_mmap, buf_len);
    EXPECT(rc == 0);
    exit(0);
}

TEST_CASE(shared_non_empty_inode_vmobject_sync)
{
    {
        struct sigaction new_action {
            { shared_non_empty_inode_vmobject_sync_signal_handler }, 0, 0
        };
        int rc = sigaction(SIGBUS, &new_action, nullptr);
        VERIFY(rc == 0);
    }
    size_t mmap_len = buf_len * 2;
    u8 buf[buf_len];
    memset(buf, 0, sizeof(buf));
    int fd = open("/tmp/shared_non_empty_msync_test", O_RDWR | O_CREAT, 0644);
    VERIFY(fd >= 0);
    auto rc = write(fd, buf, sizeof(buf));
    VERIFY(rc == sizeof(buf));
    first_mmap = (u8*)mmap(nullptr, mmap_len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    EXPECT(first_mmap != MAP_FAILED);

    second_mmap = (u8*)mmap(nullptr, mmap_len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    EXPECT(second_mmap != MAP_FAILED);

    // test that changes to one shared mmap are visible in the other shared mmap
    u8 new_val = first_mmap[0] + 1;
    first_mmap[0] = new_val;
    EXPECT(second_mmap[0] == new_val);
    new_val = second_mmap[1] + 1;
    second_mmap[1] = new_val;
    EXPECT(first_mmap[1] == new_val);

    // test that changes in the shared mmap are visible to read()
    new_val = first_mmap[0] + 1;
    first_mmap[0] = new_val;
    rc = msync(first_mmap, mmap_len, MS_SYNC);
    VERIFY(rc == 0);
    rc = lseek(fd, 0, SEEK_SET);
    VERIFY(rc == 0);
    u8 read_byte = 0;
    rc = read(fd, &read_byte, 1);
    VERIFY(rc == 1);
    EXPECT(read_byte == new_val);

    // test that changes made by write() are visible in shared mmaps
    rc = lseek(fd, 0, SEEK_SET);
    VERIFY(rc == 0);
    new_val = first_mmap[0] + 1;
    rc = write(fd, &new_val, 1);
    VERIFY(rc == 1);
    EXPECT(first_mmap[0] == new_val && second_mmap[0] == new_val);

    // test that writes between the file length (buf_len) and mmap_len cause a SIGBUS
    rc = msync(first_mmap, mmap_len, MS_ASYNC);
    EXPECT(rc == 0);
    rc = msync(second_mmap, mmap_len, MS_ASYNC);
    EXPECT(rc == 0);
    first_mmap[buf_len + 1] = 0x1;
    VERIFY_NOT_REACHED();
}
