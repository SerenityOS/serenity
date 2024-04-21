/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

int f();
[[gnu::weak]] int f() { return 1; }
int g();

TEST_CASE(weak_symbol_resolution)
{
    EXPECT_EQ(g(), 1);
}
