/*
 * Copyright (c) 2024, sdomi <ja@sdomi.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

static void test_write_path(char const* path)
{
    struct stat st;

    auto fd = open(path, O_RDWR | O_CREAT);
    EXPECT_NE(fd, -1);

    EXPECT_NE(fstat(fd, &st), -1);
    EXPECT(st.st_blocks == 0);
    EXPECT_EQ(st.st_size, 0);

    auto rc = write(fd, "meow", 4);
    EXPECT_NE(rc, 0);

    EXPECT_NE(fstat(fd, &st), -1);

    EXPECT(st.st_blocks > 0);
    EXPECT(st.st_size > 0);
    close(fd);
    unlink(path);
}

TEST_CASE(reported_blocksize_ramfs)
{
    test_write_path("/tmp/asdf");
}

TEST_CASE(reported_blocksize_ext2fs)
{
    test_write_path("/home/anon/asdf");
}

// TODO: automate testing of FatFS/FUSE/...
