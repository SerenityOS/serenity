/*
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/FuzzyMatch.h>

TEST_CASE(is_leading_letter_penalty_correctly_applied)
{
    // Leading penalty is -5 points for each initial unmatched letter up to a maximum of -15.
    EXPECT_EQ(fuzzy_match("b"sv, "ab"sv).score, 94);
    EXPECT_EQ(fuzzy_match("c"sv, "abc"sv).score, 88);
    EXPECT_EQ(fuzzy_match("d"sv, "abcd"sv).score, 82);
    EXPECT_EQ(fuzzy_match("e"sv, "abcde"sv).score, 81);
}

TEST_CASE(is_first_letter_bonus_applied_correctly)
{
    // First letter bonus is +15 if the first letter matches.
    EXPECT_EQ(fuzzy_match("a"sv, "ab"sv).score, 114);
    EXPECT_EQ(fuzzy_match("a"sv, "Ab"sv).score, 114);
    EXPECT_EQ(fuzzy_match(" "sv, " b"sv).score, 114);
}

TEST_CASE(is_sequential_bonus_applied_correctly)
{
    // Sequential bonus is +15 for each sequential match.
    EXPECT_EQ(fuzzy_match("bc"sv, "abc"sv).score, 109);
    EXPECT_EQ(fuzzy_match("bcd"sv, "ab-cd"sv).score, 108);
    EXPECT_EQ(fuzzy_match("bcd"sv, "abcd"sv).score, 124);
    EXPECT_EQ(fuzzy_match("bcde"sv, "ab-cde"sv).score, 123);
    EXPECT_EQ(fuzzy_match("bcde"sv, "abcde"sv).score, 139);
    EXPECT_EQ(fuzzy_match("bcde"sv, "abcdef"sv).score, 138);
}

TEST_CASE(is_camel_case_bonus_applied_correctly)
{
    // Camel case bonus is +30 if the matching character is uppercase and the preceding character is lowercase.
    // These cases get no camel case bonus.
    EXPECT_EQ(fuzzy_match("b"sv, "Ab"sv).score, 94);
    EXPECT_EQ(fuzzy_match("abc"sv, "ABcd"sv).score, 144);
    EXPECT_EQ(fuzzy_match("abc"sv, "ABCd"sv).score, 144);
    EXPECT_EQ(fuzzy_match("abc"sv, "Abcd"sv).score, 144);
    EXPECT_EQ(fuzzy_match("abcd"sv, "abcde"sv).score, 159);

    // These cases get a camel case bonus.
    EXPECT_EQ(fuzzy_match("b"sv, "aB"sv).score, 124);
    EXPECT_EQ(fuzzy_match("abc"sv, "aBcd"sv).score, 174);
    EXPECT_EQ(fuzzy_match("abc"sv, "aBC-"sv).score, 174);
    EXPECT_EQ(fuzzy_match("abcd"sv, "aBcD-"sv).score, 219);
}

TEST_CASE(is_separator_bonus_applied_correctly)
{
    // Separator bonus is +30 if the character preceding the matching character is a space or an underscore.
    EXPECT_EQ(fuzzy_match("b"sv, "a b"sv).score, 118);
    EXPECT_EQ(fuzzy_match("bc"sv, "a b c"sv).score, 147);
    EXPECT_EQ(fuzzy_match("abcd"sv, "a b c d"sv).score, 202);
    EXPECT_EQ(fuzzy_match("abcd"sv, "a_b_c_d"sv).score, 202);
    EXPECT_EQ(fuzzy_match("b c"sv, "ab cd"sv).score, 153);
    EXPECT_EQ(fuzzy_match("b_c"sv, "ab_cd"sv).score, 153);
    EXPECT_EQ(fuzzy_match("bc"sv, "ab cd"sv).score, 122);
}

TEST_CASE(equality)
{
    EXPECT(fuzzy_match("abc"sv, "abc"sv).score > fuzzy_match("abc"sv, "a b c"sv).score);
}
