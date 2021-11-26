/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <cstring>
#include <sys/stat.h>
#include <unistd.h>

TEST_CASE(test_process_fd_readlink)
{
    // Make sure that stdin, stdout and stderr are actually symlinks that point somewhere interesting
    // Sadly we can't assume that they all point to the same file.
    struct stat stat_buf = {};
    struct stat lstat_buf = {};
    auto rc = stat("/proc/self/fd/0", &stat_buf);
    EXPECT_EQ(rc, 0);
    rc = lstat("/proc/self/fd/0", &lstat_buf);
    EXPECT_EQ(rc, 0);
    EXPECT_NE(0, memcmp(&stat_buf, &lstat_buf, sizeof(struct stat)));

    stat_buf = {};
    lstat_buf = {};
    rc = stat("/proc/self/fd/1", &stat_buf);
    EXPECT_EQ(rc, 0);
    rc = lstat("/proc/self/fd/1", &lstat_buf);
    EXPECT_EQ(rc, 0);
    EXPECT_NE(0, memcmp(&stat_buf, &lstat_buf, sizeof(struct stat)));

    stat_buf = {};
    lstat_buf = {};
    rc = stat("/proc/self/fd/2", &stat_buf);
    EXPECT_EQ(rc, 0);
    rc = lstat("/proc/self/fd/2", &lstat_buf);
    EXPECT_EQ(rc, 0);
    EXPECT_NE(0, memcmp(&stat_buf, &lstat_buf, sizeof(struct stat)));

    // Create a new file descriptor that is a dup of 0 with various big values in order to reproduce issue #7820.
    // We should get the same link value for each fd that was duplicated.
    char expected_link[MAXPATHLEN];
    char buf[MAXPATHLEN];

    // Read the symlink for stdin, stdout and stderr
    auto link_length = readlink("/proc/self/fd/0", expected_link, sizeof(expected_link));
    expected_link[link_length] = '\0';

    // 255 is the first broken file descriptor that was discovered and might be used by other software (e.g. bash)
    auto new_fd = dup2(0, 255);
    EXPECT_EQ(new_fd, 255);
    link_length = readlink("/proc/self/fd/255", buf, sizeof(buf));
    buf[link_length] = '\0';
    EXPECT_EQ(0, strcmp(buf, expected_link));

    // 215 is the last fd before we have to encode the fd using more than one byte (due to the offset by FI_MaxStaticFileIndex)
    new_fd = dup2(0, 215);
    EXPECT_EQ(new_fd, 215);
    link_length = readlink("/proc/self/fd/215", buf, sizeof(buf));
    buf[link_length] = '\0';
    EXPECT_EQ(0, strcmp(buf, expected_link));

    // 216 is the first fd that is encoded using more than one byte
    new_fd = dup2(0, 216);
    EXPECT_EQ(new_fd, 216);
    link_length = readlink("/proc/self/fd/216", buf, sizeof(buf));
    buf[link_length] = '\0';
    EXPECT_EQ(0, strcmp(buf, expected_link));

    // 1023 is the largest possible file descriptor
    new_fd = dup2(0, 1023);
    EXPECT_EQ(new_fd, 1023);
    link_length = readlink("/proc/self/fd/1023", buf, sizeof(buf));
    buf[link_length] = '\0';
    EXPECT_EQ(0, strcmp(buf, expected_link));
}
