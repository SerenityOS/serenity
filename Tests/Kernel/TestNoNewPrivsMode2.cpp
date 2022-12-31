/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <sys/prctl.h>
#include <unistd.h>

TEST_CASE(test_no_new_privs_mode_2)
{
    auto uid = geteuid();
    // This test only makes sense as root.
    EXPECT_EQ(uid, 0u);
    EXPECT_EQ(setuid(0), 0);

    int rc = prctl(PR_SET_NO_NEW_PRIVS, NO_NEW_PRIVS_MODE_ENFORCED, 0, 0);
    EXPECT_EQ(rc, 0);

    rc = setuid(0);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EPERM);

    rc = setgid(0);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EPERM);

    rc = seteuid(0);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EPERM);

    rc = setegid(0);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EPERM);
}
