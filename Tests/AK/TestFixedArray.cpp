/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestSuite.h>

#include <AK/FixedArray.h>
#include <AK/String.h>

TEST_CASE(construct)
{
    EXPECT(FixedArray<int>().size() == 0);
}

TEST_CASE(ints)
{
    FixedArray<int> ints(3);
    ints[0] = 0;
    ints[1] = 1;
    ints[2] = 2;
    EXPECT_EQ(ints[0], 0);
    EXPECT_EQ(ints[1], 1);
    EXPECT_EQ(ints[2], 2);

    ints.clear();
    EXPECT_EQ(ints.size(), 0u);
}
