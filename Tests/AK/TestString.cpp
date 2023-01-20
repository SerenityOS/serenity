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

TEST_CASE(move_assignment)
{
    String string1 = MUST(String::from_utf8("hello"sv));
    string1 = MUST(String::from_utf8("friends!"sv));
    EXPECT_EQ(string1, "friends!"sv);
}

TEST_CASE(short_strings)
{
#ifdef AK_ARCH_64_BIT
    auto string1 = MUST(String::from_utf8("abcdefg"sv));
    EXPECT_EQ(string1.is_short_string(), true);
    EXPECT_EQ(string1.bytes().size(), 7u);
    EXPECT_EQ(string1.bytes_as_string_view(), "abcdefg"sv);

    constexpr auto string2 = String::from_utf8_short_string("abcdefg"sv);
    EXPECT_EQ(string2.is_short_string(), true);
    EXPECT_EQ(string2.bytes().size(), 7u);
    EXPECT_EQ(string2, string1);
#else
    auto string1 = MUST(String::from_utf8("abc"sv));
    EXPECT_EQ(string1.is_short_string(), true);
    EXPECT_EQ(string1.bytes().size(), 3u);
    EXPECT_EQ(string1.bytes_as_string_view(), "abc"sv);

    constexpr auto string2 = String::from_utf8_short_string("abc"sv);
    EXPECT_EQ(string2.is_short_string(), true);
    EXPECT_EQ(string2.bytes().size(), 3u);
    EXPECT_EQ(string2, string1);
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

TEST_CASE(reverse)
{
    auto test_reverse = [](auto test, auto expected) {
        auto string = MUST(String::from_utf8(test));
        auto result = MUST(string.reverse());

        EXPECT_EQ(result, expected);
    };

    test_reverse(""sv, ""sv);
    test_reverse("a"sv, "a"sv);
    test_reverse("ab"sv, "ba"sv);
    test_reverse("ab cd ef"sv, "fe dc ba"sv);
    test_reverse("😀"sv, "😀"sv);
    test_reverse("ab😀cd"sv, "dc😀ba"sv);
}

TEST_CASE(to_lowercase)
{
    {
        auto string = MUST(String::from_utf8("Aa"sv));
        auto result = MUST(string.to_lowercase());
        EXPECT_EQ(result, "aa"sv);
    }
    {
        auto string = MUST(String::from_utf8("Ωω"sv));
        auto result = MUST(string.to_lowercase());
        EXPECT_EQ(result, "ωω"sv);
    }
    {
        auto string = MUST(String::from_utf8("İi̇"sv));
        auto result = MUST(string.to_lowercase());
        EXPECT_EQ(result, "i̇i̇"sv);
    }
}

TEST_CASE(to_uppercase)
{
    {
        auto string = MUST(String::from_utf8("Aa"sv));
        auto result = MUST(string.to_uppercase());
        EXPECT_EQ(result, "AA"sv);
    }
    {
        auto string = MUST(String::from_utf8("Ωω"sv));
        auto result = MUST(string.to_uppercase());
        EXPECT_EQ(result, "ΩΩ"sv);
    }
    {
        auto string = MUST(String::from_utf8("ŉ"sv));
        auto result = MUST(string.to_uppercase());
        EXPECT_EQ(result, "ʼN"sv);
    }
}

TEST_CASE(to_titlecase)
{
    {
        auto string = MUST(String::from_utf8("foo bar baz"sv));
        auto result = MUST(string.to_titlecase());
        EXPECT_EQ(result, "Foo Bar Baz"sv);
    }
    {
        auto string = MUST(String::from_utf8("foo \n \r bar \t baz"sv));
        auto result = MUST(string.to_titlecase());
        EXPECT_EQ(result, "Foo \n \r Bar \t Baz"sv);
    }
    {
        auto string = MUST(String::from_utf8("f\"oo\" b'ar'"sv));
        auto result = MUST(string.to_titlecase());
        EXPECT_EQ(result, "F\"Oo\" B'Ar'"sv);
    }
    {
        auto string = MUST(String::from_utf8("123dollars"sv));
        auto result = MUST(string.to_titlecase());
        EXPECT_EQ(result, "123Dollars"sv);
    }
}

TEST_CASE(equals_ignoring_case)
{
    {
        String string1 {};
        String string2 {};

        EXPECT(MUST(string1.equals_ignoring_case(string2)));
    }
    {
        auto string1 = MUST(String::from_utf8("abcd"sv));
        auto string2 = MUST(String::from_utf8("ABCD"sv));
        auto string3 = MUST(String::from_utf8("AbCd"sv));
        auto string4 = MUST(String::from_utf8("dcba"sv));

        EXPECT(MUST(string1.equals_ignoring_case(string2)));
        EXPECT(MUST(string1.equals_ignoring_case(string3)));
        EXPECT(!MUST(string1.equals_ignoring_case(string4)));

        EXPECT(MUST(string2.equals_ignoring_case(string1)));
        EXPECT(MUST(string2.equals_ignoring_case(string3)));
        EXPECT(!MUST(string2.equals_ignoring_case(string4)));

        EXPECT(MUST(string3.equals_ignoring_case(string1)));
        EXPECT(MUST(string3.equals_ignoring_case(string2)));
        EXPECT(!MUST(string3.equals_ignoring_case(string4)));
    }
    {
        auto string1 = MUST(String::from_utf8("\u00DF"sv)); // LATIN SMALL LETTER SHARP S
        auto string2 = MUST(String::from_utf8("SS"sv));
        auto string3 = MUST(String::from_utf8("Ss"sv));
        auto string4 = MUST(String::from_utf8("ss"sv));
        auto string5 = MUST(String::from_utf8("S"sv));
        auto string6 = MUST(String::from_utf8("s"sv));

        EXPECT(MUST(string1.equals_ignoring_case(string2)));
        EXPECT(MUST(string1.equals_ignoring_case(string3)));
        EXPECT(MUST(string1.equals_ignoring_case(string4)));
        EXPECT(!MUST(string1.equals_ignoring_case(string5)));
        EXPECT(!MUST(string1.equals_ignoring_case(string6)));

        EXPECT(MUST(string2.equals_ignoring_case(string1)));
        EXPECT(MUST(string2.equals_ignoring_case(string3)));
        EXPECT(MUST(string2.equals_ignoring_case(string4)));
        EXPECT(!MUST(string2.equals_ignoring_case(string5)));
        EXPECT(!MUST(string2.equals_ignoring_case(string6)));

        EXPECT(MUST(string3.equals_ignoring_case(string1)));
        EXPECT(MUST(string3.equals_ignoring_case(string2)));
        EXPECT(MUST(string3.equals_ignoring_case(string4)));
        EXPECT(!MUST(string3.equals_ignoring_case(string5)));
        EXPECT(!MUST(string3.equals_ignoring_case(string6)));

        EXPECT(MUST(string4.equals_ignoring_case(string1)));
        EXPECT(MUST(string4.equals_ignoring_case(string2)));
        EXPECT(MUST(string4.equals_ignoring_case(string3)));
        EXPECT(!MUST(string4.equals_ignoring_case(string5)));
        EXPECT(!MUST(string4.equals_ignoring_case(string6)));
    }
}

TEST_CASE(is_one_of)
{
    auto foo = MUST(String::from_utf8("foo"sv));
    auto bar = MUST(String::from_utf8("bar"sv));

    EXPECT(foo.is_one_of(foo));
    EXPECT(foo.is_one_of(foo, bar));
    EXPECT(foo.is_one_of(bar, foo));
    EXPECT(!foo.is_one_of(bar));

    EXPECT(!bar.is_one_of("foo"sv));
    EXPECT(bar.is_one_of("foo"sv, "bar"sv));
    EXPECT(bar.is_one_of("bar"sv, "foo"sv));
    EXPECT(bar.is_one_of("bar"sv));
}
