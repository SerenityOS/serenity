/*
 * Copyright (c) 2023, Muhammad Zahalqa <m@tryfinally.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/StdLibExtras.h>

TEST_CASE(max_returns_correct_type_category)
{
    int r = 1;
    int const c = 42;

    static_assert(IsSame<decltype(min(r, r)), int&>);
    static_assert(IsSame<decltype(min(c, c)), int const&>);
    static_assert(IsSame<decltype(min(r, c)), int const&>);
    static_assert(IsSame<decltype(min(c, r)), int const&>);

    static_assert(IsSame<decltype(min(r, 2)), int>);
    static_assert(IsSame<decltype(min(2, r)), int>);
    static_assert(IsSame<decltype(min(c, 2)), int>);
    static_assert(IsSame<decltype(min(2, c)), int>);
    static_assert(IsSame<decltype(min(2, 3)), int>);
}

TEST_CASE(min_returns_correct_type_category)
{
    int r = 1;
    int const c = 42;

    static_assert(IsSame<decltype(max(r, r)), int&>);
    static_assert(IsSame<decltype(max(c, c)), int const&>);
    static_assert(IsSame<decltype(max(r, c)), int const&>);
    static_assert(IsSame<decltype(max(c, r)), int const&>);

    static_assert(IsSame<decltype(max(r, 2)), int>);
    static_assert(IsSame<decltype(max(2, r)), int>);
    static_assert(IsSame<decltype(max(c, 2)), int>);
    static_assert(IsSame<decltype(max(2, c)), int>);
    static_assert(IsSame<decltype(max(2, 3)), int>);
}
