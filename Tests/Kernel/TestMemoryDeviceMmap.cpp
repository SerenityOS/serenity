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
#include <inttypes.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

static u8 read_buffer[0x100000];

static ALWAYS_INLINE bool mem_chunk(int fd, u64 base, u64 length)
{
    u64 mmoffset = base % sysconf(_SC_PAGESIZE);
    void* mmp = mmap(NULL, mmoffset + length, PROT_READ, MAP_SHARED, fd, base - mmoffset);
    if (mmp == MAP_FAILED)
        return false;
    if (munmap(mmp, mmoffset + length) < 0)
        perror("munmap");
    return true;
}

enum class ReadResult {
    SeekFailure,
    ReadFailure,
    ReadSuccess,
};

static ALWAYS_INLINE ReadResult read_chunk(int fd, u64 base, u64 length)
{
    VERIFY(length <= sizeof(read_buffer));
    auto rs = lseek(fd, base, SEEK_SET);
    if (rs < 0) {
        fprintf(stderr, "Couldn't seek to offset %" PRIi64 " while verifying: %s\n", base, strerror(errno));
        return ReadResult::SeekFailure;
    }
    if (read(fd, read_buffer, length) < 0)
        return ReadResult::ReadFailure;
    return ReadResult::ReadSuccess;
}

TEST_CASE(test_memory_access_device_read)
{
    int rc = geteuid();
    EXPECT_EQ(rc, 0);

    int fd = open("/dev/mem", O_RDONLY);
    EXPECT(fd >= 0);

    // FIXME: This is expected to work on QEMU machines (both 440FX and Q35),
    // however, it will be much nicer to have some sort of a node in the ProcFS
    // to expose physical memory ranges (e820 memory map).

    auto read_result = read_chunk(fd, 0x0, 0x100000);
    EXPECT_EQ(read_result, ReadResult::ReadFailure);

    read_result = read_chunk(fd, 0xe0000, 0x100000 - 0xe0000);
    EXPECT_EQ(read_result, ReadResult::ReadSuccess);

    read_result = read_chunk(fd, 0x100000, 0x200000 - 0x100000);
    EXPECT_EQ(read_result, ReadResult::ReadFailure);

    read_result = read_chunk(fd, 0xf0000, 70000);
    EXPECT_EQ(read_result, ReadResult::ReadFailure);

    read_result = read_chunk(fd, 0xfffc0000, 16384);
    EXPECT_EQ(read_result, ReadResult::ReadSuccess);

    read_result = read_chunk(fd, 0xfffc0000, 0x100000);
    EXPECT_EQ(read_result, ReadResult::ReadFailure);
}

TEST_CASE(test_memory_access_device_mmap)
{
    int rc = geteuid();
    EXPECT_EQ(rc, 0);

    int fd = open("/dev/mem", O_RDONLY);
    EXPECT(fd >= 0);

    // FIXME: This is expected to work on QEMU machines (both 440FX and Q35),
    // however, it will be much nicer to have some sort of a node in the ProcFS
    // to expose physical memory ranges (e820 memory map).

    auto mmap_result = mem_chunk(fd, 0xe0000, 0x100000 - 0xe0000);
    EXPECT_EQ(mmap_result, true);

    mmap_result = mem_chunk(fd, 0x100000, 0x200000 - 0x100000);
    EXPECT_EQ(mmap_result, false);

    mmap_result = mem_chunk(fd, 0xf0000, 70000);
    EXPECT_EQ(mmap_result, false);

    mmap_result = mem_chunk(fd, 0xfffc0000, 16384);
    EXPECT_EQ(mmap_result, true);

    mmap_result = mem_chunk(fd, 0xfffc0000, 0x100000);
    EXPECT_EQ(mmap_result, false);
}
