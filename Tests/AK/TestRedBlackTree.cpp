/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Random.h>
#include <AK/RedBlackTree.h>

TEST_CASE(construct)
{
    RedBlackTree<int, int> empty;
    EXPECT(empty.is_empty());
    EXPECT(empty.size() == 0);
}

TEST_CASE(ints)
{
    RedBlackTree<int, int> ints;
    ints.insert(1, 10);
    ints.insert(3, 20);
    ints.insert(2, 30);
    EXPECT_EQ(ints.size(), 3u);
    EXPECT_EQ(*ints.find(3), 20);
    EXPECT_EQ(*ints.find(2), 30);
    EXPECT_EQ(*ints.find(1), 10);
    EXPECT(!ints.remove(4));
    EXPECT(ints.remove(2));
    EXPECT(ints.remove(1));
    EXPECT(ints.remove(3));
    EXPECT_EQ(ints.size(), 0u);
}

TEST_CASE(largest_smaller_than)
{
    RedBlackTree<int, int> ints;
    ints.insert(1, 10);
    ints.insert(11, 20);
    ints.insert(21, 30);
    EXPECT_EQ(ints.size(), 3u);
    EXPECT_EQ(*ints.find_largest_not_above(3), 10);
    EXPECT_EQ(*ints.find_largest_not_above(17), 20);
    EXPECT_EQ(*ints.find_largest_not_above(22), 30);
    EXPECT_EQ(ints.find_largest_not_above(-5), nullptr);
}

TEST_CASE(key_ordered_iteration)
{
    constexpr auto amount = 10000;
    RedBlackTree<int, size_t> test;
    Array<int, amount> keys {};

    // generate random key order
    for (int i = 0; i < amount; i++) {
        keys[i] = i;
    }
    for (size_t i = 0; i < amount; i++) {
        swap(keys[i], keys[get_random<size_t>() % amount]);
    }

    // insert random keys
    for (size_t i = 0; i < amount; i++) {
        test.insert(keys[i], keys[i]);
    }

    // check key-ordered iteration
    size_t index = 0;
    for (auto& value : test) {
        EXPECT(value == index++);
    }

    // ensure we can remove all of them (aka, tree structure is not destroyed somehow)
    for (size_t i = 0; i < amount; i++) {
        EXPECT(test.remove(i));
    }
}

TEST_CASE(clear)
{
    RedBlackTree<size_t, size_t> test;
    for (size_t i = 0; i < 1000; i++) {
        test.insert(i, i);
    }
    test.clear();
    EXPECT_EQ(test.size(), 0u);
}
