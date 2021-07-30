/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <sys/mman.h>

TEST_CASE(munmap_zero_page)
{
    // munmap of the unmapped zero page should always "succeed".
    auto res = munmap(0x0, 0xF);
    EXPECT_EQ(res, 0);
}
