/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Optional.h>
#include <AK/String.h>

TEST_CASE(basic_optional)
{
    Optional<int> x;
    EXPECT_EQ(x.has_value(), false);
    x = 3;
    EXPECT_EQ(x.has_value(), true);
    EXPECT_EQ(x.value(), 3);
}

TEST_CASE(move_optional)
{
    Optional<int> x;
    EXPECT_EQ(x.has_value(), false);
    x = 3;
    EXPECT_EQ(x.has_value(), true);
    EXPECT_EQ(x.value(), 3);

    Optional<int> y;
    y = move(x);
    EXPECT_EQ(y.has_value(), true);
    EXPECT_EQ(y.value(), 3);
    EXPECT_EQ(x.has_value(), false);
}

TEST_CASE(optional_leak_1)
{
    struct Structure {
        Optional<String> str;
    };

    // This used to leak, it does not anymore.
    Vector<Structure> vec;
    vec.append({ "foo" });
    EXPECT_EQ(vec[0].str.has_value(), true);
    EXPECT_EQ(vec[0].str.value(), "foo");
}

TEST_CASE(short_notation)
{
    Optional<StringView> value = "foo";

    EXPECT_EQ(value->length(), 3u);
    EXPECT_EQ(*value, "foo");
}

TEST_CASE(comparison_without_values)
{
    Optional<StringView> opt0;
    Optional<StringView> opt1;
    Optional<String> opt2;
    EXPECT_EQ(opt0, opt1);
    EXPECT_EQ(opt0, opt2);
}

TEST_CASE(comparison_with_values)
{
    Optional<StringView> opt0;
    Optional<StringView> opt1 = "foo";
    Optional<String> opt2 = "foo";
    Optional<StringView> opt3 = "bar";
    EXPECT_NE(opt0, opt1);
    EXPECT_EQ(opt1, opt2);
    EXPECT_NE(opt1, opt3);
}

TEST_CASE(comparison_to_underlying_types)
{
    Optional<String> opt0;
    EXPECT_NE(opt0, String());
    EXPECT_NE(opt0, "foo");

    Optional<StringView> opt1 = "foo";
    EXPECT_EQ(opt1, "foo");
    EXPECT_NE(opt1, "bar");
    EXPECT_EQ(opt1, String("foo"));
}

TEST_CASE(comparison_with_numeric_types)
{
    Optional<u8> opt0;
    EXPECT_NE(opt0, 0);
    Optional<u8> opt1 = 7;
    EXPECT_EQ(opt1, 7);
    EXPECT_EQ(opt1, 7.0);
    EXPECT_EQ(opt1, 7u);
    EXPECT_NE(opt1, -2);
}
