/*
 * Copyright (c) 2023, Jonatan Klemets <jonatan.r.klemets@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibWeb/HTML/Numbers.h>

TEST_CASE(parse_integer)
{
    auto optional_value = Web::HTML::parse_integer(""sv);
    EXPECT(!optional_value.has_value());

    optional_value = Web::HTML::parse_integer("123"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 123);

    optional_value = Web::HTML::parse_integer(" 456"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 456);

    optional_value = Web::HTML::parse_integer("789 "sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 789);

    optional_value = Web::HTML::parse_integer("   22   "sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 22);

    optional_value = Web::HTML::parse_integer(" \n\t31\t\t\n\n"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 31);

    optional_value = Web::HTML::parse_integer("765foo"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 765);

    optional_value = Web::HTML::parse_integer("3;"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 3);

    optional_value = Web::HTML::parse_integer("foo765"sv);
    EXPECT(!optional_value.has_value());

    optional_value = Web::HTML::parse_integer("1"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 1);

    optional_value = Web::HTML::parse_integer("+2"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 2);

    optional_value = Web::HTML::parse_integer("-3"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), -3);
}

TEST_CASE(parse_non_negative_integer)
{
    auto optional_value = Web::HTML::parse_non_negative_integer(""sv);
    EXPECT(!optional_value.has_value());

    optional_value = Web::HTML::parse_non_negative_integer("123"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 123u);

    optional_value = Web::HTML::parse_non_negative_integer(" 456"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 456u);

    optional_value = Web::HTML::parse_non_negative_integer("789 "sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 789u);

    optional_value = Web::HTML::parse_non_negative_integer("   22   "sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 22u);

    optional_value = Web::HTML::parse_non_negative_integer(" \n\t31\t\t\n\n"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 31u);

    optional_value = Web::HTML::parse_non_negative_integer("765foo"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 765u);

    optional_value = Web::HTML::parse_non_negative_integer("3;"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 3u);

    optional_value = Web::HTML::parse_non_negative_integer("foo765"sv);
    EXPECT(!optional_value.has_value());

    optional_value = Web::HTML::parse_non_negative_integer("1"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 1u);

    optional_value = Web::HTML::parse_non_negative_integer("+2"sv);
    EXPECT(optional_value.has_value());
    EXPECT_EQ(optional_value.value(), 2u);

    optional_value = Web::HTML::parse_non_negative_integer("-3"sv);
    EXPECT(!optional_value.has_value());
}
