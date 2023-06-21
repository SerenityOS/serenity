/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Tim Schumacher <timschumi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <AK/UnicodeUtils.h>
#include <errno.h>
#include <string.h>
#include <time.h>
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcslen.html
size_t wcslen(wchar_t const* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcscpy.html
wchar_t* wcscpy(wchar_t* dest, wchar_t const* src)
{
    wchar_t* original_dest = dest;
    while ((*dest++ = *src++) != '\0')
        ;
    return original_dest;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsdup.html
wchar_t* wcsdup(wchar_t const* str)
{
    size_t length = wcslen(str);
    wchar_t* new_str = (wchar_t*)malloc(sizeof(wchar_t) * (length + 1));

    if (!new_str) {
        errno = ENOMEM;
        return nullptr;
    }

    return wcscpy(new_str, str);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsncpy.html
wchar_t* wcsncpy(wchar_t* dest, wchar_t const* src, size_t num)
{
    wchar_t* original_dest = dest;
    while (((*dest++ = *src++) != '\0') && ((size_t)(dest - original_dest) < num))
        ;
    return original_dest;
}

size_t wcslcpy(wchar_t* dest, wchar_t const* src, size_t n)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcscmp.html
int wcscmp(wchar_t const* s1, wchar_t const* s2)
{
    while (*s1 == *s2++)
        if (*s1++ == 0)
            return 0;
    return *(wchar_t const*)s1 - *(wchar_t const*)--s2;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsncmp.html
int wcsncmp(wchar_t const* s1, wchar_t const* s2, size_t n)
{
    if (!n)
        return 0;
    do {
        if (*s1 != *s2++)
            return *(wchar_t const*)s1 - *(wchar_t const*)--s2;
        if (*s1++ == 0)
            break;
    } while (--n);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcschr.html
wchar_t* wcschr(wchar_t const* str, int c)
{
    wchar_t ch = c;
    for (;; ++str) {
        if (*str == ch)
            return const_cast<wchar_t*>(str);
        if (!*str)
            return nullptr;
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsrchr.html
wchar_t* wcsrchr(wchar_t const* str, wchar_t wc)
{
    wchar_t* last = nullptr;
    wchar_t c;
    for (; (c = *str); ++str) {
        if (c == wc)
            last = const_cast<wchar_t*>(str);
    }
    return last;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcscat.html
wchar_t* wcscat(wchar_t* dest, wchar_t const* src)
{
    size_t dest_length = wcslen(dest);
    size_t i;
    for (i = 0; src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsncat.html
wchar_t* wcsncat(wchar_t* dest, wchar_t const* src, size_t n)
{
    size_t dest_length = wcslen(dest);
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstok.html
wchar_t* wcstok(wchar_t* str, wchar_t const* delim, wchar_t** ptr)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstol.html
long wcstol(wchar_t const*, wchar_t**, int)
{
    dbgln("FIXME: Implement wcstol()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstoll.html
long long wcstoll(wchar_t const*, wchar_t**, int)
{
    dbgln("FIXME: Implement wcstoll()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/btowc.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mbrtowc.html
size_t mbrtowc(wchar_t* pwc, char const* s, size_t n, mbstate_t* state)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mbrlen.html
size_t mbrlen(char const* s, size_t n, mbstate_t* ps)
{
    static mbstate_t anonymous_state = {};

    if (ps == nullptr)
        ps = &anonymous_state;

    return mbrtowc(nullptr, s, n, ps);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcrtomb.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcscoll.html
int wcscoll(wchar_t const* ws1, wchar_t const* ws2)
{
    // TODO: Actually implement a sensible sort order for this,
    // because right now we are doing what LC_COLLATE=C would do.
    return wcscmp(ws1, ws2);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsxfrm.html
size_t wcsxfrm(wchar_t* dest, wchar_t const* src, size_t n)
{
    // TODO: This needs to be changed when wcscoll is not just doing wcscmp
    return wcslcpy(dest, src, n);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wctob.html
int wctob(wint_t c)
{
    if (c > 0x7f)
        return EOF;

    return static_cast<unsigned char>(c);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mbsinit.html
int mbsinit(mbstate_t const* state)
{
    if (!state) {
        return 1;
    }

    if (state->stored_bytes != 0) {
        return 0;
    }

    return 1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcspbrk.html
wchar_t* wcspbrk(wchar_t const* wcs, wchar_t const* accept)
{
    for (wchar_t const* cur = accept; *cur; cur++) {
        wchar_t* res = wcschr(wcs, *cur);
        if (res)
            return res;
    }

    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsstr.html
wchar_t* wcsstr(wchar_t const* haystack, wchar_t const* needle)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wmemchr.html
wchar_t* wmemchr(wchar_t const* s, wchar_t c, size_t n)
{
    for (size_t i = 0; i < n; i++) {
        if (s[i] == c)
            return const_cast<wchar_t*>(&s[i]);
    }

    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wmemcpy.html
wchar_t* wmemcpy(wchar_t* dest, wchar_t const* src, size_t n)
{
    for (size_t i = 0; i < n; i++)
        dest[i] = src[i];

    return dest;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wmemset.html
wchar_t* wmemset(wchar_t* wcs, wchar_t wc, size_t n)
{
    for (size_t i = 0; i < n; i++) {
        wcs[i] = wc;
    }

    return wcs;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wmemmove.html
wchar_t* wmemmove(wchar_t* dest, wchar_t const* src, size_t n)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstoul.html
unsigned long wcstoul(wchar_t const*, wchar_t**, int)
{
    dbgln("TODO: Implement wcstoul()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstoull.html
unsigned long long wcstoull(wchar_t const*, wchar_t**, int)
{
    dbgln("TODO: Implement wcstoull()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstof.html
float wcstof(wchar_t const*, wchar_t**)
{
    dbgln("TODO: Implement wcstof()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstod.html
double wcstod(wchar_t const*, wchar_t**)
{
    dbgln("TODO: Implement wcstod()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcstold.html
long double wcstold(wchar_t const*, wchar_t**)
{
    dbgln("TODO: Implement wcstold()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcwidth.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcswidth.html
int wcswidth(wchar_t const* pwcs, size_t n)
{
    int len = 0;

    for (size_t i = 0; i < n && pwcs[i]; i++) {
        int char_len = wcwidth(pwcs[i]);

        if (char_len == -1)
            return -1;

        len += char_len;
    }

    return len;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsnrtombs.html
size_t wcsnrtombs(char* dest, wchar_t const** src, size_t nwc, size_t len, mbstate_t* ps)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mbsnrtowcs.html
size_t mbsnrtowcs(wchar_t* dst, char const** src, size_t nms, size_t len, mbstate_t* ps)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wmemcmp.html
int wmemcmp(wchar_t const* s1, wchar_t const* s2, size_t n)
{
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsrtombs.html
size_t wcsrtombs(char* dest, wchar_t const** src, size_t len, mbstate_t* ps)
{
    static mbstate_t anonymous_state = {};

    if (ps == nullptr)
        ps = &anonymous_state;

    // SIZE_MAX is as close as we are going to get to "unlimited".
    return wcsnrtombs(dest, src, SIZE_MAX, len, ps);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mbsrtowcs.html
size_t mbsrtowcs(wchar_t* dst, char const** src, size_t len, mbstate_t* ps)
{
    static mbstate_t anonymous_state = {};

    if (ps == nullptr)
        ps = &anonymous_state;

    // SIZE_MAX is as close as we are going to get to "unlimited".
    return mbsnrtowcs(dst, src, SIZE_MAX, len, ps);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcscspn.html
size_t wcscspn(wchar_t const* wcs, wchar_t const* reject)
{
    for (auto const* wc_pointer = wcs;;) {
        auto c = *wc_pointer++;
        wchar_t rc;
        auto const* reject_copy = reject;
        do {
            if ((rc = *reject_copy++) == c)
                return wc_pointer - 1 - wcs;
        } while (rc != 0);
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsspn.html
size_t wcsspn(wchar_t const* wcs, wchar_t const* accept)
{
    for (auto const* wc_pointer = wcs;;) {
        auto c = *wc_pointer++;
        wchar_t rc;
        auto const* accept_copy = accept;
        do {
            if ((rc = *accept_copy++) != c)
                return wc_pointer - 1 - wcs;
        } while (rc != 0);
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcsftime.html
size_t wcsftime(wchar_t* destination, size_t maxsize, wchar_t const* format, const struct tm* tm)
{
    // FIXME: Add actual wide char support for this.
    char* ascii_format = static_cast<char*>(malloc(wcslen(format) + 1));
    char* ascii_destination = static_cast<char*>(malloc(maxsize));

    VERIFY(ascii_format && ascii_destination);

    // These are copied by value because we will change the pointers without rolling them back.
    ScopeGuard free_ascii = [ascii_format, ascii_destination] {
        free(ascii_format);
        free(ascii_destination);
    };

    char* ascii_format_copy = ascii_format;
    do {
        VERIFY(*format <= 0x7f);
        *ascii_format_copy++ = static_cast<char>(*format);
    } while (*format++ != L'\0');

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wformat-nonliteral"
    size_t ret = strftime(ascii_destination, maxsize, ascii_format, tm);
#pragma GCC diagnostic pop

    if (ret == 0)
        return 0;

    do {
        *destination++ = *ascii_destination;
    } while (*ascii_destination++ != '\0');

    return ret;
}
}
