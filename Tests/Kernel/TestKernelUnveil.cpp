/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <unistd.h>

TEST_CASE(test_failures)
{
    auto res = unveil("/etc", "r");
    if (res < 0)
        FAIL("unveil read only failed");

    res = unveil("/etc", "w");
    if (res >= 0)
        FAIL("unveil write permitted after unveil read only");

    res = unveil("/etc", "x");
    if (res >= 0)
        FAIL("unveil execute permitted after unveil read only");

    res = unveil("/etc", "c");
    if (res >= 0)
        FAIL("unveil create permitted after unveil read only");

    res = unveil("/tmp/doesnotexist", "c");
    if (res < 0)
        FAIL("unveil create on non-existent path failed");

    res = unveil("/home", "b");
    if (res < 0)
        FAIL("unveil browse failed");

    res = unveil("/home", "w");
    if (res >= 0)
        FAIL("unveil write permitted after unveil browse only");

    res = unveil("/home", "x");
    if (res >= 0)
        FAIL("unveil execute permitted after unveil browse only");

    res = unveil("/home", "c");
    if (res >= 0)
        FAIL("unveil create permitted after unveil browse only");

    res = unveil(nullptr, nullptr);
    if (res < 0)
        FAIL("unveil state lock failed");

    res = unveil("/bin", "w");
    if (res >= 0)
        FAIL("unveil permitted after unveil state locked");

    res = access("/bin/id", F_OK);
    if (res == 0)
        FAIL("access(..., F_OK) permitted after locked veil without relevant unveil");
}
