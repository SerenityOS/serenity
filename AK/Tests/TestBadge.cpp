/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TestSuite.h>

#include <AK/Badge.h>

TEST_CASE(should_provide_underlying_type)
{
    static_assert(IsSame<int, Badge<int>::Type>);
}

TEST_MAIN(Badge)
