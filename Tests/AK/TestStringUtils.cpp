/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <YAK/StringUtils.h>

TEST_CASE(matches_null)
{
    EXPECT(YAK::StringUtils::matches(StringView(), StringView()));

    EXPECT(!YAK::StringUtils::matches(StringView(), ""));
    EXPECT(!YAK::StringUtils::matches(StringView(), "*"));
    EXPECT(!YAK::StringUtils::matches(StringView(), "?"));
    EXPECT(!YAK::StringUtils::matches(StringView(), "a"));

    EXPECT(!YAK::StringUtils::matches("", StringView()));
    EXPECT(!YAK::StringUtils::matches("a", StringView()));
}

TEST_CASE(matches_empty)
{
    EXPECT(YAK::StringUtils::matches("", ""));

    EXPECT(YAK::StringUtils::matches("", "*"));
    EXPECT(!YAK::StringUtils::matches("", "?"));
    EXPECT(!YAK::StringUtils::matches("", "a"));

    EXPECT(!YAK::StringUtils::matches("a", ""));
}

TEST_CASE(matches_case_sensitive)
{
    EXPECT(YAK::StringUtils::matches("a", "a", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::matches("a", "A", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::matches("A", "a", CaseSensitivity::CaseSensitive));
}

TEST_CASE(matches_case_insensitive)
{
    EXPECT(!YAK::StringUtils::matches("aa", "a"));
    EXPECT(YAK::StringUtils::matches("aa", "*"));
    EXPECT(!YAK::StringUtils::matches("cb", "?a"));
    EXPECT(YAK::StringUtils::matches("adceb", "a*b"));
    EXPECT(!YAK::StringUtils::matches("acdcb", "a*c?b"));
}

TEST_CASE(matches_with_positions)
{
    Vector<YAK::MaskSpan> spans;
    EXPECT(YAK::StringUtils::matches("abbb", "a*", CaseSensitivity::CaseSensitive, &spans));
    EXPECT(spans == Vector<YAK::MaskSpan>({ { 1, 3 } }));

    spans.clear();
    EXPECT(YAK::StringUtils::matches("abbb", "?*", CaseSensitivity::CaseSensitive, &spans));
    EXPECT_EQ(spans, Vector<YAK::MaskSpan>({ { 0, 1 }, { 1, 3 } }));

    spans.clear();
    EXPECT(YAK::StringUtils::matches("acdcxb", "a*c?b", CaseSensitivity::CaseSensitive, &spans));
    EXPECT_EQ(spans, Vector<YAK::MaskSpan>({ { 1, 2 }, { 4, 1 } }));

    spans.clear();
    EXPECT(YAK::StringUtils::matches("aaaa", "A*", CaseSensitivity::CaseInsensitive, &spans));
    EXPECT_EQ(spans, Vector<YAK::MaskSpan>({ { 1, 3 } }));
}

// #4607
TEST_CASE(matches_trailing)
{
    EXPECT(YAK::StringUtils::matches("ab", "ab*"));
    EXPECT(YAK::StringUtils::matches("ab", "ab****"));
    EXPECT(YAK::StringUtils::matches("ab", "*ab****"));
}

TEST_CASE(convert_to_int)
{
    auto value = YAK::StringUtils::convert_to_int(StringView());
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_int("");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_int("a");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_int("+");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_int("-");
    EXPECT(!value.has_value());

    auto actual = YAK::StringUtils::convert_to_int("0");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 0);

    actual = YAK::StringUtils::convert_to_int("1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1);

    actual = YAK::StringUtils::convert_to_int("+1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1);

    actual = YAK::StringUtils::convert_to_int("-1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), -1);

    actual = YAK::StringUtils::convert_to_int("01");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1);

    actual = YAK::StringUtils::convert_to_int("12345");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 12345);

    actual = YAK::StringUtils::convert_to_int("-12345");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), -12345);

    actual = YAK::StringUtils::convert_to_int(" \t-12345 \n\n");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), -12345);

    auto actual_i8 = YAK::StringUtils::convert_to_int<i8>("-1");
    EXPECT(actual_i8.has_value());
    EXPECT_EQ(actual_i8.value(), -1);
    EXPECT_EQ(sizeof(actual_i8.value()), (size_t)1);
    actual_i8 = YAK::StringUtils::convert_to_int<i8>("128");
    EXPECT(!actual_i8.has_value());

    auto actual_i16 = YAK::StringUtils::convert_to_int<i16>("-1");
    EXPECT(actual_i16.has_value());
    EXPECT_EQ(actual_i16.value(), -1);
    EXPECT_EQ(sizeof(actual_i16.value()), (size_t)2);
    actual_i16 = YAK::StringUtils::convert_to_int<i16>("32768");
    EXPECT(!actual_i16.has_value());

    auto actual_i32 = YAK::StringUtils::convert_to_int<i32>("-1");
    EXPECT(actual_i32.has_value());
    EXPECT_EQ(actual_i32.value(), -1);
    EXPECT_EQ(sizeof(actual_i32.value()), (size_t)4);
    actual_i32 = YAK::StringUtils::convert_to_int<i32>("2147483648");
    EXPECT(!actual_i32.has_value());

    auto actual_i64 = YAK::StringUtils::convert_to_int<i64>("-1");
    EXPECT(actual_i64.has_value());
    EXPECT_EQ(actual_i64.value(), -1);
    EXPECT_EQ(sizeof(actual_i64.value()), (size_t)8);
    actual_i64 = YAK::StringUtils::convert_to_int<i64>("9223372036854775808");
    EXPECT(!actual_i64.has_value());
}

TEST_CASE(convert_to_uint)
{
    auto value = YAK::StringUtils::convert_to_uint(StringView());
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_uint("");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_uint("a");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_uint("+");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_uint("-");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_uint("+1");
    EXPECT(!value.has_value());

    value = YAK::StringUtils::convert_to_uint("-1");
    EXPECT(!value.has_value());

    auto actual = YAK::StringUtils::convert_to_uint("0");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 0u);

    actual = YAK::StringUtils::convert_to_uint("1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1u);

    actual = YAK::StringUtils::convert_to_uint("01");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1u);

    actual = YAK::StringUtils::convert_to_uint("12345");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 12345u);

    actual = YAK::StringUtils::convert_to_uint(" \t12345 \n\n");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 12345u);

    auto actual_u8 = YAK::StringUtils::convert_to_uint<u8>("255");
    EXPECT(actual_u8.has_value());
    EXPECT_EQ(actual_u8.value(), 255u);
    EXPECT_EQ(sizeof(actual_u8.value()), (size_t)1);
    actual_u8 = YAK::StringUtils::convert_to_uint<u8>("256");
    EXPECT(!actual_u8.has_value());

    auto actual_u16 = YAK::StringUtils::convert_to_uint<u16>("65535");
    EXPECT(actual_u16.has_value());
    EXPECT_EQ(actual_u16.value(), 65535u);
    EXPECT_EQ(sizeof(actual_u16.value()), (size_t)2);
    actual_u16 = YAK::StringUtils::convert_to_uint<u16>("65536");
    EXPECT(!actual_u16.has_value());

    auto actual_u32 = YAK::StringUtils::convert_to_uint<u32>("4294967295");
    EXPECT(actual_u32.has_value());
    EXPECT_EQ(actual_u32.value(), 4294967295ul);
    EXPECT_EQ(sizeof(actual_u32.value()), (size_t)4);
    actual_u32 = YAK::StringUtils::convert_to_uint<u32>("4294967296");
    EXPECT(!actual_u32.has_value());

    auto actual_u64 = YAK::StringUtils::convert_to_uint<u64>("18446744073709551615");
    EXPECT(actual_u64.has_value());
    EXPECT_EQ(actual_u64.value(), 18446744073709551615ull);
    EXPECT_EQ(sizeof(actual_u64.value()), (size_t)8);
    actual_u64 = YAK::StringUtils::convert_to_uint<u64>("18446744073709551616");
    EXPECT(!actual_u64.has_value());
}

TEST_CASE(ends_with)
{
    String test_string = "ABCDEF";
    EXPECT(YAK::StringUtils::ends_with(test_string, "DEF", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::ends_with(test_string, "ABCDEF", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::ends_with(test_string, "ABCDE", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::ends_with(test_string, "ABCDEFG", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::ends_with(test_string, "def", CaseSensitivity::CaseInsensitive));
    EXPECT(!YAK::StringUtils::ends_with(test_string, "def", CaseSensitivity::CaseSensitive));
}

TEST_CASE(starts_with)
{
    String test_string = "ABCDEF";
    EXPECT(YAK::StringUtils::starts_with(test_string, "ABC", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::starts_with(test_string, "ABCDEF", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::starts_with(test_string, "BCDEF", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::starts_with(test_string, "ABCDEFG", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::starts_with(test_string, "abc", CaseSensitivity::CaseInsensitive));
    EXPECT(!YAK::StringUtils::starts_with(test_string, "abc", CaseSensitivity::CaseSensitive));
}

TEST_CASE(contains)
{
    String test_string = "ABCDEFABCXYZ";
    EXPECT(YAK::StringUtils::contains(test_string, "ABC", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "ABC", CaseSensitivity::CaseInsensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "AbC", CaseSensitivity::CaseInsensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "BCX", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "BCX", CaseSensitivity::CaseInsensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "BcX", CaseSensitivity::CaseInsensitive));
    EXPECT(!YAK::StringUtils::contains(test_string, "xyz", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "xyz", CaseSensitivity::CaseInsensitive));
    EXPECT(!YAK::StringUtils::contains(test_string, "EFG", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::contains(test_string, "EfG", CaseSensitivity::CaseInsensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "", CaseSensitivity::CaseSensitive));
    EXPECT(YAK::StringUtils::contains(test_string, "", CaseSensitivity::CaseInsensitive));
    EXPECT(!YAK::StringUtils::contains("", test_string, CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::contains("", test_string, CaseSensitivity::CaseInsensitive));
    EXPECT(!YAK::StringUtils::contains(test_string, "L", CaseSensitivity::CaseSensitive));
    EXPECT(!YAK::StringUtils::contains(test_string, "L", CaseSensitivity::CaseInsensitive));
}

TEST_CASE(is_whitespace)
{
    EXPECT(YAK::StringUtils::is_whitespace(""));
    EXPECT(YAK::StringUtils::is_whitespace("   "));
    EXPECT(YAK::StringUtils::is_whitespace("  \t"));
    EXPECT(YAK::StringUtils::is_whitespace("  \t\n"));
    EXPECT(YAK::StringUtils::is_whitespace("  \t\n\r\v"));
    EXPECT(!YAK::StringUtils::is_whitespace("  a "));
    EXPECT(!YAK::StringUtils::is_whitespace("a\t"));
}

TEST_CASE(find)
{
    String test_string = "1234567";
    EXPECT_EQ(YAK::StringUtils::find(test_string, "1").value_or(1), 0u);
    EXPECT_EQ(YAK::StringUtils::find(test_string, "2").value_or(2), 1u);
    EXPECT_EQ(YAK::StringUtils::find(test_string, "3").value_or(3), 2u);
    EXPECT_EQ(YAK::StringUtils::find(test_string, "4").value_or(4), 3u);
    EXPECT_EQ(YAK::StringUtils::find(test_string, "5").value_or(5), 4u);
    EXPECT_EQ(YAK::StringUtils::find(test_string, "34").value_or(3), 2u);
    EXPECT_EQ(YAK::StringUtils::find(test_string, "78").has_value(), false);
}

TEST_CASE(to_snakecase)
{
    EXPECT_EQ(YAK::StringUtils::to_snakecase("foobar"), "foobar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("Foobar"), "foobar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("FOOBAR"), "foobar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("fooBar"), "foo_bar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("FooBar"), "foo_bar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("fooBAR"), "foo_bar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("FOOBar"), "foo_bar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("foo_bar"), "foo_bar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("FBar"), "f_bar");
    EXPECT_EQ(YAK::StringUtils::to_snakecase("FooB"), "foo_b");
}

TEST_CASE(to_titlecase)
{
    EXPECT_EQ(YAK::StringUtils::to_titlecase(""sv), ""sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("f"sv), "F"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("foobar"sv), "Foobar"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("Foobar"sv), "Foobar"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("FOOBAR"sv), "Foobar"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("foo bar"sv), "Foo Bar"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("foo bAR"sv), "Foo Bar"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("foo  bar"sv), "Foo  Bar"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("foo   bar"sv), "Foo   Bar"sv);
    EXPECT_EQ(YAK::StringUtils::to_titlecase("   foo   bar   "sv), "   Foo   Bar   "sv);
}
