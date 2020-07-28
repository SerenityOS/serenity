/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
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
    u32 replacements = test_string.replace("Friends", "Testers");
    EXPECT(replacements == 1);
    EXPECT(test_string == "Well, hello Testers!");

    replacements = test_string.replace("ell", "e're", true);
    EXPECT(replacements == 2);
    EXPECT(test_string == "We're, he'reo Testers!");

    replacements = test_string.replace("!", " :^)");
    EXPECT(replacements == 1);
    EXPECT(test_string == "We're, he'reo Testers :^)");

    test_string = String("111._.111._.111");
    replacements = test_string.replace("111", "|||", true);
    EXPECT(replacements == 3);
    EXPECT(test_string == "|||._.|||._.|||");

    replacements = test_string.replace("|||", "111");
    EXPECT(replacements == 1);
    EXPECT(test_string == "111._.|||._.|||");
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

TEST_MAIN(String)
