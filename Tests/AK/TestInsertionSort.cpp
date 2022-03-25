/*
 * Copyright (c) 2022, Marc Luqué <marc.luque@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/InsertionSort.h>
#include <AK/Random.h>
#include <AK/Vector.h>

static constexpr int const n = 10;
static constexpr int const iterations = 10;
static constexpr int const seed = 1337;

TEST_CASE(sorts_ascending)
{
    srand(seed);

    for (int i = 0; i < iterations; ++i) {
        Vector<int, n> ints;
        for (int j = 0; j < n; ++j)
            ints.append(get_random<int>());
        Vector<int, n> ints_copy = ints;

        insertion_sort(ints);
        insertion_sort(ints_copy);

        for (int j = 0; j < n - 1; ++j) {
            EXPECT(ints[j] <= ints[j + 1]);
            EXPECT_EQ(ints[j], ints_copy[j]);
            EXPECT_EQ(ints[j + 1], ints_copy[j + 1]);
        }
    }
}

TEST_CASE(sorts_decending)
{
    srand(seed);

    for (int i = 0; i < iterations; ++i) {
        Vector<int, n> ints;
        for (int j = 0; j < n; ++j)
            ints.append(get_random<int>());
        Vector<int, n> ints_copy = ints;

        insertion_sort(ints, [](auto& a, auto& b) { return a > b; });
        insertion_sort(ints_copy, [](auto& a, auto& b) { return a > b; });

        for (int j = 0; j < n - 1; ++j) {
            EXPECT(ints[j] >= ints[j + 1]);
            EXPECT_EQ(ints[j], ints_copy[j]);
            EXPECT_EQ(ints[j + 1], ints_copy[j + 1]);
        }
    }
}
