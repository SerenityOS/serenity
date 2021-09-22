/*
 * Copyright (c) 2021, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <string.h>
#include <wchar.h>

TEST_CASE(wcspbrk)
{
    const wchar_t* input;
    wchar_t* ret;

    // Test empty haystack.
    ret = wcspbrk(L"", L"ab");
    EXPECT_EQ(ret, nullptr);

    // Test empty needle.
    ret = wcspbrk(L"ab", L"");
    EXPECT_EQ(ret, nullptr);

    // Test search for a single character.
    input = L"abcd";
    ret = wcspbrk(input, L"a");
    EXPECT_EQ(ret, input);

    // Test search for multiple characters, none matches.
    ret = wcspbrk(input, L"zxy");
    EXPECT_EQ(ret, nullptr);

    // Test search for multiple characters, last matches.
    ret = wcspbrk(input, L"zxyc");
    EXPECT_EQ(ret, input + 2);
}

TEST_CASE(wcsstr)
{
    const wchar_t* input = L"abcde";
    wchar_t* ret;

    // Empty needle should return haystack.
    ret = wcsstr(input, L"");
    EXPECT_EQ(ret, input);

    // Test exact match.
    ret = wcsstr(input, input);
    EXPECT_EQ(ret, input);

    // Test match at string start.
    ret = wcsstr(input, L"ab");
    EXPECT_EQ(ret, input);

    // Test match at string end.
    ret = wcsstr(input, L"de");
    EXPECT_EQ(ret, input + 3);

    // Test no match.
    ret = wcsstr(input, L"z");
    EXPECT_EQ(ret, nullptr);

    // Test needle that is longer than the haystack.
    ret = wcsstr(input, L"abcdef");
    EXPECT_EQ(ret, nullptr);
}

TEST_CASE(wmemchr)
{
    const wchar_t* input = L"abcde";
    wchar_t* ret;

    // Empty haystack returns nothing.
    ret = wmemchr(L"", L'c', 0);
    EXPECT_EQ(ret, nullptr);

    // Not included character returns nothing.
    ret = wmemchr(input, L'z', 5);
    EXPECT_EQ(ret, nullptr);

    // Match at string start.
    ret = wmemchr(input, L'a', 5);
    EXPECT_EQ(ret, input);

    // Match at string end.
    ret = wmemchr(input, L'e', 5);
    EXPECT_EQ(ret, input + 4);

    input = L"abcde\0fg";

    // Handle finding null characters.
    ret = wmemchr(input, L'\0', 8);
    EXPECT_EQ(ret, input + 5);

    // Don't stop at null characters.
    ret = wmemchr(input, L'f', 8);
    EXPECT_EQ(ret, input + 6);
}

TEST_CASE(wmemcpy)
{
    const wchar_t* input = L"abc\0def";
    auto buf = static_cast<wchar_t*>(malloc(8 * sizeof(wchar_t)));

    if (!buf) {
        FAIL("Could not allocate space for copy target");
        return;
    }

    wchar_t* ret = wmemcpy(buf, input, 8);

    EXPECT_EQ(ret, buf);
    EXPECT_EQ(memcmp(buf, input, 8 * sizeof(wchar_t)), 0);
}

TEST_CASE(wmemset)
{
    auto buf_length = 8;
    auto buf = static_cast<wchar_t*>(calloc(buf_length, sizeof(wchar_t)));

    if (!buf) {
        FAIL("Could not allocate memory for target buffer");
        return;
    }

    wchar_t* ret = wmemset(buf, L'\U0001f41e', buf_length - 1);

    EXPECT_EQ(ret, buf);

    for (int i = 0; i < buf_length - 1; i++) {
        EXPECT_EQ(buf[i], L'\U0001f41e');
    }

    EXPECT_EQ(buf[buf_length - 1], L'\0');

    free(buf);
}

TEST_CASE(wcscoll)
{
    // Check if wcscoll is sorting correctly. At the moment we are doing raw char comparisons,
    // so it's digits, then uppercase letters, then lowercase letters.

    // Equalness between equal strings.
    EXPECT(wcscoll(L"", L"") == 0);
    EXPECT(wcscoll(L"0", L"0") == 0);

    // Shorter strings before longer strings.
    EXPECT(wcscoll(L"", L"0") < 0);
    EXPECT(wcscoll(L"0", L"") > 0);
    EXPECT(wcscoll(L"123", L"1234") < 0);
    EXPECT(wcscoll(L"1234", L"123") > 0);

    // Order within digits.
    EXPECT(wcscoll(L"0", L"9") < 0);
    EXPECT(wcscoll(L"9", L"0") > 0);

    // Digits before uppercase letters.
    EXPECT(wcscoll(L"9", L"A") < 0);
    EXPECT(wcscoll(L"A", L"9") > 0);

    // Order within uppercase letters.
    EXPECT(wcscoll(L"A", L"Z") < 0);
    EXPECT(wcscoll(L"Z", L"A") > 0);

    // Uppercase letters before lowercase letters.
    EXPECT(wcscoll(L"Z", L"a") < 0);
    EXPECT(wcscoll(L"a", L"Z") > 0);

    // Uppercase letters before lowercase letters.
    EXPECT(wcscoll(L"a", L"z") < 0);
    EXPECT(wcscoll(L"z", L"a") > 0);
}
