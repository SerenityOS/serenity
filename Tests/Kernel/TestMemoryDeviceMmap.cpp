/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <LibTest/TestCase.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

static ALWAYS_INLINE bool mem_chunk(int fd, u64 base, u64 length)
{
    u64 mmoffset = base % sysconf(_SC_PAGESIZE);
    void* mmp = mmap(NULL, mmoffset + length, PROT_READ, MAP_SHARED, fd, base - mmoffset);
    return mmp != MAP_FAILED;
}

TEST_CASE(test_memory_access_device_mmap)
{
    int rc = geteuid();
    EXPECT_EQ(rc, 0);

    int fd = open("/dev/mem", O_RDONLY);
    EXPECT_EQ(fd < 0, false);

    // FIXME: This is expected to work on QEMU machines (both 440FX and Q35),
    // however, it will be much nicer to have some sort of a node in the ProcFS
    // to expose physical memory ranges (e820 memory map).

    auto result = mem_chunk(fd, 0xe0000, 0x100000 - 0xe0000);
    EXPECT_EQ(result, true);

    result = mem_chunk(fd, 0x100000, 0x200000 - 0x100000);
    EXPECT_EQ(result, false);

    result = mem_chunk(fd, 0xf0000, 70000);
    EXPECT_EQ(result, false);

    result = mem_chunk(fd, 0xfffc0000, 16384);
    EXPECT_EQ(result, true);

    result = mem_chunk(fd, 0xfffc0000, 0x100000);
    EXPECT_EQ(result, false);
}
