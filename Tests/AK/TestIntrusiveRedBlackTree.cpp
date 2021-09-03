/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <YAK/IntrusiveRedBlackTree.h>
#include <YAK/NonnullOwnPtrVector.h>
#include <YAK/Random.h>

class IntrusiveTest {
public:
    IntrusiveTest(int key, int value)
        : m_tree_node(key)
        , m_some_value(value)
    {
    }

    IntrusiveRedBlackTreeNode<int> m_tree_node;
    int m_some_value;
};

TEST_CASE(construct)
{
    IntrusiveRedBlackTree<int, IntrusiveTest, &IntrusiveTest::m_tree_node> empty;
    EXPECT(empty.is_empty());
    EXPECT(empty.size() == 0);
}

TEST_CASE(ints)
{
    IntrusiveRedBlackTree<int, IntrusiveTest, &IntrusiveTest::m_tree_node> test;
    IntrusiveTest first { 1, 10 };
    test.insert(first);
    IntrusiveTest second { 3, 20 };
    test.insert(second);
    IntrusiveTest third { 2, 30 };
    test.insert(third);
    EXPECT_EQ(test.size(), 3u);
    EXPECT_EQ(test.find(3)->m_some_value, 20);
    EXPECT_EQ(test.find(2)->m_some_value, 30);
    EXPECT_EQ(test.find(1)->m_some_value, 10);
    EXPECT(!test.remove(4));
    EXPECT(test.remove(2));
    EXPECT(test.remove(1));
    EXPECT(test.remove(3));
    EXPECT_EQ(test.size(), 0u);
}

TEST_CASE(largest_smaller_than)
{
    IntrusiveRedBlackTree<int, IntrusiveTest, &IntrusiveTest::m_tree_node> test;
    IntrusiveTest first { 1, 10 };
    test.insert(first);
    IntrusiveTest second { 11, 20 };
    test.insert(second);
    IntrusiveTest third { 21, 30 };
    test.insert(third);
    EXPECT_EQ(test.size(), 3u);
    EXPECT_EQ(test.find_largest_not_above(3)->m_some_value, 10);
    EXPECT_EQ(test.find_largest_not_above(17)->m_some_value, 20);
    EXPECT_EQ(test.find_largest_not_above(22)->m_some_value, 30);
    EXPECT_EQ(test.find_largest_not_above(-5), nullptr);
    VERIFY(test.remove(1));
    VERIFY(test.remove(11));
    VERIFY(test.remove(21));
}

TEST_CASE(key_ordered_iteration)
{
    constexpr auto amount = 10000;
    IntrusiveRedBlackTree<int, IntrusiveTest, &IntrusiveTest::m_tree_node> test;
    NonnullOwnPtrVector<IntrusiveTest> m_entries;
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
        auto entry = make<IntrusiveTest>(keys[i], keys[i]);
        test.insert(*entry);
        m_entries.append(move(entry));
    }

    // check key-ordered iteration
    int index = 0;
    for (auto& value : test) {
        EXPECT(value.m_some_value == index++);
    }

    // ensure we can remove all of them (aka, tree structure is not destroyed somehow)
    for (size_t i = 0; i < amount; i++) {
        EXPECT(test.remove(i));
    }
}

TEST_CASE(clear)
{
    IntrusiveRedBlackTree<int, IntrusiveTest, &IntrusiveTest::m_tree_node> test;
    NonnullOwnPtrVector<IntrusiveTest> m_entries;
    for (size_t i = 0; i < 1000; i++) {
        auto entry = make<IntrusiveTest>(i, i);
        test.insert(*entry);
        m_entries.append(move(entry));
    }
    test.clear();
    EXPECT_EQ(test.size(), 0u);
}
