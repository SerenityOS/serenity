/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/Try.h>

TEST_CASE(empty_string)
{
    FlyString fly {};
    EXPECT(fly.is_empty());
    EXPECT_EQ(fly, ""sv);

    // Short strings do not get stored in the fly string table.
    EXPECT_EQ(FlyString::number_of_fly_strings(), 0u);
}

TEST_CASE(short_string)
{
    FlyString fly1 { MUST(String::from_utf8("foo"sv)) };
    EXPECT_EQ(fly1, "foo"sv);

    FlyString fly2 { MUST(String::from_utf8("foo"sv)) };
    EXPECT_EQ(fly2, "foo"sv);

    FlyString fly3 { MUST(String::from_utf8("bar"sv)) };
    EXPECT_EQ(fly3, "bar"sv);

    EXPECT_EQ(fly1, fly2);
    EXPECT_NE(fly1, fly3);
    EXPECT_NE(fly2, fly3);

    EXPECT(fly1.to_string().is_short_string());
    EXPECT(fly2.to_string().is_short_string());
    EXPECT(fly3.to_string().is_short_string());

    // Short strings do not get stored in the fly string table.
    EXPECT_EQ(FlyString::number_of_fly_strings(), 0u);
}

TEST_CASE(long_string)
{
    FlyString fly1 { MUST(String::from_utf8("thisisdefinitelymorethan7bytes"sv)) };
    EXPECT_EQ(fly1, "thisisdefinitelymorethan7bytes"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);

    FlyString fly2 { MUST(String::from_utf8("thisisdefinitelymorethan7bytes"sv)) };
    EXPECT_EQ(fly2, "thisisdefinitelymorethan7bytes"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);

    FlyString fly3 { MUST(String::from_utf8("thisisalsoforsuremorethan7bytes"sv)) };
    EXPECT_EQ(fly3, "thisisalsoforsuremorethan7bytes"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 2u);

    EXPECT_EQ(fly1, fly2);
    EXPECT_NE(fly1, fly3);
    EXPECT_NE(fly2, fly3);

    EXPECT(!fly1.to_string().is_short_string());
    EXPECT(!fly2.to_string().is_short_string());
    EXPECT(!fly3.to_string().is_short_string());
}

TEST_CASE(from_string_view)
{
    auto fly1 = MUST(FlyString::from_utf8("thisisdefinitelymorethan7bytes"sv));
    EXPECT_EQ(fly1, "thisisdefinitelymorethan7bytes"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);

    auto fly2 = MUST(FlyString::from_utf8("thisisdefinitelymorethan7bytes"sv));
    EXPECT_EQ(fly2, "thisisdefinitelymorethan7bytes"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);

    auto fly3 = MUST(FlyString::from_utf8("foo"sv));
    EXPECT_EQ(fly3, "foo"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);

    EXPECT_EQ(fly1, fly2);
    EXPECT_NE(fly1, fly3);
    EXPECT_NE(fly2, fly3);
}

TEST_CASE(fly_string_keep_string_data_alive)
{
    EXPECT_EQ(FlyString::number_of_fly_strings(), 0u);
    {
        FlyString fly {};
        {
            auto string = MUST(String::from_utf8("thisisdefinitelymorethan7bytes"sv));
            fly = FlyString { string };
            EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);
        }

        EXPECT_EQ(fly, "thisisdefinitelymorethan7bytes"sv);
        EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);
    }

    EXPECT_EQ(FlyString::number_of_fly_strings(), 0u);
}

TEST_CASE(moved_fly_string_becomes_empty)
{
    FlyString fly1 {};
    EXPECT(fly1.is_empty());

    FlyString fly2 { MUST(String::from_utf8("thisisdefinitelymorethan7bytes"sv)) };
    EXPECT_EQ(fly2, "thisisdefinitelymorethan7bytes"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);

    fly1 = move(fly2);

    EXPECT(fly2.is_empty());
    EXPECT_EQ(fly1, "thisisdefinitelymorethan7bytes"sv);
    EXPECT_EQ(FlyString::number_of_fly_strings(), 1u);
}
