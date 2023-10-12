/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <cstring>

TEST_CASE(construct_empty)
{
    EXPECT(DeprecatedString().is_empty());
    EXPECT(DeprecatedString().characters() != nullptr);

    EXPECT(DeprecatedString("").is_empty());
    EXPECT(DeprecatedString("").characters() != nullptr);

    EXPECT(DeprecatedString("").impl() == DeprecatedString::empty().impl());
}

TEST_CASE(construct_contents)
{
    DeprecatedString test_string = "ABCDEF";
    EXPECT(!test_string.is_empty());
    EXPECT_EQ(test_string.length(), 6u);
    EXPECT_EQ(test_string.length(), strlen(test_string.characters()));
    EXPECT(test_string.characters() != nullptr);
    EXPECT(!strcmp(test_string.characters(), "ABCDEF"));

    EXPECT(test_string == "ABCDEF");
    EXPECT(test_string != "ABCDE");
    EXPECT(test_string != "ABCDEFG");
}

TEST_CASE(equal)
{
    EXPECT_EQ(DeprecatedString::empty(), DeprecatedString {});
}

TEST_CASE(compare)
{
    EXPECT("a"sv < DeprecatedString("b"));
    EXPECT(!("a"sv > DeprecatedString("b")));
    EXPECT("b"sv > DeprecatedString("a"));
    EXPECT(!("b"sv < DeprecatedString("b")));
    EXPECT("a"sv >= DeprecatedString("a"));
    EXPECT(!("a"sv >= DeprecatedString("b")));
    EXPECT("a"sv <= DeprecatedString("a"));
    EXPECT(!("b"sv <= DeprecatedString("a")));

    EXPECT(DeprecatedString("a") > DeprecatedString());
    EXPECT(!(DeprecatedString() > DeprecatedString("a")));
    EXPECT(DeprecatedString() < DeprecatedString("a"));
    EXPECT(!(DeprecatedString("a") < DeprecatedString()));
    EXPECT(DeprecatedString("a") >= DeprecatedString());
    EXPECT(!(DeprecatedString() >= DeprecatedString("a")));
    EXPECT(DeprecatedString() <= DeprecatedString("a"));
    EXPECT(!(DeprecatedString("a") <= DeprecatedString()));

    EXPECT(!(DeprecatedString() > DeprecatedString()));
    EXPECT(!(DeprecatedString() < DeprecatedString()));
    EXPECT(DeprecatedString() >= DeprecatedString());
    EXPECT(DeprecatedString() <= DeprecatedString());
}

TEST_CASE(index_access)
{
    DeprecatedString test_string = "ABCDEF";
    EXPECT_EQ(test_string[0], 'A');
    EXPECT_EQ(test_string[1], 'B');
}

TEST_CASE(starts_with)
{
    DeprecatedString test_string = "ABCDEF";
    EXPECT(test_string.starts_with("AB"sv));
    EXPECT(test_string.starts_with('A'));
    EXPECT(!test_string.starts_with('B'));
    EXPECT(test_string.starts_with("ABCDEF"sv));
    EXPECT(!test_string.starts_with("DEF"sv));
    EXPECT(test_string.starts_with("abc"sv, CaseSensitivity::CaseInsensitive));
    EXPECT(!test_string.starts_with("abc"sv, CaseSensitivity::CaseSensitive));
}

TEST_CASE(ends_with)
{
    DeprecatedString test_string = "ABCDEF";
    EXPECT(test_string.ends_with("EF"sv));
    EXPECT(test_string.ends_with('F'));
    EXPECT(!test_string.ends_with('E'));
    EXPECT(test_string.ends_with("ABCDEF"sv));
    EXPECT(!test_string.ends_with("ABC"sv));
    EXPECT(test_string.ends_with("def"sv, CaseSensitivity::CaseInsensitive));
    EXPECT(!test_string.ends_with("def"sv, CaseSensitivity::CaseSensitive));
}

TEST_CASE(copy_string)
{
    DeprecatedString test_string = "ABCDEF";
    auto test_string_copy = test_string;
    EXPECT_EQ(test_string, test_string_copy);
    EXPECT_EQ(test_string.characters(), test_string_copy.characters());
}

TEST_CASE(move_string)
{
    DeprecatedString test_string = "ABCDEF";
    auto test_string_copy = test_string;
    auto test_string_move = move(test_string_copy);
    EXPECT_EQ(test_string, test_string_move);
    EXPECT(test_string_copy.is_empty());
}

TEST_CASE(repeated)
{
    EXPECT_EQ(DeprecatedString::repeated('x', 0), "");
    EXPECT_EQ(DeprecatedString::repeated('x', 1), "x");
    EXPECT_EQ(DeprecatedString::repeated('x', 2), "xx");
}

TEST_CASE(to_int)
{
    EXPECT_EQ(DeprecatedString("123").to_int().value(), 123);
    EXPECT_EQ(DeprecatedString("-123").to_int().value(), -123);
}

TEST_CASE(to_lowercase)
{
    EXPECT(DeprecatedString("ABC").to_lowercase() == "abc");
}

TEST_CASE(to_uppercase)
{
    EXPECT(DeprecatedString("AbC").to_uppercase() == "ABC");
}

TEST_CASE(flystring)
{
    {
        DeprecatedFlyString a("foo");
        DeprecatedFlyString b("foo");
        EXPECT_EQ(a.impl(), b.impl());
    }

    {
        DeprecatedString a = "foo";
        DeprecatedFlyString b = a;
        StringBuilder builder;
        builder.append('f');
        builder.append("oo"sv);
        DeprecatedFlyString c = builder.to_deprecated_string();
        EXPECT_EQ(a.impl(), b.impl());
        EXPECT_EQ(a.impl(), c.impl());
    }
}

TEST_CASE(replace)
{
    DeprecatedString test_string = "Well, hello Friends!";

    test_string = test_string.replace("Friends"sv, "Testers"sv, ReplaceMode::FirstOnly);
    EXPECT(test_string == "Well, hello Testers!");

    test_string = test_string.replace("ell"sv, "e're"sv, ReplaceMode::All);
    EXPECT(test_string == "We're, he'reo Testers!");

    test_string = test_string.replace("!"sv, " :^)"sv, ReplaceMode::FirstOnly);
    EXPECT(test_string == "We're, he'reo Testers :^)");

    test_string = DeprecatedString("111._.111._.111");
    test_string = test_string.replace("111"sv, "|||"sv, ReplaceMode::All);
    EXPECT(test_string == "|||._.|||._.|||");

    test_string = test_string.replace("|||"sv, "111"sv, ReplaceMode::FirstOnly);
    EXPECT(test_string == "111._.|||._.|||");
}

TEST_CASE(count)
{
    DeprecatedString test_string = "Well, hello Friends!";
    u32 count = test_string.count("Friends"sv);
    EXPECT(count == 1);

    count = test_string.count("ell"sv);
    EXPECT(count == 2);

    count = test_string.count("!"sv);
    EXPECT(count == 1);

    test_string = DeprecatedString("111._.111._.111");
    count = test_string.count("111"sv);
    EXPECT(count == 3);

    count = test_string.count("._."sv);
    EXPECT(count == 2);
}

TEST_CASE(substring)
{
    DeprecatedString test = "abcdef";
    EXPECT_EQ(test.substring(0, 6), test);
    EXPECT_EQ(test.substring(0, 3), "abc");
    EXPECT_EQ(test.substring(3, 3), "def");
    EXPECT_EQ(test.substring(3, 0), "");
    EXPECT_EQ(test.substring(6, 0), "");
}

TEST_CASE(split)
{
    DeprecatedString test = "foo bar baz";
    auto parts = test.split(' ');
    EXPECT_EQ(parts.size(), 3u);
    EXPECT_EQ(parts[0], "foo");
    EXPECT_EQ(parts[1], "bar");
    EXPECT_EQ(parts[2], "baz");

    EXPECT_EQ(parts[0].characters()[3], '\0');
    EXPECT_EQ(parts[1].characters()[3], '\0');
    EXPECT_EQ(parts[2].characters()[3], '\0');

    test = "a    b";

    parts = test.split(' ');
    EXPECT_EQ(parts.size(), 2u);
    EXPECT_EQ(parts[0], "a");
    EXPECT_EQ(parts[1], "b");

    parts = test.split(' ', SplitBehavior::KeepEmpty);
    EXPECT_EQ(parts.size(), 5u);
    EXPECT_EQ(parts[0], "a");
    EXPECT_EQ(parts[1], "");
    EXPECT_EQ(parts[2], "");
    EXPECT_EQ(parts[3], "");
    EXPECT_EQ(parts[4], "b");

    test = "axxbx";
    EXPECT_EQ(test.split('x').size(), 2u);
    EXPECT_EQ(test.split('x', SplitBehavior::KeepEmpty).size(), 4u);
    EXPECT_EQ(test.split_view('x').size(), 2u);
    EXPECT_EQ(test.split_view('x', SplitBehavior::KeepEmpty).size(), 4u);
}

TEST_CASE(builder_zero_initial_capacity)
{
    StringBuilder builder(0);
    builder.append(""sv);
    auto built = builder.to_deprecated_string();
    EXPECT_EQ(built.length(), 0u);
}

TEST_CASE(find)
{
    DeprecatedString a = "foobarbar";
    EXPECT_EQ(a.find("bar"sv), Optional<size_t> { 3 });
    EXPECT_EQ(a.find("baz"sv), Optional<size_t> {});
    EXPECT_EQ(a.find("bar"sv, 4), Optional<size_t> { 6 });
    EXPECT_EQ(a.find("bar"sv, 9), Optional<size_t> {});

    EXPECT_EQ(a.find('f'), Optional<size_t> { 0 });
    EXPECT_EQ(a.find('x'), Optional<size_t> {});
    EXPECT_EQ(a.find('f', 1), Optional<size_t> {});
    EXPECT_EQ(a.find('b'), Optional<size_t> { 3 });
    EXPECT_EQ(a.find('b', 4), Optional<size_t> { 6 });
    EXPECT_EQ(a.find('b', 9), Optional<size_t> {});
}

TEST_CASE(find_with_empty_needle)
{
    DeprecatedString string = "";
    EXPECT_EQ(string.find(""sv), 0u);
    EXPECT_EQ(string.find_all(""sv), (Vector<size_t> { 0u }));

    string = "abc";
    EXPECT_EQ(string.find(""sv), 0u);
    EXPECT_EQ(string.find_all(""sv), (Vector<size_t> { 0u, 1u, 2u, 3u }));
}

TEST_CASE(bijective_base)
{
    EXPECT_EQ(DeprecatedString::bijective_base_from(0), "A");
    EXPECT_EQ(DeprecatedString::bijective_base_from(25), "Z");
    EXPECT_EQ(DeprecatedString::bijective_base_from(26), "AA");
    EXPECT_EQ(DeprecatedString::bijective_base_from(52), "BA");
    EXPECT_EQ(DeprecatedString::bijective_base_from(701), "ZZ");
    EXPECT_EQ(DeprecatedString::bijective_base_from(702), "AAA");
    EXPECT_EQ(DeprecatedString::bijective_base_from(730), "ABC");
    EXPECT_EQ(DeprecatedString::bijective_base_from(18277), "ZZZ");
}

TEST_CASE(roman_numerals)
{
    auto zero = DeprecatedString::roman_number_from(0);
    EXPECT_EQ(zero, "");

    auto one = DeprecatedString::roman_number_from(1);
    EXPECT_EQ(one, "I");

    auto nine = DeprecatedString::roman_number_from(9);
    EXPECT_EQ(nine, "IX");

    auto fourty_eight = DeprecatedString::roman_number_from(48);
    EXPECT_EQ(fourty_eight, "XLVIII");

    auto one_thousand_nine_hundred_ninety_eight = DeprecatedString::roman_number_from(1998);
    EXPECT_EQ(one_thousand_nine_hundred_ninety_eight, "MCMXCVIII");

    auto four_thousand = DeprecatedString::roman_number_from(4000);
    EXPECT_EQ(four_thousand, "4000");
}
