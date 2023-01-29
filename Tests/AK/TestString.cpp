/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// This is included first on purpose. We specifically do not want LibTest to override VERIFY here so
// that we can actually test that some String factory methods cause a crash with invalid input.
#include <AK/String.h>

#include <LibTest/TestCase.h>

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

TEST_CASE(from_code_points)
{
    for (u32 code_point = 0; code_point < 0x80; ++code_point) {
        auto string = String::from_code_point(code_point);

        auto ch = static_cast<char>(code_point);
        StringView view { &ch, 1 };

        EXPECT_EQ(string, view);
    }

    auto string = String::from_code_point(0x10ffff);
    EXPECT_EQ(string, "\xF4\x8F\xBF\xBF"sv);

    EXPECT_CRASH("Creating a string from an invalid code point", [] {
        String::from_code_point(0xffffffff);
        return Test::Crash::Failure::DidNotCrash;
    });
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

TEST_CASE(split)
{
    {
        auto test = MUST(String::from_utf8("foo bar baz"sv));
        auto parts = MUST(test.split(' '));
        EXPECT_EQ(parts.size(), 3u);
        EXPECT_EQ(parts[0], "foo");
        EXPECT_EQ(parts[1], "bar");
        EXPECT_EQ(parts[2], "baz");
    }
    {
        auto test = MUST(String::from_utf8("ωΣ2ωΣω"sv));
        auto parts = MUST(test.split(0x03A3u));
        EXPECT_EQ(parts.size(), 3u);
        EXPECT_EQ(parts[0], "ω"sv);
        EXPECT_EQ(parts[1], "2ω"sv);
        EXPECT_EQ(parts[2], "ω"sv);
    }
}

TEST_CASE(find_byte_offset)
{
    {
        String string {};
        auto index1 = string.find_byte_offset(0);
        EXPECT(!index1.has_value());

        auto index2 = string.find_byte_offset(""sv);
        EXPECT(!index2.has_value());
    }
    {
        auto string = MUST(String::from_utf8("foo"sv));

        auto index1 = string.find_byte_offset('f');
        EXPECT_EQ(index1, 0u);

        auto index2 = string.find_byte_offset('o');
        EXPECT_EQ(index2, 1u);

        auto index3 = string.find_byte_offset('o', *index2 + 1);
        EXPECT_EQ(index3, 2u);

        auto index4 = string.find_byte_offset('b');
        EXPECT(!index4.has_value());
    }
    {
        auto string = MUST(String::from_utf8("foo"sv));

        auto index1 = string.find_byte_offset("fo"sv);
        EXPECT_EQ(index1, 0u);

        auto index2 = string.find_byte_offset("oo"sv);
        EXPECT_EQ(index2, 1u);

        auto index3 = string.find_byte_offset("o"sv, *index2 + 1);
        EXPECT_EQ(index3, 2u);

        auto index4 = string.find_byte_offset("fooo"sv);
        EXPECT(!index4.has_value());
    }
    {
        auto string = MUST(String::from_utf8("ωΣωΣω"sv));

        auto index1 = string.find_byte_offset(0x03C9U);
        EXPECT_EQ(index1, 0u);

        auto index2 = string.find_byte_offset(0x03A3u);
        EXPECT_EQ(index2, 2u);

        auto index3 = string.find_byte_offset(0x03C9U, 2);
        EXPECT_EQ(index3, 4u);

        auto index4 = string.find_byte_offset(0x03A3u, 4);
        EXPECT_EQ(index4, 6u);

        auto index5 = string.find_byte_offset(0x03C9U, 6);
        EXPECT_EQ(index5, 8u);
    }
    {
        auto string = MUST(String::from_utf8("ωΣωΣω"sv));

        auto index1 = string.find_byte_offset("ω"sv);
        EXPECT_EQ(index1, 0u);

        auto index2 = string.find_byte_offset("Σ"sv);
        EXPECT_EQ(index2, 2u);

        auto index3 = string.find_byte_offset("ω"sv, 2);
        EXPECT_EQ(index3, 4u);

        auto index4 = string.find_byte_offset("Σ"sv, 4);
        EXPECT_EQ(index4, 6u);

        auto index5 = string.find_byte_offset("ω"sv, 6);
        EXPECT_EQ(index5, 8u);
    }
}

TEST_CASE(repeated)
{
    {
        auto string1 = MUST(String::repeated('a', 0));
        EXPECT(string1.is_short_string());
        EXPECT(string1.is_empty());

        auto string2 = MUST(String::repeated(0x03C9U, 0));
        EXPECT(string2.is_short_string());
        EXPECT(string2.is_empty());

        auto string3 = MUST(String::repeated(0x10300, 0));
        EXPECT(string3.is_short_string());
        EXPECT(string3.is_empty());
    }
    {
        auto string1 = MUST(String::repeated('a', 1));
        EXPECT(string1.is_short_string());
        EXPECT_EQ(string1.bytes_as_string_view().length(), 1u);
        EXPECT_EQ(string1, "a"sv);

        auto string2 = MUST(String::repeated(0x03C9U, 1));
        EXPECT(string2.is_short_string());
        EXPECT_EQ(string2.bytes_as_string_view().length(), 2u);
        EXPECT_EQ(string2, "ω"sv);

        auto string3 = MUST(String::repeated(0x10300, 1));
#ifdef AK_ARCH_64_BIT
        EXPECT(string3.is_short_string());
#else
        EXPECT(!string3.is_short_string());
#endif
        EXPECT_EQ(string3.bytes_as_string_view().length(), 4u);
        EXPECT_EQ(string3, "𐌀"sv);
    }
    {
        auto string1 = MUST(String::repeated('a', 3));
        EXPECT(string1.is_short_string());
        EXPECT_EQ(string1.bytes_as_string_view().length(), 3u);
        EXPECT_EQ(string1, "aaa"sv);

        auto string2 = MUST(String::repeated(0x03C9U, 3));
#ifdef AK_ARCH_64_BIT
        EXPECT(string2.is_short_string());
#else
        EXPECT(!string2.is_short_string());
#endif
        EXPECT_EQ(string2.bytes_as_string_view().length(), 6u);
        EXPECT_EQ(string2, "ωωω"sv);

        auto string3 = MUST(String::repeated(0x10300, 3));
        EXPECT(!string3.is_short_string());
        EXPECT_EQ(string3.bytes_as_string_view().length(), 12u);
        EXPECT_EQ(string3, "𐌀𐌀𐌀"sv);
    }
    {
        auto string1 = MUST(String::repeated('a', 10));
        EXPECT(!string1.is_short_string());
        EXPECT_EQ(string1.bytes_as_string_view().length(), 10u);
        EXPECT_EQ(string1, "aaaaaaaaaa"sv);

        auto string2 = MUST(String::repeated(0x03C9U, 10));
        EXPECT(!string2.is_short_string());
        EXPECT_EQ(string2.bytes_as_string_view().length(), 20u);
        EXPECT_EQ(string2, "ωωωωωωωωωω"sv);

        auto string3 = MUST(String::repeated(0x10300, 10));
        EXPECT(!string3.is_short_string());
        EXPECT_EQ(string3.bytes_as_string_view().length(), 40u);
        EXPECT_EQ(string3, "𐌀𐌀𐌀𐌀𐌀𐌀𐌀𐌀𐌀𐌀"sv);
    }

    EXPECT_CRASH("Creating a string from an invalid code point", [] {
        (void)String::repeated(0xffffffff, 1);
        return Test::Crash::Failure::DidNotCrash;
    });
}

TEST_CASE(join)
{
    auto string1 = MUST(String::join(',', Vector<i32> {}));
    EXPECT(string1.is_empty());

    auto string2 = MUST(String::join(',', Array { 1 }));
    EXPECT_EQ(string2, "1"sv);

    auto string3 = MUST(String::join(':', Array { 1 }, "[{}]"sv));
    EXPECT_EQ(string3, "[1]"sv);

    auto string4 = MUST(String::join(',', Array { 1, 2, 3 }));
    EXPECT_EQ(string4, "1,2,3"sv);

    auto string5 = MUST(String::join(',', Array { 1, 2, 3 }, "[{}]"sv));
    EXPECT_EQ(string5, "[1],[2],[3]"sv);

    auto string6 = MUST(String::join(String::from_utf8_short_string("!!!"sv), Array { "foo"sv, "bar"sv, "baz"sv }));
    EXPECT_EQ(string6, "foo!!!bar!!!baz"sv);

    auto string7 = MUST(String::join(" - "sv, Array { 1, 16, 256, 4096 }, "[{:#04x}]"sv));
    EXPECT_EQ(string7, "[0x0001] - [0x0010] - [0x0100] - [0x1000]"sv);
}

TEST_CASE(trim)
{
    {
        String string {};

        auto result = MUST(string.trim(" "sv, TrimMode::Both));
        EXPECT(result.is_empty());

        result = MUST(string.trim(" "sv, TrimMode::Left));
        EXPECT(result.is_empty());

        result = MUST(string.trim(" "sv, TrimMode::Right));
        EXPECT(result.is_empty());
    }
    {
        auto string = MUST(String::from_utf8("word"sv));

        auto result = MUST(string.trim(" "sv, TrimMode::Both));
        EXPECT_EQ(result, "word"sv);

        result = MUST(string.trim(" "sv, TrimMode::Left));
        EXPECT_EQ(result, "word"sv);

        result = MUST(string.trim(" "sv, TrimMode::Right));
        EXPECT_EQ(result, "word"sv);
    }
    {
        auto string = MUST(String::from_utf8("    word"sv));

        auto result = MUST(string.trim(" "sv, TrimMode::Both));
        EXPECT_EQ(result, "word"sv);

        result = MUST(string.trim(" "sv, TrimMode::Left));
        EXPECT_EQ(result, "word"sv);

        result = MUST(string.trim(" "sv, TrimMode::Right));
        EXPECT_EQ(result, "    word"sv);
    }
    {
        auto string = MUST(String::from_utf8("word    "sv));

        auto result = MUST(string.trim(" "sv, TrimMode::Both));
        EXPECT_EQ(result, "word"sv);

        result = MUST(string.trim(" "sv, TrimMode::Left));
        EXPECT_EQ(result, "word    "sv);

        result = MUST(string.trim(" "sv, TrimMode::Right));
        EXPECT_EQ(result, "word"sv);
    }
    {
        auto string = MUST(String::from_utf8("    word    "sv));

        auto result = MUST(string.trim(" "sv, TrimMode::Both));
        EXPECT_EQ(result, "word"sv);

        result = MUST(string.trim(" "sv, TrimMode::Left));
        EXPECT_EQ(result, "word    "sv);

        result = MUST(string.trim(" "sv, TrimMode::Right));
        EXPECT_EQ(result, "    word"sv);
    }
    {
        auto string = MUST(String::from_utf8("    word    "sv));

        auto result = MUST(string.trim("\t"sv, TrimMode::Both));
        EXPECT_EQ(result, "    word    "sv);

        result = MUST(string.trim("\t"sv, TrimMode::Left));
        EXPECT_EQ(result, "    word    "sv);

        result = MUST(string.trim("\t"sv, TrimMode::Right));
        EXPECT_EQ(result, "    word    "sv);
    }
    {
        auto string = MUST(String::from_utf8("ωΣωΣω"sv));

        auto result = MUST(string.trim("ω"sv, TrimMode::Both));
        EXPECT_EQ(result, "ΣωΣ"sv);

        result = MUST(string.trim("ω"sv, TrimMode::Left));
        EXPECT_EQ(result, "ΣωΣω"sv);

        result = MUST(string.trim("ω"sv, TrimMode::Right));
        EXPECT_EQ(result, "ωΣωΣ"sv);
    }
    {
        auto string = MUST(String::from_utf8("ωΣωΣω"sv));

        auto result = MUST(string.trim("ωΣ"sv, TrimMode::Both));
        EXPECT(result.is_empty());

        result = MUST(string.trim("ωΣ"sv, TrimMode::Left));
        EXPECT(result.is_empty());

        result = MUST(string.trim("ωΣ"sv, TrimMode::Right));
        EXPECT(result.is_empty());
    }
    {
        auto string = MUST(String::from_utf8("ωΣωΣω"sv));

        auto result = MUST(string.trim("Σω"sv, TrimMode::Both));
        EXPECT(result.is_empty());

        result = MUST(string.trim("Σω"sv, TrimMode::Left));
        EXPECT(result.is_empty());

        result = MUST(string.trim("Σω"sv, TrimMode::Right));
        EXPECT(result.is_empty());
    }
}
