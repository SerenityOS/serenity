/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/SinglyLinkedList.h>

static SinglyLinkedList<int> make_list()
{
    SinglyLinkedList<int> list {};
    list.append(0);
    list.append(1);
    list.append(2);
    list.append(3);
    list.append(4);
    list.append(5);
    list.append(6);
    list.append(7);
    list.append(8);
    list.append(9);
    return list;
}

TEST_CASE(should_find_mutable)
{
    auto sut = make_list();

    EXPECT_EQ(4, *sut.find(4));

    EXPECT_EQ(sut.end(), sut.find(42));
}

TEST_CASE(should_find_mutable_with_predicate)
{
    auto sut = make_list();

    EXPECT_EQ(4, *sut.find_if([](const auto v) { return v == 4; }));

    EXPECT_EQ(sut.end(), sut.find_if([](const auto v) { return v == 42; }));
}

TEST_CASE(should_find_const)
{
    const auto sut = make_list();

    EXPECT_EQ(4, *sut.find(4));

    EXPECT_EQ(sut.end(), sut.find(42));
}

TEST_CASE(should_find_const_with_predicate)
{
    const auto sut = make_list();

    EXPECT_EQ(4, *sut.find_if([](const auto v) { return v == 4; }));

    EXPECT_EQ(sut.end(), sut.find_if([](const auto v) { return v == 42; }));
}

TEST_CASE(removal_during_iteration)
{
    auto list = make_list();
    auto size = list.size_slow();

    for (auto it = list.begin(); it != list.end(); ++it, --size) {
        VERIFY(list.size_slow() == size);
        it.remove(list);
    }
}
