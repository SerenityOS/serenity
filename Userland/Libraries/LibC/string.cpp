/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/MemMem.h>
#include <AK/Memory.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strspn.html
size_t strspn(char const* s, char const* accept)
{
    char const* p = s;
cont:
    char ch = *p++;
    char ac;
    for (char const* ap = accept; (ac = *ap++) != '\0';) {
        if (ac == ch)
            goto cont;
    }
    return p - 1 - s;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strcspn.html
size_t strcspn(char const* s, char const* reject)
{
    for (auto* p = s;;) {
        char c = *p++;
        auto* rp = reject;
        char rc;
        do {
            if ((rc = *rp++) == c)
                return p - 1 - s;
        } while (rc);
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strlen.html
size_t strlen(char const* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strnlen.html
size_t strnlen(char const* str, size_t maxlen)
{
    size_t len = 0;
    for (; len < maxlen && *str; str++)
        len++;
    return len;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strdup.html
char* strdup(char const* str)
{
    size_t len = strlen(str);
    char* new_str = (char*)malloc(len + 1);
    if (!new_str)
        return nullptr;
    memcpy(new_str, str, len);
    new_str[len] = '\0';
    return new_str;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strndup.html
char* strndup(char const* str, size_t maxlen)
{
    size_t len = strnlen(str, maxlen);
    char* new_str = (char*)malloc(len + 1);
    if (!new_str)
        return nullptr;
    memcpy(new_str, str, len);
    new_str[len] = 0;
    return new_str;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strcmp.html
int strcmp(char const* s1, char const* s2)
{
    while (*s1 == *s2++)
        if (*s1++ == 0)
            return 0;
    return *(unsigned char const*)s1 - *(unsigned char const*)--s2;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strncmp.html
int strncmp(char const* s1, char const* s2, size_t n)
{
    if (!n)
        return 0;
    do {
        if (*s1 != *s2++)
            return *(unsigned char const*)s1 - *(unsigned char const*)--s2;
        if (*s1++ == 0)
            break;
    } while (--n);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/memcmp.html
int memcmp(void const* v1, void const* v2, size_t n)
{
    auto* s1 = (uint8_t const*)v1;
    auto* s2 = (uint8_t const*)v2;
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

// Not in POSIX, originated in BSD
// https://man.openbsd.org/timingsafe_memcmp.3
int timingsafe_memcmp(void const* b1, void const* b2, size_t len)
{
    return AK::timing_safe_compare(b1, b2, len) ? 1 : 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/memcpy.html
void* memcpy(void* dest_ptr, void const* src_ptr, size_t n)
{
#if ARCH(X86_64)
    void* original_dest = dest_ptr;
    asm volatile(
        "rep movsb"
        : "+D"(dest_ptr), "+S"(src_ptr), "+c"(n)::"memory");
    return original_dest;
#else
    u8* pd = (u8*)dest_ptr;
    u8 const* ps = (u8 const*)src_ptr;
    for (; n--;)
        *pd++ = *ps++;
    return dest_ptr;
#endif
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/memccpy.html
void* memccpy(void* dest_ptr, void const* src_ptr, int c, size_t n)
{
    u8* pd = static_cast<u8*>(dest_ptr);
    u8 const* ps = static_cast<u8 const*>(src_ptr);
    for (; n--; pd++, ps++) {
        *pd = *ps;
        if (*pd == static_cast<u8>(c))
            return pd + 1;
    }
    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/memset.html
// For x86-64, an optimized ASM implementation is found in ./arch/x86_64/memset.S
#if ARCH(X86_64)
#else
void* memset(void* dest_ptr, int c, size_t n)
{
    u8* pd = (u8*)dest_ptr;
    for (; n--;)
        *pd++ = c;
    return dest_ptr;
}
#endif

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/memmove.html
void* memmove(void* dest, void const* src, size_t n)
{
    if (dest < src)
        return memcpy(dest, src, n);
    if (((FlatPtr)dest - (FlatPtr)src) >= n)
        return memcpy(dest, src, n);

    u8* pd = (u8*)dest;
    u8 const* ps = (u8 const*)src;
    for (pd += n, ps += n; n--;)
        *--pd = *--ps;
    return dest;
}

// https://linux.die.net/man/3/memmem (GNU extension)
void* memmem(void const* haystack, size_t haystack_length, void const* needle, size_t needle_length)
{
    return const_cast<void*>(AK::memmem(haystack, haystack_length, needle, needle_length));
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strcpy.html
char* strcpy(char* dest, char const* src)
{
    char* original_dest = dest;
    while ((*dest = *src) != '\0') {
        dest++;
        src++;
    }
    return original_dest;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/stpcpy.html
char* stpcpy(char* dest, char const* src)
{
    while ((*dest = *src) != '\0') {
        dest++;
        src++;
    }
    return dest;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strncpy.html
char* strncpy(char* dest, char const* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for (; i < n; ++i)
        dest[i] = '\0';
    return dest;
}

// Not in POSIX, originated in BSD but also supported on Linux.
// https://man.openbsd.org/strlcpy.3
size_t strlcpy(char* dest, char const* src, size_t n)
{
    size_t i;
    // Would like to test i < n - 1 here, but n might be 0.
    for (i = 0; i + 1 < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    if (n)
        dest[i] = '\0';
    for (; src[i] != '\0'; ++i)
        ; // Determine the length of src, don't copy.
    return i;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strchr.html
char* strchr(char const* str, int c)
{
    char ch = c;
    for (;; ++str) {
        if (*str == ch)
            return const_cast<char*>(str);
        if (!*str)
            return nullptr;
    }
}

// https://pubs.opengroup.org/onlinepubs/9699959399/functions/index.html
char* index(char const* str, int c)
{
    return strchr(str, c);
}

// https://linux.die.net/man/3/strchrnul (GNU extension)
char* strchrnul(char const* str, int c)
{
    char ch = c;
    for (;; ++str) {
        if (*str == ch || !*str)
            return const_cast<char*>(str);
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/memchr.html
void* memchr(void const* ptr, int c, size_t size)
{
    char ch = c;
    auto* cptr = (char const*)ptr;
    for (size_t i = 0; i < size; ++i) {
        if (cptr[i] == ch)
            return const_cast<char*>(cptr + i);
    }
    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strrchr.html
char* strrchr(char const* str, int ch)
{
    char* last = nullptr;
    char c;
    for (; (c = *str); ++str) {
        if (c == ch)
            last = const_cast<char*>(str);
    }
    return last;
}

// https://pubs.opengroup.org/onlinepubs/9699959399/functions/rindex.html
char* rindex(char const* str, int ch)
{
    return strrchr(str, ch);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strcat.html
char* strcat(char* dest, char const* src)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0; src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strncat.html
char* strncat(char* dest, char const* src, size_t n)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

char const* const sys_errlist[] = {
#define __ENUMERATE_ERRNO_CODE(c, s) s,
    ENUMERATE_ERRNO_CODES(__ENUMERATE_ERRNO_CODE)
#undef __ENUMERATE_ERRNO_CODE
};
static_assert(array_size(sys_errlist) == (EMAXERRNO + 1));

int sys_nerr = EMAXERRNO;

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strerror_r.html
int strerror_r(int errnum, char* buf, size_t buflen)
{
    auto saved_errno = errno;
    if (errnum < 0 || errnum >= EMAXERRNO) {
        auto rc = strlcpy(buf, "unknown error", buflen);
        if (rc >= buflen)
            dbgln("strerror_r(): Invalid error number '{}' specified and the output buffer is too small.", errnum);
        errno = saved_errno;
        return EINVAL;
    }
    auto text = strerror(errnum);
    auto rc = strlcpy(buf, text, buflen);
    if (rc >= buflen) {
        errno = saved_errno;
        return ERANGE;
    }
    errno = saved_errno;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strerror.html
char* strerror(int errnum)
{
    if (errnum < 0 || errnum >= EMAXERRNO) {
        return const_cast<char*>("Unknown error");
    }
    return const_cast<char*>(sys_errlist[errnum]);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strsignal.html
char* strsignal(int signum)
{
    if (signum <= 0 || signum >= NSIG || !sys_siglist[signum]) {
        dbgln("strsignal() missing string for signum={}", signum);
        return const_cast<char*>("Unknown signal");
    }
    return const_cast<char*>(sys_siglist[signum]);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strstr.html
char* strstr(char const* haystack, char const* needle)
{
    char nch;
    char hch;

    if ((nch = *needle++) != 0) {
        size_t len = strlen(needle);
        do {
            do {
                if ((hch = *haystack++) == 0)
                    return nullptr;
            } while (hch != nch);
        } while (strncmp(haystack, needle, len) != 0);
        --haystack;
    }
    return const_cast<char*>(haystack);
}

// https://linux.die.net/man/3/strcasestr
char* strcasestr(char const* haystack, char const* needle)
{
    char nch;
    char hch;

    if ((nch = *needle++) != 0) {
        size_t len = strlen(needle);
        do {
            do {
                if ((hch = *haystack++) == 0)
                    return nullptr;
            } while (toupper(hch) != toupper(nch));
        } while (strncasecmp(haystack, needle, len) != 0);
        --haystack;
    }
    return const_cast<char*>(haystack);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strpbrk.html
char* strpbrk(char const* s, char const* accept)
{
    while (*s)
        if (strchr(accept, *s++))
            return const_cast<char*>(--s);
    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtok_r.html
char* strtok_r(char* str, char const* delim, char** saved_str)
{
    if (!str) {
        if (!saved_str || *saved_str == nullptr)
            return nullptr;
        str = *saved_str;
    }

    size_t token_start = 0;
    size_t token_end = 0;
    size_t str_len = strlen(str);
    size_t delim_len = strlen(delim);

    for (size_t i = 0; i < str_len; ++i) {
        bool is_proper_delim = false;

        for (size_t j = 0; j < delim_len; ++j) {
            if (str[i] == delim[j]) {
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

    if (str[token_start] == '\0') {
        *saved_str = nullptr;
        return nullptr;
    }

    if (token_end == 0) {
        *saved_str = nullptr;
        return &str[token_start];
    }

    if (str[token_end] == '\0')
        *saved_str = &str[token_end];
    else
        *saved_str = &str[token_end + 1];

    str[token_end] = '\0';
    return &str[token_start];
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strtok.html
char* strtok(char* str, char const* delim)
{
    static char* saved_str;
    return strtok_r(str, delim, &saved_str);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strcoll.html
int strcoll(char const* s1, char const* s2)
{
    return strcmp(s1, s2);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/strxfrm.html
size_t strxfrm(char* dest, char const* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for (; i < n; ++i)
        dest[i] = '\0';
    return i;
}

// Not in POSIX, originated in BSD but also supported on Linux.
// https://man.openbsd.org/strsep.3
char* strsep(char** str, char const* delim)
{
    if (*str == nullptr)
        return nullptr;
    auto* begin = *str;
    auto* end = begin + strcspn(begin, delim);
    if (*end) {
        *end = '\0';
        *str = ++end;
    } else {
        *str = nullptr;
    }
    return begin;
}

// Not in POSIX, originated in BSD but also supported on Linux.
// https://man.openbsd.org/explicit_bzero.3
void explicit_bzero(void* ptr, size_t size)
{
    secure_zero(ptr, size);
}
}
