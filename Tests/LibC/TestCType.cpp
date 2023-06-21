/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <ctype.h>

// https://open-std.org/JTC1/SC22/WG14/www/docs/n2912.pdf
// Section 7.4.1 Character classification functions

// 7.4.1.1 The isalnum function
// The isalnum function tests for any character for which isalpha or isdigit is true.
TEST_CASE(test_isalnum)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= '0' && i <= '9') || (i >= 'A' && i <= 'Z') || (i >= 'a' && i <= 'z'))
            EXPECT_NE(isalnum(i), 0);
        else
            EXPECT_EQ(isalnum(i), 0);
    }
}

// 7.4.1.2 The isalpha function
// The isalpha function tests for any character for which isupper or islower is true, or any character
// that is one of a locale-specific set of alphabetic characters for which none of iscntrl, isdigit,
// ispunct, or isspace is true. In the "C" locale, isalpha returns true only for the characters for
// which isupper or islower is true.
TEST_CASE(test_isalpha)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= 'A' && i <= 'Z') || (i >= 'a' && i <= 'z'))
            EXPECT_NE(isalpha(i), 0);
        else
            EXPECT_EQ(isalpha(i), 0);
    }
}

// 7.4.1.3 The isblank function
// The isblank function tests for any character that is a standard blank character or is one of a locale-
// specific set of characters for which isspace is true and that is used to separate words within a line
// of text. The standard blank characters are the following: space (’ ’ ), and horizontal tab (’\t’ ). In
// the "C" locale, isblank returns true only for the standard blank characters.
TEST_CASE(test_isblank)
{
    for (int i = 0; i < 256; ++i) {
        if ((i == ' ') || (i == '\t'))
            EXPECT_NE(isblank(i), 0);
        else
            EXPECT_EQ(isblank(i), 0);
    }
}

// 7.4.1.4 The iscntrl function
// The iscntrl function tests for any control character
TEST_CASE(test_iscntrl)
{
    for (int i = 0; i < 256; ++i) {
        if ((i < ' ') || (i == '\x7F')) // 7F is DEL
            EXPECT_NE(iscntrl(i), 0);
        else
            EXPECT_EQ(iscntrl(i), 0);
    }
}

// 7.4.1.5 The isdigit function
// The isdigit function tests for any decimal-digit character (as defined in 5.2.1)
TEST_CASE(test_isdigit)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= '0' && i <= '9'))
            EXPECT_NE(isdigit(i), 0);
        else
            EXPECT_EQ(isdigit(i), 0);
    }
}

// 7.4.1.6 The isgraph function
// The isgraph function tests for any printing character except space (’ ’).
TEST_CASE(test_isgraph)
{
    for (int i = 0; i < 256; ++i) {
        if ((i > ' ' && i <= '~'))
            EXPECT_NE(isgraph(i), 0);
        else
            EXPECT_EQ(isgraph(i), 0);
    }
}

// 7.4.1.7 The islower function
// The islower function tests for any character that is a lowercase letter or is one of a locale-specific set
// of characters for which none of iscntrl, isdigit, ispunct, or isspace is true. In the "C" locale,
// islower returns true only for the lowercase letters (as defined in 5.2.1
TEST_CASE(test_islower)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= 'a' && i <= 'z'))
            EXPECT_NE(islower(i), 0);
        else
            EXPECT_EQ(islower(i), 0);
    }
}

// 7.4.1.8 The isprint function
// The isprint function tests for any printing character including space (’ ’).
TEST_CASE(test_isprint)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= ' ' && i <= '~'))
            EXPECT_NE(isprint(i), 0);
        else
            EXPECT_EQ(isprint(i), 0);
    }
}

// 7.4.1.9 The ispunct function
// The ispunct function tests for any printing character that is one of a locale-specific set of punctuation
// characters for which neither isspace nor isalnum is true. In the "C" locale, ispunct returns true
// for every printing character for which neither isspace nor isalnum is true
TEST_CASE(test_ispunct)
{
    for (int i = 0; i < 256; ++i) {
        if ((i > ' ' && i < '0') || (i > '9' && i < 'A') || (i > 'Z' && i < 'a') || (i > 'z' && i < '\x7F'))
            EXPECT_NE(ispunct(i), 0);
        else
            EXPECT_EQ(ispunct(i), 0);
    }
}

// 7.4.1.10 The isspace function
// The isspace function tests for any character that is a standard white-space character or is one of
// a locale-specific set of characters for which isalnum is false. The standard white-space characters
// are the following: space (’ ’ ), form feed (’\f’ ), new-line (’\n’ ), carriage return (’\r’ ), horizontal
// tab (’\t’ ), and vertical tab (’\v’ ). In the "C" locale, isspace returns true only for the standard
// white-space characters.
TEST_CASE(test_isspace)
{
    for (int i = 0; i < 256; ++i) {
        if ((i == ' ') || (i == '\f') || (i == '\n') || (i == '\r') || (i == '\t') || (i == '\v'))
            EXPECT_NE(isspace(i), 0);
        else
            EXPECT_EQ(isspace(i), 0);
    }
}

// 7.4.1.11 The isupper function
// The isupper function tests for any character that is an uppercase letter or is one of a locale-specific
// set of characters for which none of iscntrl, isdigit, ispunct, or isspace is true. In the "C" locale,
// isupper returns true only for the uppercase letters (as defined in 5.2.1)
TEST_CASE(test_isupper)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= 'A' && i <= 'Z'))
            EXPECT_NE(isupper(i), 0);
        else
            EXPECT_EQ(isupper(i), 0);
    }
}

// 7.4.1.12 The isxdigit function
// The isxdigit function tests for any hexadecimal-digit character (as defined in 6.4.4.1).
TEST_CASE(test_isxdigit)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= '0' && i <= '9') || (i >= 'A' && i <= 'F') || (i >= 'a' && i <= 'f'))
            EXPECT_NE(isxdigit(i), 0);
        else
            EXPECT_EQ(isxdigit(i), 0);
    }
}

// 7.4.2.1 The tolower function
// The tolower function converts an uppercase letter to a corresponding lowercase letter
TEST_CASE(test_tolower)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= 'A' && i <= 'Z'))
            EXPECT_EQ(tolower(i), i + 0x20);
        else
            EXPECT_EQ(tolower(i), i);
    }
}

// 7.4.2.2 The toupper function
// The toupper function converts an lowercase letter to a corresponding uppercase letter
TEST_CASE(test_toupper)
{
    for (int i = 0; i < 256; ++i) {
        if ((i >= 'a' && i <= 'z'))
            EXPECT_EQ(toupper(i), i - 0x20);
        else
            EXPECT_EQ(toupper(i), i);
    }
}
