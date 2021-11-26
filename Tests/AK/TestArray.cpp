/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>

static constexpr int constexpr_sum(const Span<const int> span)
{
    int sum = 0;
    for (auto value : span)
        sum += value;

    return sum;
}

TEST_CASE(compile_time_contructible)
{
    constexpr Array<int, 4> array = { 0, 1, 2, 3 };
    static_assert(array.size() == 4);
}

TEST_CASE(compile_time_iterable)
{
    constexpr Array<int, 8> array = { 0, 1, 2, 3, 4, 5, 6, 7 };
    static_assert(constexpr_sum(array) == 28);
}
