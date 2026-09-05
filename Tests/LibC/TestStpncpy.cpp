/*
 * Copyright (c) 2026, Linus Groh <mail@linusgroh.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <string.h>

TEST_CASE(src_longer_than_dest)
{
    char dest[8] = {};
    memset(dest, 'X', sizeof(dest));

    char* result = stpncpy(dest, "Hello World!", sizeof(dest));
    EXPECT_EQ(result, dest + sizeof(dest));
    EXPECT(memcmp(dest, "Hello Wo", sizeof(dest)) == 0);
}

TEST_CASE(src_shorter_than_dest)
{
    char dest[8] = {};
    memset(dest, 'X', sizeof(dest));

    char* result = stpncpy(dest, "Hello", sizeof(dest));
    EXPECT_EQ(result, dest + 5);
    EXPECT(memcmp(dest, "Hello\0\0\0", sizeof(dest)) == 0);
}

TEST_CASE(dest_empty)
{
    char dest[8] = {};
    memset(dest, 'X', sizeof(dest));

    char* result = stpncpy(dest, "Hello World!", 0);
    EXPECT_EQ(result, dest);
    EXPECT(memcmp(dest, "XXXXXXXX", sizeof(dest)) == 0);
}

TEST_CASE(nul_in_src)
{
    char dest[8] = {};
    memset(dest, 'X', sizeof(dest));

    char const src[] = { 'a', 'b', '\0', 'c', 'd' };
    char* result = stpncpy(dest, src, sizeof(dest));
    EXPECT_EQ(result, dest + 2);
    EXPECT(memcmp(dest, "ab\0\0\0\0\0\0", sizeof(dest)) == 0);
}
