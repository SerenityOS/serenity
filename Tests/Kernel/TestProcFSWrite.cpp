/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <sys/prctl.h>
#include <unistd.h>

TEST_CASE(check_root)
{
    auto uid = geteuid();
    // This test only makes sense as root.
    EXPECT_EQ(uid, 0u);

    // Before we make the process dumpable, become "fully" root, so that the user cannot tamper with our memory:
    EXPECT_EQ(setuid(0), 0);

    // If running as setuid, the process is automatically marked as non-dumpable, which bars access to /proc/self/.
    // However, that is the easiest guess for a /proc/$PID/ directory, so we'd like to use that.
    // In order to do so, mark this process as dumpable:
    EXPECT_EQ(prctl(PR_SET_DUMPABLE, 1, 0, 0), 0);
}

TEST_CASE(root_writes_to_procfs)
{
    int fd = open("/proc/self/unveil", O_RDWR | O_APPEND | O_CREAT, 0666); // = 6
    if (fd < 0) {
        perror("open");
        dbgln("fd was {}", fd);
        FAIL("open failed?! See debugout");
        return;
    }

    int rc = write(fd, "hello", 5);
    perror("write");
    dbgln("write rc = {}", rc);
    if (rc >= 0) {
        FAIL("Wrote successfully?!");
    }
}
