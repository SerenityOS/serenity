/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

TEST_CASE(exec_should_not_search_current_directory)
{
    int fd = open("hax", O_CREAT | O_RDWR, 0755);
    ftruncate(fd, 0);
    close(fd);

    int rc = execlp("hax", "hax", nullptr);
    int saved_errno = errno;
    perror("execlp");
    unlink("hax");

    EXPECT_EQ(rc, -1);
    EXPECT_NE(saved_errno, ENOEXEC);
}
