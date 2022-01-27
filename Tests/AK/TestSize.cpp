/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Size.h>
#include <AK/Vector.h>

STATICTEST_CASE(size_of_array)
{
    int array[8] {};
    EXPECT_EQ(size(array), 8u);
}
RUN_STATICTEST_CASE(size_of_array);

STATICTEST_CASE(size_of_Array)
{
    Array<int, 8> array {};
    EXPECT_EQ(size(array), 8u);
}
RUN_STATICTEST_CASE(size_of_Array);

TEST_CASE(size_of_SinglyLinkedList)
{
    SinglyLinkedList<int> list {};
    EXPECT_EQ(size(list), 0u);

    list.append(42);
    EXPECT_EQ(size(list), 1u);
}

TEST_CASE(size_of_Vector)
{
    Vector<int> vector {};
    EXPECT_EQ(size(vector), 0u);

    vector.append(42);
    EXPECT_EQ(size(vector), 1u);
}
