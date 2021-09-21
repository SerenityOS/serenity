/*
 * Copyright (c) 2021, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <errno.h>
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

TEST_CASE(wmemmove)
{
    wchar_t* ret;
    const wchar_t* string = L"abc\0def";
    auto buf = static_cast<wchar_t*>(calloc(32, sizeof(wchar_t)));

    if (!buf) {
        FAIL("Could not allocate memory for target buffer");
        return;
    }

    // Test moving to smaller addresses.
    wmemcpy(buf + 3, string, 8);
    ret = wmemmove(buf + 1, buf + 3, 8);
    EXPECT_EQ(ret, buf + 1);
    EXPECT_EQ(memcmp(string, buf + 1, 8 * sizeof(wchar_t)), 0);

    // Test moving to larger addresses.
    wmemcpy(buf + 16, string, 8);
    ret = wmemmove(buf + 18, buf + 16, 8);
    EXPECT_EQ(ret, buf + 18);
    EXPECT_EQ(memcmp(string, buf + 18, 8 * sizeof(wchar_t)), 0);

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

TEST_CASE(mbsinit)
{
    // Ensure that nullptr is considered an initial state.
    EXPECT(mbsinit(nullptr) != 0);

    // Ensure that a zero-initialized state is recognized as initial state.
    mbstate_t state = {};
    EXPECT(mbsinit(&state) != 0);

    // Read a partial multibyte sequence (0b11011111 / 0xdf).
    size_t ret = mbrtowc(nullptr, "\xdf", 1, &state);

    if (ret != -2ul)
        FAIL(String::formatted("mbrtowc accepted partial multibyte sequence with return code {} (expected -2)", static_cast<ssize_t>(ret)));

    // Ensure that we are not in an initial state.
    EXPECT(mbsinit(&state) == 0);

    // Read the remaining multibyte sequence (0b10111111 / 0xbf).
    ret = mbrtowc(nullptr, "\xbf", 1, &state);

    if (ret != 1ul)
        FAIL(String::formatted("mbrtowc did not consume the expected number of bytes (1), returned {} instead", static_cast<ssize_t>(ret)));

    // Ensure that we are in an initial state again.
    EXPECT(mbsinit(&state) != 0);
}

TEST_CASE(mbrtowc)
{
    size_t ret = 0;
    mbstate_t state = {};
    wchar_t wc = 0;

    // Ensure that we can parse normal ASCII characters.
    ret = mbrtowc(&wc, "Hello", 5, &state);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(wc, 'H');

    // Try two three-byte codepoints (™™), only one of which should be consumed.
    ret = mbrtowc(&wc, "\xe2\x84\xa2\xe2\x84\xa2", 6, &state);
    EXPECT_EQ(ret, 3ul);
    EXPECT_EQ(wc, 0x2122);

    // Try a null character, which should return 0 and reset the state to the initial state.
    ret = mbrtowc(&wc, "\x00\x00", 2, &state);
    EXPECT_EQ(ret, 0ul);
    EXPECT_EQ(wc, 0);
    EXPECT_NE(mbsinit(&state), 0);

    // Try an incomplete multibyte character.
    ret = mbrtowc(&wc, "\xe2\x84", 2, &state);
    EXPECT_EQ(ret, -2ul);
    EXPECT_EQ(mbsinit(&state), 0);

    mbstate_t incomplete_state = state;

    // Finish the previous multibyte character.
    ret = mbrtowc(&wc, "\xa2", 1, &state);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(wc, 0x2122);

    // Try an invalid multibyte sequence.
    // Reset the state afterwards because the effects are undefined.
    ret = mbrtowc(&wc, "\xff", 1, &state);
    EXPECT_EQ(ret, -1ul);
    EXPECT_EQ(errno, EILSEQ);
    state = {};

    // Try a successful conversion, but without target address.
    ret = mbrtowc(nullptr, "\xe2\x84\xa2\xe2\x84\xa2", 6, &state);
    EXPECT_EQ(ret, 3ul);

    // Test the "null byte shorthand". Ensure that wc is ignored.
    state = {};
    wchar_t old_wc = wc;
    ret = mbrtowc(&wc, nullptr, 0, &state);
    EXPECT_EQ(ret, 0ul);
    EXPECT_EQ(wc, old_wc);

    // Test recognition of incomplete multibyte sequences.
    ret = mbrtowc(nullptr, nullptr, 0, &incomplete_state);
    EXPECT_EQ(ret, -1ul);
    EXPECT_EQ(errno, EILSEQ);
}
