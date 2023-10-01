/*
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <unistd.h>

TEST_CASE(test_uid_and_gid_high_bits_are_set)
{
    static constexpr auto TEST_FILE_PATH = "/home/anon/.ext2_test";

    auto uid = geteuid();
    EXPECT_EQ(uid, 0u);

    auto fd = open(TEST_FILE_PATH, O_CREAT);
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
