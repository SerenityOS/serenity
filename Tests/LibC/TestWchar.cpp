/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <errno.h>
#include <limits.h>
#include <string.h>
#include <time.h>
#include <wchar.h>

TEST_CASE(wcspbrk)
{
    wchar_t const* input;
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
    wchar_t const* input = L"abcde";
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
    wchar_t const* input = L"abcde";
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
    wchar_t const* input = L"abc\0def";
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
    wchar_t const* string = L"abc\0def";
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
        FAIL(ByteString::formatted("mbrtowc accepted partial multibyte sequence with return code {} (expected -2)", static_cast<ssize_t>(ret)));

    // Ensure that we are not in an initial state.
    EXPECT(mbsinit(&state) == 0);

    // Read the remaining multibyte sequence (0b10111111 / 0xbf).
    ret = mbrtowc(nullptr, "\xbf", 1, &state);

    if (ret != 1ul)
        FAIL(ByteString::formatted("mbrtowc did not consume the expected number of bytes (1), returned {} instead", static_cast<ssize_t>(ret)));

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
    EXPECT_EQ(wc, static_cast<wchar_t>('H'));

    // Try two three-byte codepoints (™™), only one of which should be consumed.
    ret = mbrtowc(&wc, "\xe2\x84\xa2\xe2\x84\xa2", 6, &state);
    EXPECT_EQ(ret, 3ul);
    EXPECT_EQ(wc, static_cast<wchar_t>(0x2122));

    // Try a null character, which should return 0 and reset the state to the initial state.
    ret = mbrtowc(&wc, "\x00\x00", 2, &state);
    EXPECT_EQ(ret, 0ul);
    EXPECT_EQ(wc, static_cast<wchar_t>(0));
    EXPECT_NE(mbsinit(&state), 0);

    // Try an incomplete multibyte character.
    ret = mbrtowc(&wc, "\xe2\x84", 2, &state);
    EXPECT_EQ(ret, -2ul);
    EXPECT_EQ(mbsinit(&state), 0);

    mbstate_t incomplete_state = state;

    // Finish the previous multibyte character.
    ret = mbrtowc(&wc, "\xa2", 1, &state);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(wc, static_cast<wchar_t>(0x2122));

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

TEST_CASE(wcrtomb)
{
    char buf[MB_LEN_MAX];
    size_t ret = 0;

    // Ensure that `wc` is ignored when buf is a nullptr.
    ret = wcrtomb(nullptr, L'a', nullptr);
    EXPECT_EQ(ret, 1ul);

    ret = wcrtomb(nullptr, L'\U0001F41E', nullptr);
    EXPECT_EQ(ret, 1ul);

    // When the buffer is non-null, the multibyte representation is written into it.
    ret = wcrtomb(buf, L'a', nullptr);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(memcmp(buf, "a", ret), 0);

    ret = wcrtomb(buf, L'\U0001F41E', nullptr);
    EXPECT_EQ(ret, 4ul);
    EXPECT_EQ(memcmp(buf, "\xf0\x9f\x90\x9e", ret), 0);

    // When the wide character is invalid, -1 is returned and errno is set to EILSEQ.
    ret = wcrtomb(buf, 0x110000, nullptr);
    EXPECT_EQ(ret, (size_t)-1);
    EXPECT_EQ(errno, EILSEQ);

    // Replacement characters and conversion errors are not confused.
    ret = wcrtomb(buf, L'\uFFFD', nullptr);
    EXPECT_NE(ret, (size_t)-1);
}

TEST_CASE(wcsrtombs)
{
    mbstate_t state = {};
    char buf[MB_LEN_MAX * 4];
    wchar_t const good_chars[] = { L'\U0001F41E', L'\U0001F41E', L'\0' };
    wchar_t const bad_chars[] = { L'\U0001F41E', static_cast<wchar_t>(0x1111F41E), L'\0' };
    wchar_t const* src;
    size_t ret = 0;

    // Convert normal and valid wchar_t values.
    src = good_chars;
    ret = wcsrtombs(buf, &src, 9, &state);
    EXPECT_EQ(ret, 8ul);
    EXPECT_EQ(memcmp(buf, "\xf0\x9f\x90\x9e\xf0\x9f\x90\x9e", 9), 0);
    EXPECT_EQ(src, nullptr);
    EXPECT_NE(mbsinit(&state), 0);

    // Stop on invalid wchar values.
    src = bad_chars;
    ret = wcsrtombs(buf, &src, 9, &state);
    EXPECT_EQ(ret, -1ul);
    EXPECT_EQ(memcmp(buf, "\xf0\x9f\x90\x9e", 4), 0);
    EXPECT_EQ(errno, EILSEQ);
    EXPECT_EQ(src, bad_chars + 1);

    // Valid characters but not enough space.
    src = good_chars;
    ret = wcsrtombs(buf, &src, 7, &state);
    EXPECT_EQ(ret, 4ul);
    EXPECT_EQ(memcmp(buf, "\xf0\x9f\x90\x9e", 4), 0);
    EXPECT_EQ(src, good_chars + 1);

    // Try a conversion with no destination and too short length.
    src = good_chars;
    ret = wcsrtombs(nullptr, &src, 2, &state);
    EXPECT_EQ(ret, 8ul);
    EXPECT_EQ(src, nullptr);
    EXPECT_NE(mbsinit(&state), 0);

    // Try a conversion using the internal anonymous state.
    src = good_chars;
    ret = wcsrtombs(buf, &src, 9, nullptr);
    EXPECT_EQ(ret, 8ul);
    EXPECT_EQ(memcmp(buf, "\xf0\x9f\x90\x9e\xf0\x9f\x90\x9e", 9), 0);
    EXPECT_EQ(src, nullptr);
}

TEST_CASE(wcsnrtombs)
{
    mbstate_t state = {};
    wchar_t const good_chars[] = { L'\U0001F41E', L'\U0001F41E', L'\0' };
    wchar_t const* src;
    size_t ret = 0;

    // Convert nothing.
    src = good_chars;
    ret = wcsnrtombs(nullptr, &src, 0, 0, &state);
    EXPECT_EQ(ret, 0ul);
    EXPECT_EQ(src, good_chars);

    // Convert one wide char.
    src = good_chars;
    ret = wcsnrtombs(nullptr, &src, 1, 0, &state);
    EXPECT_EQ(ret, 4ul);
    EXPECT_EQ(src, good_chars + 1);

    // Encounter a null character.
    src = good_chars;
    ret = wcsnrtombs(nullptr, &src, 4, 0, &state);
    EXPECT_EQ(ret, 8ul);
    EXPECT_EQ(src, nullptr);
}

TEST_CASE(mbsrtowcs)
{
    mbstate_t state = {};
    wchar_t buf[4];
    char const good_chars[] = "\xf0\x9f\x90\x9e\xf0\x9f\x90\x9e";
    char const bad_chars[] = "\xf0\x9f\x90\x9e\xf0\xff\x90\x9e";
    char const* src;
    size_t ret = 0;

    // Convert normal and valid multibyte sequences.
    src = good_chars;
    ret = mbsrtowcs(buf, &src, 3, &state);
    EXPECT_EQ(ret, 2ul);
    EXPECT_EQ(buf[0], L'\U0001F41E');
    EXPECT_EQ(buf[1], L'\U0001F41E');
    EXPECT_EQ(buf[2], L'\0');
    EXPECT_EQ(src, nullptr);
    EXPECT_NE(mbsinit(&state), 0);

    // Stop on invalid multibyte sequences.
    src = bad_chars;
    ret = mbsrtowcs(buf, &src, 3, &state);
    EXPECT_EQ(ret, -1ul);
    EXPECT_EQ(buf[0], L'\U0001F41E');
    EXPECT_EQ(errno, EILSEQ);
    EXPECT_EQ(src, bad_chars + 4);

    // Valid sequence but not enough space.
    src = good_chars;
    ret = mbsrtowcs(buf, &src, 1, &state);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(buf[0], L'\U0001F41E');
    EXPECT_EQ(src, good_chars + 4);

    // Try a conversion with no destination and too short length.
    src = good_chars;
    ret = mbsrtowcs(nullptr, &src, 1, &state);
    EXPECT_EQ(ret, 2ul);
    EXPECT_EQ(src, nullptr);
    EXPECT_NE(mbsinit(&state), 0);

    // Try a conversion using the internal anonymous state.
    src = good_chars;
    ret = mbsrtowcs(buf, &src, 3, nullptr);
    EXPECT_EQ(ret, 2ul);
    EXPECT_EQ(buf[0], L'\U0001F41E');
    EXPECT_EQ(buf[1], L'\U0001F41E');
    EXPECT_EQ(buf[2], L'\0');
    EXPECT_EQ(src, nullptr);
}

TEST_CASE(mbsnrtowcs)
{
    mbstate_t state = {};
    char const good_chars[] = "\xf0\x9f\x90\x9e\xf0\x9f\x90\x9e";
    char const* src;
    size_t ret = 0;

    // Convert nothing.
    src = good_chars;
    ret = mbsnrtowcs(nullptr, &src, 0, 0, &state);
    EXPECT_EQ(ret, 0ul);
    EXPECT_EQ(src, good_chars);

    // Convert one full wide character.
    src = good_chars;
    ret = mbsnrtowcs(nullptr, &src, 4, 0, &state);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(src, good_chars + 4);

    // Encounter a null character.
    src = good_chars;
    ret = mbsnrtowcs(nullptr, &src, 10, 0, &state);
    EXPECT_EQ(ret, 2ul);
    EXPECT_EQ(src, nullptr);

    // Convert an incomplete character.
    // Make sure that we point past the last processed byte.
    src = good_chars;
    ret = mbsnrtowcs(nullptr, &src, 6, 0, &state);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(src, good_chars + 6);
    EXPECT_EQ(mbsinit(&state), 0);

    // Finish converting the incomplete character.
    ret = mbsnrtowcs(nullptr, &src, 2, 0, &state);
    EXPECT_EQ(ret, 1ul);
    EXPECT_EQ(src, good_chars + 8);
}

TEST_CASE(wcslcpy)
{
    auto buf = static_cast<wchar_t*>(malloc(8 * sizeof(wchar_t)));
    if (!buf) {
        FAIL("Could not allocate space for copy target");
        return;
    }

    size_t ret;

    // If buffer is long enough, a straight-forward string copy is performed.
    ret = wcslcpy(buf, L"abc", 8);
    EXPECT_EQ(ret, 3ul);
    EXPECT_EQ(wmemcmp(L"abc", buf, 4), 0);

    // If buffer is (supposedly) too small, the string will be truncated.
    ret = wcslcpy(buf, L"1234", 4);
    EXPECT_EQ(ret, 4ul);
    EXPECT_EQ(wmemcmp(L"123", buf, 4), 0);

    // If the buffer is null, the length of the input is returned.
    ret = wcslcpy(nullptr, L"abc", 0);
    EXPECT_EQ(ret, 3ul);
}

TEST_CASE(mbrlen)
{
    size_t ret = 0;
    mbstate_t state = {};

    // Ensure that we can parse normal ASCII characters.
    ret = mbrlen("Hello", 5, &state);
    EXPECT_EQ(ret, 1ul);

    // Try two three-byte codepoints (™™), only one of which should be consumed.
    ret = mbrlen("\xe2\x84\xa2\xe2\x84\xa2", 6, &state);
    EXPECT_EQ(ret, 3ul);

    // Try a null character, which should return 0 and reset the state to the initial state.
    ret = mbrlen("\x00\x00", 2, &state);
    EXPECT_EQ(ret, 0ul);
    EXPECT_NE(mbsinit(&state), 0);

    // Try an incomplete multibyte character.
    ret = mbrlen("\xe2\x84", 2, &state);
    EXPECT_EQ(ret, -2ul);
    EXPECT_EQ(mbsinit(&state), 0);

    // Finish the previous multibyte character.
    ret = mbrlen("\xa2", 1, &state);
    EXPECT_EQ(ret, 1ul);

    // Try an invalid multibyte sequence.
    // Reset the state afterwards because the effects are undefined.
    ret = mbrlen("\xff", 1, &state);
    EXPECT_EQ(ret, -1ul);
    EXPECT_EQ(errno, EILSEQ);
    state = {};
}

TEST_CASE(mbtowc)
{
    int ret = 0;
    wchar_t wc = 0;

    // Ensure that we can parse normal ASCII characters.
    ret = mbtowc(&wc, "Hello", 5);
    EXPECT_EQ(ret, 1);
    EXPECT_EQ(wc, static_cast<wchar_t>('H'));

    // Try two three-byte codepoints (™™), only one of which should be consumed.
    ret = mbtowc(&wc, "\xe2\x84\xa2\xe2\x84\xa2", 6);
    EXPECT_EQ(ret, 3);
    EXPECT_EQ(wc, static_cast<wchar_t>(0x2122));

    // Try a null character, which should return 0.
    ret = mbtowc(&wc, "\x00\x00", 2);
    EXPECT_EQ(ret, 0);
    EXPECT_EQ(wc, static_cast<wchar_t>(0));

    // Try an incomplete multibyte character.
    ret = mbtowc(&wc, "\xe2\x84", 2);
    EXPECT_EQ(ret, -1);
    EXPECT_EQ(errno, EILSEQ);

    // Ask if we support shift states and reset the internal state in the process.
    ret = mbtowc(nullptr, nullptr, 2);
    EXPECT_EQ(ret, 0); // We don't support shift states.
    ret = mbtowc(nullptr, "\x00", 1);
    EXPECT_EQ(ret, 0); // No error likely means that the state is working again.

    // Try an invalid multibyte sequence.
    ret = mbtowc(&wc, "\xff", 1);
    EXPECT_EQ(ret, -1);
    EXPECT_EQ(errno, EILSEQ);

    // Try a successful conversion, but without target address.
    ret = mbtowc(nullptr, "\xe2\x84\xa2\xe2\x84\xa2", 6);
    EXPECT_EQ(ret, 3);
}

TEST_CASE(mblen)
{
    int ret = 0;

    // Ensure that we can parse normal ASCII characters.
    ret = mblen("Hello", 5);
    EXPECT_EQ(ret, 1);

    // Try two three-byte codepoints (™™), only one of which should be consumed.
    ret = mblen("\xe2\x84\xa2\xe2\x84\xa2", 6);
    EXPECT_EQ(ret, 3);

    // Try a null character, which should return 0.
    ret = mblen("\x00\x00", 2);
    EXPECT_EQ(ret, 0);

    // Try an incomplete multibyte character.
    ret = mblen("\xe2\x84", 2);
    EXPECT_EQ(ret, -1);
    EXPECT_EQ(errno, EILSEQ);

    // Ask if we support shift states and reset the internal state in the process.
    ret = mblen(nullptr, 2);
    EXPECT_EQ(ret, 0); // We don't support shift states.
    ret = mblen("\x00", 1);
    EXPECT_EQ(ret, 0); // No error likely means that the state is working again.

    // Try an invalid multibyte sequence.
    ret = mblen("\xff", 1);
    EXPECT_EQ(ret, -1);
    EXPECT_EQ(errno, EILSEQ);
}

TEST_CASE(wcsftime)
{
    // FIXME: Test actual wide char inputs once those are implemented.

    auto* buf = static_cast<wchar_t*>(malloc(32 * sizeof(wchar_t)));
    if (!buf) {
        FAIL("Could not allocate space for copy target");
        return;
    }

    struct tm time = {
        .tm_sec = 54,
        .tm_min = 44,
        .tm_hour = 12,
        .tm_mday = 27,
        .tm_mon = 4,
        .tm_year = 121,
        .tm_wday = 4,
        .tm_yday = 0,
        .tm_isdst = 0,
    };

    size_t ret;

    // Normal behavior.
    ret = wcsftime(buf, 32, L"%a, %d %b %Y %H:%M:%S", &time);
    EXPECT_EQ(ret, 25ul);
    EXPECT_EQ(wcscmp(buf, L"Thu, 27 May 2021 12:44:54"), 0);

    // String fits exactly.
    ret = wcsftime(buf, 26, L"%a, %d %b %Y %H:%M:%S", &time);
    EXPECT_EQ(ret, 25ul);
    EXPECT_EQ(wcscmp(buf, L"Thu, 27 May 2021 12:44:54"), 0);

    // Buffer is too small.
    ret = wcsftime(buf, 25, L"%a, %d %b %Y %H:%M:%S", &time);
    EXPECT_EQ(ret, 0ul);
    ret = wcsftime(buf, 1, L"%a, %d %b %Y %H:%M:%S", &time);
    EXPECT_EQ(ret, 0ul);
    ret = wcsftime(nullptr, 0, L"%a, %d %b %Y %H:%M:%S", &time);
    EXPECT_EQ(ret, 0ul);
}
