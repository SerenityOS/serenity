/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/UnicodeUtils.h>
#include <errno.h>
#include <string.h>
#include <wchar.h>

static unsigned int mbstate_expected_bytes(mbstate_t* state)
{
    if (state->stored_bytes == 0) {
        return 0;
    }

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

size_t wcslcpy(wchar_t* dest, const wchar_t* src, size_t n)
{
    size_t i;
    for (i = 0; i + 1 < n && src[i] != L'\0'; ++i)
        dest[i] = src[i];
    if (n)
        dest[i] = L'\0';
    for (; src[i] != L'\0'; ++i)
        ; // Determine the length of src, don't copy.
    return i;
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

wchar_t* wcsrchr(const wchar_t* str, wchar_t wc)
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
    static mbstate_t _anonymous_state = {};

    if (state == nullptr) {
        state = &_anonymous_state;
    }

    // s being a null pointer is a shorthand for reading a single null byte.
    if (s == nullptr) {
        pwc = nullptr;
        s = "";
        n = 1;
    }

    // Stop early if we can't read anything
    if (n == 0) {
        return 0;
    }

    size_t consumed_bytes = 0;

    // Fill the first byte if we haven't done that yet
    if (state->stored_bytes == 0) {
        state->bytes[state->stored_bytes++] = s[0];
        consumed_bytes++;
    }

    size_t expected_bytes = mbstate_expected_bytes(state);

    // Check if the first byte is invalid
    if (expected_bytes == 0) {
        *state = {};
        errno = EILSEQ;
        return -1;
    }

    while (state->stored_bytes < expected_bytes) {
        if (consumed_bytes == n) {
            // No complete multibyte character
            return -2;
        }

        unsigned char c = s[consumed_bytes];

        // Continuation bytes have to start with 0b10xxxxxx
        if ((c & 0b11000000) != 0b10000000) {
            // Invalid multibyte character
            *state = {};
            errno = EILSEQ;
            return -1;
        }

        state->bytes[state->stored_bytes++] = c;
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

    // We want to read the next multibyte character, but keep all other properties.
    state->stored_bytes = 0;

    if (codepoint == 0) {
        *state = {};
        return 0;
    }

    return consumed_bytes;
}

size_t mbrlen(const char* s, size_t n, mbstate_t* ps)
{
    static mbstate_t anonymous_state = {};

    if (ps == nullptr)
        ps = &anonymous_state;

    return mbrtowc(nullptr, s, n, ps);
}

size_t wcrtomb(char* s, wchar_t wc, mbstate_t*)
{
    if (s == nullptr)
        wc = L'\0';

    auto nwritten = AK::UnicodeUtils::code_point_to_utf8(wc, [&s](char byte) {
        if (s != nullptr)
            *s++ = byte;
    });

    if (nwritten < 0) {
        errno = EILSEQ;
        return (size_t)-1;
    } else {
        return nwritten;
    }
}

int wcscoll(const wchar_t* ws1, const wchar_t* ws2)
{
    // TODO: Actually implement a sensible sort order for this,
    // because right now we are doing what LC_COLLATE=C would do.
    return wcscmp(ws1, ws2);
}

size_t wcsxfrm(wchar_t* dest, const wchar_t* src, size_t n)
{
    // TODO: This needs to be changed when wcscoll is not just doing wcscmp
    return wcslcpy(dest, src, n);
}

int wctob(wint_t c)
{
    if (c > 0x7f)
        return EOF;

    return static_cast<unsigned char>(c);
}

int mbsinit(const mbstate_t* state)
{
    if (!state) {
        return 1;
    }

    if (state->stored_bytes != 0) {
        return 0;
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

unsigned long long wcstoull(const wchar_t*, wchar_t**, int)
{
    dbgln("TODO: Implement wcstoull()");
    TODO();
}

float wcstof(const wchar_t*, wchar_t**)
{
    dbgln("TODO: Implement wcstof()");
    TODO();
}

double wcstod(const wchar_t*, wchar_t**)
{
    dbgln("TODO: Implement wcstod()");
    TODO();
}

long double wcstold(const wchar_t*, wchar_t**)
{
    dbgln("TODO: Implement wcstold()");
    TODO();
}

int swprintf(wchar_t*, size_t, const wchar_t*, ...)
{
    dbgln("TODO: Implement swprintf()");
    TODO();
}

int wcwidth(wchar_t wc)
{
    if (wc == L'\0')
        return 0;

    // Printable ASCII.
    if (wc >= 0x20 && wc <= 0x7e)
        return 1;

    // Non-printable ASCII.
    if (wc <= 0x7f)
        return -1;

    // TODO: Implement wcwidth for non-ASCII characters.
    return 1;
}

size_t wcsnrtombs(char* dest, const wchar_t** src, size_t nwc, size_t len, mbstate_t* ps)
{
    static mbstate_t _anonymous_state = {};

    if (ps == nullptr)
        ps = &_anonymous_state;

    size_t written = 0;
    size_t read = 0;
    while (read < nwc) {
        size_t ret = 0;
        char buf[MB_LEN_MAX];

        // Convert next wchar to multibyte.
        ret = wcrtomb(buf, **src, ps);

        // wchar can't be represented as multibyte.
        if (ret == (size_t)-1) {
            errno = EILSEQ;
            return (size_t)-1;
        }

        // New bytes don't fit the buffer.
        if (dest && len < written + ret) {
            return written;
        }

        if (dest) {
            memcpy(dest, buf, ret);
            dest += ret;
        }

        // Null character has been reached
        if (**src == L'\0') {
            *src = nullptr;
            return written;
        }

        *src += 1;
        read += 1;
        written += ret;
    }

    return written;
}

size_t mbsnrtowcs(wchar_t* dst, const char** src, size_t nms, size_t len, mbstate_t* ps)
{
    static mbstate_t _anonymous_state = {};

    if (ps == nullptr)
        ps = &_anonymous_state;

    size_t written = 0;
    while (written < len || !dst) {
        // End of source buffer, no incomplete character.
        // src continues to point to the next byte.
        if (nms == 0) {
            return written;
        }

        // Convert next multibyte to wchar.
        size_t ret = mbrtowc(dst, *src, nms, ps);

        // Multibyte sequence is incomplete.
        if (ret == -2ul) {
            // Point just past the last processed byte.
            *src += nms;
            return written;
        }

        // Multibyte sequence is invalid.
        if (ret == -1ul) {
            errno = EILSEQ;
            return (size_t)-1;
        }

        // Null byte has been reached.
        if (**src == '\0') {
            *src = nullptr;
            return written;
        }

        *src += ret;
        nms -= ret;
        written += 1;
        if (dst)
            dst += 1;
    }

    // If we are here, we have written `len` wchars, but not reached the null byte.
    return written;
}

int wmemcmp(const wchar_t* s1, const wchar_t* s2, size_t n)
{
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

size_t wcsrtombs(char* dest, const wchar_t** src, size_t len, mbstate_t* ps)
{
    static mbstate_t anonymous_state = {};

    if (ps == nullptr)
        ps = &anonymous_state;

    // SIZE_MAX is as close as we are going to get to "unlimited".
    return wcsnrtombs(dest, src, SIZE_MAX, len, ps);
}

size_t mbsrtowcs(wchar_t* dst, const char** src, size_t len, mbstate_t* ps)
{
    static mbstate_t anonymous_state = {};

    if (ps == nullptr)
        ps = &anonymous_state;

    // SIZE_MAX is as close as we are going to get to "unlimited".
    return mbsnrtowcs(dst, src, SIZE_MAX, len, ps);
}
}
