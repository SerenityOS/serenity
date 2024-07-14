/*
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <math.h>
#include <stdlib.h>
#include <sys/statvfs.h>
#include <unistd.h>

TEST_CASE(test_uid_and_gid_high_bits_are_set)
{
    static constexpr auto TEST_FILE_PATH = "/home/anon/.ext2_test";

    auto uid = geteuid();
    EXPECT_EQ(uid, 0u);

    auto fd = open(TEST_FILE_PATH, O_CREAT);
    VERIFY(fd != -1);
    auto cleanup_guard = ScopeGuard([&] {
        close(fd);
        unlink(TEST_FILE_PATH);
    });

    EXPECT_EQ(setuid(0), 0);
    EXPECT_EQ(fchown(fd, 65536, 65536), 0);

    struct stat st;
    EXPECT_EQ(fstat(fd, &st), 0);
    EXPECT_EQ(st.st_uid, 65536u);
    EXPECT_EQ(st.st_gid, 65536u);
}

TEST_CASE(test_ext2_writes_and_reads_to_block_ranges)
{
    static constexpr auto TEST_FILE_PATH = "/home/anon/.ext2_test";

    auto fd = open(TEST_FILE_PATH, O_RDWR | O_CREAT);
    VERIFY(fd != -1);
    auto cleanup_guard = ScopeGuard([&] {
        close(fd);
        unlink(TEST_FILE_PATH);
    });

    struct statvfs stvfs;
    int rc = fstatvfs(fd, &stvfs);
    VERIFY(rc != -1);

    size_t block_size = (size_t)stvfs.f_bsize;
    size_t ptrs_per_indirect_block = block_size / sizeof(u32);

    size_t direct_block_count = 12;
    size_t singly_indirect_block_count = ptrs_per_indirect_block;
    size_t doubly_indirect_block_count = pow(ptrs_per_indirect_block, 2);
    size_t triply_indirect_block_count = pow(ptrs_per_indirect_block, 3);

    size_t direct_blocks_capacity = direct_block_count;
    size_t singly_indirect_blocks_capacity = direct_blocks_capacity + singly_indirect_block_count;
    size_t doubly_indirect_blocks_capacity = singly_indirect_blocks_capacity + doubly_indirect_block_count;
    size_t triply_indirect_blocks_capacity = doubly_indirect_blocks_capacity + triply_indirect_block_count;

    char* block_buf = (char*)malloc(block_size);
    block_buf[0] = '!';
    block_buf[block_size - 1] = '!';
    char* read_buf = (char*)malloc(block_size);
    auto malloc_cleanup_guard = ScopeGuard([&] {
        free(block_buf);
        free(read_buf);
    });

    auto write_then_read_block = [&](size_t block) {
        size_t offset = block * block_size;

        // write the block, and verify that write() was successful
        off_t seek_rc = lseek(fd, offset, SEEK_SET);
        VERIFY(seek_rc != -1);
        int nwrite = write(fd, block_buf, block_size);
        EXPECT((size_t)nwrite == block_size);

        // read the block we just wrote, and verify that read() was successful
        seek_rc = lseek(fd, offset, SEEK_SET);
        VERIFY(seek_rc != -1);
        int nread = read(fd, read_buf, block_size);
        EXPECT((size_t)nread == block_size);

        // verify that the block we read back is identical to the block we wrote
        EXPECT(memcmp(read_buf, block_buf, block_size) == 0);
    };

    // run test on the first & last direct blocks
    write_then_read_block(0);
    write_then_read_block(direct_blocks_capacity - 1);

    // run test on the first & last singly indirect blocks
    write_then_read_block(direct_blocks_capacity);
    write_then_read_block(singly_indirect_blocks_capacity - 1);

    // run test on the first & last doubly indirect blocks
    write_then_read_block(singly_indirect_blocks_capacity);
    write_then_read_block(doubly_indirect_blocks_capacity - 1);

    // run test on the first & last triply indirect blocks
    write_then_read_block(doubly_indirect_blocks_capacity);
    write_then_read_block(triply_indirect_blocks_capacity - 1);
}
