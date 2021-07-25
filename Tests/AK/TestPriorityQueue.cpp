/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/PriorityQueue.h>
#include <AK/QuickSort.h>
#include <AK/Random.h>

TEST_CASE(test_add_and_remove_values)
{
    AK::PriorityQueue<u32> vals;
    vals.insert(3u);
    vals.insert(1u);
    vals.insert(2u);
    EXPECT_EQ(vals.take(), 1u);
    vals.insert(4u);
    EXPECT_EQ(vals.take(), 2u);
    EXPECT_EQ(vals.take(), 3u);
    EXPECT_EQ(vals.take(), 4u);
}

TEST_CASE(test_add_lots_of_numbers)
{
    AK::Vector<u32> vals;
    vals.resize(1000);
    AK::fill_with_random(vals.data(), vals.size() * sizeof(u32));
    AK::PriorityQueue<u32> queue;
    for (auto v : vals) {
        queue.insert(v);
    }
    AK::quick_sort(vals.begin(), vals.end());
    for (auto v: vals) {
        auto value_from_queue = queue.take();
        EXPECT_EQ(v, value_from_queue);
    }
}