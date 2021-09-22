/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <errno.h>
#include <wchar.h>

static void mbstate_reset(mbstate_t* state)
{
    *state = { 0 };
}

static unsigned int mbstate_stored_bytes(mbstate_t* state)
{
    for (unsigned int i = 0; i < sizeof(state->bytes); i++) {
        if (!state->bytes[i]) {
            return i;
        }
    }

    return sizeof(state->bytes);
}

static unsigned int mbstate_expected_bytes(mbstate_t* state)
{
    unsigned char first = state->bytes[0];

    // Single-byte sequences have their first bit unset
    if ((first & 0b10000000) == 0) {
        return 1;
    }

    // Two-byte sequences start with 0b110xxxxx
    if ((first & 0b11100000) == 0b11000000) {
        return 2;
    }

    // Three-byte sequences start with 0b1110xxxx
    if ((first & 0b11110000) == 0b11100000) {
        return 3;
    }

    // Four-byte sequences start with 0b11110xxx
    if ((first & 0b11111000) == 0b11110000) {
        return 4;
    }

    // Everything else is invalid
    return 0;
}

extern "C" {

size_t wcslen(const wchar_t* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

wchar_t* wcscpy(wchar_t* dest, const wchar_t* src)
{
    wchar_t* original_dest = dest;
    while ((*dest++ = *src++) != '\0')
        ;
    return original_dest;
}

wchar_t* wcsncpy(wchar_t* dest, const wchar_t* src, size_t num)
{
    wchar_t* original_dest = dest;
    while (((*dest++ = *src++) != '\0') && ((size_t)(dest - original_dest) < num))
        ;
    return original_dest;
}

int wcscmp(const wchar_t* s1, const wchar_t* s2)
{
    while (*s1 == *s2++)
        if (*s1++ == 0)
            return 0;
    return *(const wchar_t*)s1 - *(const wchar_t*)--s2;
}

int wcsncmp(const wchar_t* s1, const wchar_t* s2, size_t n)
{
    if (!n)
        return 0;
    do {
        if (*s1 != *s2++)
            return *(const wchar_t*)s1 - *(const wchar_t*)--s2;
        if (*s1++ == 0)
            break;
    } while (--n);
    return 0;
}

wchar_t* wcschr(const wchar_t* str, int c)
{
    wchar_t ch = c;
    for (;; ++str) {
        if (*str == ch)
            return const_cast<wchar_t*>(str);
        if (!*str)
            return nullptr;
    }
}

const wchar_t* wcsrchr(const wchar_t* str, wchar_t wc)
{
    wchar_t* last = nullptr;
    wchar_t c;
    for (; (c = *str); ++str) {
        if (c == wc)
            last = const_cast<wchar_t*>(str);
    }
    return last;
}

wchar_t* wcscat(wchar_t* dest, const wchar_t* src)
{
    size_t dest_length = wcslen(dest);
    size_t i;
    for (i = 0; src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

wchar_t* wcsncat(wchar_t* dest, const wchar_t* src, size_t n)
{
    size_t dest_length = wcslen(dest);
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

wchar_t* wcstok(wchar_t* str, const wchar_t* delim, wchar_t** ptr)
{
    wchar_t* used_str = str;
    if (!used_str) {
        used_str = *ptr;
    }

    size_t token_start = 0;
    size_t token_end = 0;
    size_t str_len = wcslen(used_str);
    size_t delim_len = wcslen(delim);

    for (size_t i = 0; i < str_len; ++i) {
        bool is_proper_delim = false;

        for (size_t j = 0; j < delim_len; ++j) {
            if (used_str[i] == delim[j]) {
                // Skip beginning delimiters
                if (token_end - token_start == 0) {
                    ++token_start;
                    break;
                }

                is_proper_delim = true;
            }
        }

        ++token_end;
        if (is_proper_delim && token_end > 0) {
            --token_end;
            break;
        }
    }

    if (used_str[token_start] == '\0')
        return nullptr;

    if (token_end == 0) {
        return &used_str[token_start];
    }

    used_str[token_end] = '\0';
    return &used_str[token_start];
}

long wcstol(const wchar_t*, wchar_t**, int)
{
    dbgln("FIXME: Implement wcstol()");
    TODO();
}

long long wcstoll(const wchar_t*, wchar_t**, int)
{
    dbgln("FIXME: Implement wcstoll()");
    TODO();
}

wint_t btowc(int c)
{
    if (c == EOF) {
        return WEOF;
    }

    // Multi-byte sequences in UTF-8 have their highest bit set
    if (c & (1 << 7)) {
        return WEOF;
    }

    return c;
}

size_t mbrtowc(wchar_t* pwc, const char* s, size_t n, mbstate_t* state)
{
    static mbstate_t _anonymous_state = { 0 };

    if (state == nullptr) {
        state = &_anonymous_state;
    }

    // If s is nullptr, check if the state contains a complete multibyte character
    if (s == nullptr) {
        if (mbstate_expected_bytes(state) == mbstate_stored_bytes(state)) {
            mbstate_reset(state);
            return 0;
        } else {
            mbstate_reset(state);
            errno = EILSEQ;
            return -1;
        }
    }

    // Stop early if we can't read anything
    if (n == 0) {
        return 0;
    }

    size_t consumed_bytes = 0;
    size_t stored_bytes = mbstate_stored_bytes(state);

    // Fill the first byte if we haven't done that yet
    if (state->bytes[0] == 0) {
        state->bytes[0] = s[0];
        consumed_bytes++;
    }

    size_t expected_bytes = mbstate_expected_bytes(state);

    // Check if the first byte is invalid
    if (expected_bytes == 0) {
        mbstate_reset(state);
        errno = EILSEQ;
        return -1;
    }

    size_t needed_bytes = expected_bytes - stored_bytes;

    while (consumed_bytes < needed_bytes) {
        if (consumed_bytes == n) {
            // No complete multibyte character
            return -2;
        }

        unsigned char c = s[consumed_bytes];

        // Continuation bytes have to start with 0b10xxxxxx
        if ((c & 0b11000000) != 0b10000000) {
            // Invalid multibyte character
            mbstate_reset(state);
            errno = EILSEQ;
            return -1;
        }

        state->bytes[mbstate_stored_bytes(state)] = c;
        consumed_bytes++;
    }

    wchar_t codepoint = state->bytes[0];

    // Mask out the "length" bits if necessary
    if (expected_bytes > 1) {
        codepoint &= (1 << (7 - expected_bytes)) - 1;
    }

    for (unsigned int i = 1; i < expected_bytes; i++) {
        // Each continuation byte contains 6 bits of data
        codepoint = codepoint << 6;
        codepoint |= state->bytes[i] & 0b111111;
    }

    if (pwc) {
        *pwc = codepoint;
    }

    // We don't have a shift state that we need to keep, so just clear the entire state
    mbstate_reset(state);

    if (codepoint == 0) {
        return 0;
    }

    return consumed_bytes;
}

size_t mbrlen(const char*, size_t, mbstate_t*)
{
    dbgln("FIXME: Implement mbrlen()");
    TODO();
}

size_t wcrtomb(char*, wchar_t, mbstate_t*)
{
    dbgln("FIXME: Implement wcrtomb()");
    TODO();
}

int wcscoll(const wchar_t* ws1, const wchar_t* ws2)
{
    // TODO: Actually implement a sensible sort order for this,
    // because right now we are doing what LC_COLLATE=C would do.
    return wcscmp(ws1, ws2);
}

int wctob(wint_t)
{
    dbgln("FIXME: Implement wctob()");
    TODO();
}

int mbsinit(const mbstate_t* state)
{
    if (!state) {
        return 1;
    }

    for (unsigned char byte : state->bytes) {
        if (byte) {
            return 0;
        }
    }

    return 1;
}

wchar_t* wcspbrk(const wchar_t* wcs, const wchar_t* accept)
{
    for (const wchar_t* cur = accept; *cur; cur++) {
        wchar_t* res = wcschr(wcs, *cur);
        if (res)
            return res;
    }

    return nullptr;
}

wchar_t* wcsstr(const wchar_t* haystack, const wchar_t* needle)
{
    size_t nlen = wcslen(needle);

    if (nlen == 0)
        return const_cast<wchar_t*>(haystack);

    size_t hlen = wcslen(haystack);

    while (hlen >= nlen) {
        if (wcsncmp(haystack, needle, nlen) == 0)
            return const_cast<wchar_t*>(haystack);

        haystack++;
        hlen--;
    }

    return nullptr;
}

wchar_t* wmemchr(const wchar_t* s, wchar_t c, size_t n)
{
    for (size_t i = 0; i < n; i++) {
        if (s[i] == c)
            return const_cast<wchar_t*>(&s[i]);
    }

    return nullptr;
}

wchar_t* wmemcpy(wchar_t* dest, const wchar_t* src, size_t n)
{
    for (size_t i = 0; i < n; i++)
        dest[i] = src[i];

    return dest;
}

wchar_t* wmemset(wchar_t* wcs, wchar_t wc, size_t n)
{
    for (size_t i = 0; i < n; i++) {
        wcs[i] = wc;
    }

    return wcs;
}

wchar_t* wmemmove(wchar_t* dest, const wchar_t* src, size_t n)
{
    if (dest > src) {
        for (size_t i = 1; i <= n; i++) {
            dest[n - i] = src[n - i];
        }
    } else if (dest < src) {
        for (size_t i = 0; i < n; i++) {
            dest[i] = src[i];
        }
    }

    return dest;
}

unsigned long wcstoul(const wchar_t*, wchar_t**, int)
{
    dbgln("TODO: Implement wcstoul()");
    TODO();
}
}
