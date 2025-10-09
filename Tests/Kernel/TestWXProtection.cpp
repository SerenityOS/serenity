/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <sys/mman.h>

TEST_CASE(ax_protection)
{
    // Anonymous executable mmaps should fail.
    void* p = mmap(nullptr, PAGE_SIZE, PROT_EXEC, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    EXPECT_EQ(p, MAP_FAILED);
    EXPECT_EQ(errno, EINVAL);
}

TEST_CASE(basic_wx_protection)
{
    int fd = open("/bin/SystemServer", O_RDONLY);
    EXPECT_NE(fd, -1);

    // Writable and executable mmaps should fail.
    void* p = mmap(nullptr, PAGE_SIZE, PROT_WRITE | PROT_EXEC, MAP_PRIVATE, fd, 0);
    EXPECT_EQ(p, MAP_FAILED);
    EXPECT_EQ(errno, EINVAL);
}

TEST_CASE(advanced_wx_protection)
{
    int fd = open("/bin/SystemServer", O_RDONLY);
    EXPECT_NE(fd, -1);

    // Memory that was previously writable cannot become executable.
    void* p = mmap(nullptr, PAGE_SIZE, PROT_WRITE, MAP_PRIVATE, fd, 0);
    EXPECT_NE(p, MAP_FAILED);

    EXPECT_EQ(mprotect(p, PAGE_SIZE, PROT_EXEC), -1);
    EXPECT_EQ(errno, EINVAL);

    // Memory that was previously executable cannot become writable.
    p = mmap(nullptr, PAGE_SIZE, PROT_EXEC, MAP_PRIVATE, fd, 0);
    EXPECT_NE(p, MAP_FAILED);

    EXPECT_EQ(mprotect(p, PAGE_SIZE, PROT_WRITE), -1);
    EXPECT_EQ(errno, EINVAL);
}
