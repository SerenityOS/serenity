/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Try.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>

TEST_CASE(construct_empty)
{
    String empty;
    EXPECT(empty.is_empty());
    EXPECT_EQ(empty.bytes().size(), 0u);

    auto empty2 = MUST(String::from_utf8(""sv));
    EXPECT(empty2.is_empty());
    EXPECT_EQ(empty, empty2);
    EXPECT_EQ(empty, ""sv);
}

TEST_CASE(short_strings)
{
#ifdef AK_ARCH_64_BIT
    auto string = MUST(String::from_utf8("abcdefg"sv));
    EXPECT_EQ(string.is_short_string(), true);
    EXPECT_EQ(string.bytes().size(), 7u);
    EXPECT_EQ(string.bytes_as_string_view(), "abcdefg"sv);
#else
    auto string = MUST(String::from_utf8("abc"sv));
    EXPECT_EQ(string.is_short_string(), true);
    EXPECT_EQ(string.bytes().size(), 3u);
    EXPECT_EQ(string.bytes_as_string_view(), "abc"sv);
#endif
}

TEST_CASE(long_strings)
{
    auto string = MUST(String::from_utf8("abcdefgh"sv));
    EXPECT_EQ(string.is_short_string(), false);
    EXPECT_EQ(string.bytes().size(), 8u);
    EXPECT_EQ(string.bytes_as_string_view(), "abcdefgh"sv);
}

TEST_CASE(substring)
{
    auto superstring = MUST(String::from_utf8("Hello I am a long string"sv));
    auto short_substring = MUST(superstring.substring_from_byte_offset(0, 5));
    EXPECT_EQ(short_substring, "Hello"sv);

    auto long_substring = MUST(superstring.substring_from_byte_offset(0, 10));
    EXPECT_EQ(long_substring, "Hello I am"sv);
}

TEST_CASE(code_points)
{
    auto string = MUST(String::from_utf8("🦬🪒"sv));

    Vector<u32> code_points;
    for (auto code_point : string.code_points())
        code_points.append(code_point);

    EXPECT_EQ(code_points[0], 0x1f9acu);
    EXPECT_EQ(code_points[1], 0x1fa92u);
}

TEST_CASE(string_builder)
{
    StringBuilder builder;
    builder.append_code_point(0x1f9acu);
    builder.append_code_point(0x1fa92u);

    auto string = MUST(builder.to_string());
    EXPECT_EQ(string, "🦬🪒"sv);
    EXPECT_EQ(string.bytes().size(), 8u);
}

TEST_CASE(ak_format)
{
    auto foo = MUST(String::formatted("Hello {}", MUST(String::from_utf8("friends"sv))));
    EXPECT_EQ(foo, "Hello friends"sv);
}

TEST_CASE(replace)
{
    {
        auto haystack = MUST(String::from_utf8("Hello enemies"sv));
        auto result = MUST(haystack.replace("enemies"sv, "friends"sv, ReplaceMode::All));
        EXPECT_EQ(result, "Hello friends"sv);
    }

    {
        auto base_title = MUST(String::from_utf8("anon@courage:~"sv));
        auto result = MUST(base_title.replace("[*]"sv, "(*)"sv, ReplaceMode::FirstOnly));
        EXPECT_EQ(result, "anon@courage:~"sv);
    }
}
