/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <cstring>

TEST_CASE(construct_empty)
{
    EXPECT(String().is_null());
    EXPECT(String().is_empty());
    EXPECT(!String().characters());

    EXPECT(!String("").is_null());
    EXPECT(String("").is_empty());
    EXPECT(String("").characters() != nullptr);

    EXPECT(String("").impl() == String::empty().impl());
}

TEST_CASE(construct_contents)
{
    String test_string = "ABCDEF";
    EXPECT(!test_string.is_empty());
    EXPECT(!test_string.is_null());
    EXPECT_EQ(test_string.length(), 6u);
    EXPECT_EQ(test_string.length(), strlen(test_string.characters()));
    EXPECT(test_string.characters() != nullptr);
    EXPECT(!strcmp(test_string.characters(), "ABCDEF"));

    EXPECT(test_string == "ABCDEF");
    EXPECT(test_string != "ABCDE");
    EXPECT(test_string != "ABCDEFG");
}

TEST_CASE(compare)
{
    String test_string = "ABCDEF";
    EXPECT("a" < String("b"));
    EXPECT(!("a" > String("b")));
    EXPECT("b" > String("a"));
    EXPECT(!("b" < String("b")));
    EXPECT("a" >= String("a"));
    EXPECT(!("a" >= String("b")));
    EXPECT("a" <= String("a"));
    EXPECT(!("b" <= String("a")));
}

TEST_CASE(index_access)
{
    String test_string = "ABCDEF";
    EXPECT_EQ(test_string[0], 'A');
    EXPECT_EQ(test_string[1], 'B');
}

TEST_CASE(starts_with)
{
    String test_string = "ABCDEF";
    EXPECT(test_string.starts_with("AB"));
    EXPECT(test_string.starts_with('A'));
    EXPECT(!test_string.starts_with('B'));
    EXPECT(test_string.starts_with("ABCDEF"));
    EXPECT(!test_string.starts_with("DEF"));
    EXPECT(test_string.starts_with("abc", CaseSensitivity::CaseInsensitive));
    EXPECT(!test_string.starts_with("abc", CaseSensitivity::CaseSensitive));
}

TEST_CASE(ends_with)
{
    String test_string = "ABCDEF";
    EXPECT(test_string.ends_with("EF"));
    EXPECT(test_string.ends_with('F'));
    EXPECT(!test_string.ends_with('E'));
    EXPECT(test_string.ends_with("ABCDEF"));
    EXPECT(!test_string.ends_with("ABC"));
    EXPECT(test_string.ends_with("def", CaseSensitivity::CaseInsensitive));
    EXPECT(!test_string.ends_with("def", CaseSensitivity::CaseSensitive));
}

TEST_CASE(copy_string)
{
    String test_string = "ABCDEF";
    auto test_string_copy = test_string;
    EXPECT_EQ(test_string, test_string_copy);
    EXPECT_EQ(test_string.characters(), test_string_copy.characters());
}

TEST_CASE(move_string)
{
    String test_string = "ABCDEF";
    auto test_string_copy = test_string;
    auto test_string_move = move(test_string_copy);
    EXPECT_EQ(test_string, test_string_move);
    EXPECT(test_string_copy.is_null());
}

TEST_CASE(repeated)
{
    EXPECT_EQ(String::repeated('x', 0), "");
    EXPECT_EQ(String::repeated('x', 1), "x");
    EXPECT_EQ(String::repeated('x', 2), "xx");
}

TEST_CASE(to_int)
{
    EXPECT_EQ(String("123").to_int().value(), 123);
    EXPECT_EQ(String("-123").to_int().value(), -123);
}

TEST_CASE(to_lowercase)
{
    EXPECT(String("ABC").to_lowercase() == "abc");
}

TEST_CASE(to_uppercase)
{
    EXPECT(String("AbC").to_uppercase() == "ABC");
}

TEST_CASE(flystring)
{
    {
        FlyString a("foo");
        FlyString b("foo");
        EXPECT_EQ(a.impl(), b.impl());
    }

    {
        String a = "foo";
        FlyString b = a;
        StringBuilder builder;
        builder.append('f');
        builder.append("oo");
        FlyString c = builder.to_string();
        EXPECT_EQ(a.impl(), b.impl());
        EXPECT_EQ(a.impl(), c.impl());
    }
}

TEST_CASE(replace)
{
    String test_string = "Well, hello Friends!";

    test_string = test_string.replace("Friends", "Testers");
    EXPECT(test_string == "Well, hello Testers!");

    test_string = test_string.replace("ell", "e're", true);
    EXPECT(test_string == "We're, he'reo Testers!");

    test_string = test_string.replace("!", " :^)");
    EXPECT(test_string == "We're, he'reo Testers :^)");

    test_string = String("111._.111._.111");
    test_string = test_string.replace("111", "|||", true);
    EXPECT(test_string == "|||._.|||._.|||");

    test_string = test_string.replace("|||", "111");
    EXPECT(test_string == "111._.|||._.|||");
}

TEST_CASE(count)
{
    String test_string = "Well, hello Friends!";
    u32 count = test_string.count("Friends");
    EXPECT(count == 1);

    count = test_string.count("ell");
    EXPECT(count == 2);

    count = test_string.count("!");
    EXPECT(count == 1);

    test_string = String("111._.111._.111");
    count = test_string.count("111");
    EXPECT(count == 3);

    count = test_string.count("._.");
    EXPECT(count == 2);
}

TEST_CASE(substring)
{
    String test = "abcdef";
    EXPECT_EQ(test.substring(0, 6), test);
    EXPECT_EQ(test.substring(0, 3), "abc");
    EXPECT_EQ(test.substring(3, 3), "def");
    EXPECT_EQ(test.substring(3, 0), "");
    EXPECT_EQ(test.substring(6, 0), "");
}

TEST_CASE(split)
{
    String test = "foo bar baz";
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

    parts = test.split(' ', true);
    EXPECT_EQ(parts.size(), 5u);
    EXPECT_EQ(parts[0], "a");
    EXPECT_EQ(parts[1], "");
    EXPECT_EQ(parts[2], "");
    EXPECT_EQ(parts[3], "");
    EXPECT_EQ(parts[4], "b");

    test = "axxbx";
    EXPECT_EQ(test.split('x').size(), 2u);
    EXPECT_EQ(test.split('x', true).size(), 4u);
    EXPECT_EQ(test.split_view('x').size(), 2u);
    EXPECT_EQ(test.split_view('x', true).size(), 4u);
}

TEST_CASE(builder_zero_initial_capacity)
{
    StringBuilder builder(0);
    builder.append("");
    auto built = builder.build();
    EXPECT_EQ(built.is_null(), false);
    EXPECT_EQ(built.length(), 0u);
}

TEST_CASE(sprintf)
{
    char buf1[128];
    int ret1 = sprintf(buf1, "%+d", 12);
    EXPECT_EQ(ret1, 3);

    char buf2[128];
    int ret2 = sprintf(buf2, "%+d", -12);
    EXPECT_EQ(ret2, 3);

    EXPECT_EQ(String(buf1), String("+12"));
    EXPECT_EQ(String(buf2), String("-12"));
}

TEST_CASE(find)
{
    String a = "foobarbar";
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
    String string = "";
    EXPECT_EQ(string.find(""sv), 0u);
    EXPECT_EQ(string.find_all(""sv), (Vector<size_t> { 0u }));

    string = "abc";
    EXPECT_EQ(string.find(""sv), 0u);
    EXPECT_EQ(string.find_all(""sv), (Vector<size_t> { 0u, 1u, 2u, 3u }));
}

TEST_CASE(bijective_base)
{
    EXPECT_EQ(String::bijective_base_from(0), "A");
    EXPECT_EQ(String::bijective_base_from(25), "Z");
    EXPECT_EQ(String::bijective_base_from(26), "AA");
    EXPECT_EQ(String::bijective_base_from(52), "BA");
    EXPECT_EQ(String::bijective_base_from(704), "ABC");
}

TEST_CASE(roman_numerals)
{
    auto zero = String::roman_number_from(0);
    EXPECT_EQ(zero, "");

    auto one = String::roman_number_from(1);
    EXPECT_EQ(one, "I");

    auto nine = String::roman_number_from(9);
    EXPECT_EQ(nine, "IX");

    auto fourty_eight = String::roman_number_from(48);
    EXPECT_EQ(fourty_eight, "XLVIII");

    auto one_thousand_nine_hundred_ninety_eight = String::roman_number_from(1998);
    EXPECT_EQ(one_thousand_nine_hundred_ninety_eight, "MCMXCVIII");

    auto four_thousand = String::roman_number_from(4000);
    EXPECT_EQ(four_thousand, "4000");
}
