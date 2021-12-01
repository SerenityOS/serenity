/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

size_t strspn(const char* s, const char* accept)
{
    const char* p = s;
cont:
    char ch = *p++;
    char ac;
    for (const char* ap = accept; (ac = *ap++) != '\0';) {
        if (ac == ch)
            goto cont;
    }
    return p - 1 - s;
}

size_t strcspn(const char* s, const char* reject)
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

size_t strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

size_t strnlen(const char* str, size_t maxlen)
{
    size_t len = 0;
    for (; len < maxlen && *str; str++)
        len++;
    return len;
}

char* strdup(const char* str)
{
    size_t len = strlen(str);
    char* new_str = (char*)malloc(len + 1);
    memcpy(new_str, str, len);
    new_str[len] = '\0';
    return new_str;
}

char* strndup(const char* str, size_t maxlen)
{
    size_t len = strnlen(str, maxlen);
    char* new_str = (char*)malloc(len + 1);
    memcpy(new_str, str, len);
    new_str[len] = 0;
    return new_str;
}

int strcmp(const char* s1, const char* s2)
{
    while (*s1 == *s2++)
        if (*s1++ == 0)
            return 0;
    return *(const unsigned char*)s1 - *(const unsigned char*)--s2;
}

int strncmp(const char* s1, const char* s2, size_t n)
{
    if (!n)
        return 0;
    do {
        if (*s1 != *s2++)
            return *(const unsigned char*)s1 - *(const unsigned char*)--s2;
        if (*s1++ == 0)
            break;
    } while (--n);
    return 0;
}

int memcmp(const void* v1, const void* v2, size_t n)
{
    auto* s1 = (const uint8_t*)v1;
    auto* s2 = (const uint8_t*)v2;
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

void* memcpy(void* dest_ptr, const void* src_ptr, size_t n)
{
    void* original_dest = dest_ptr;
    asm volatile(
        "rep movsb"
        : "+D"(dest_ptr), "+S"(src_ptr), "+c"(n)::"memory");
    return original_dest;
}

void* memset(void* dest_ptr, int c, size_t n)
{
    size_t dest = (size_t)dest_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        size_t expanded_c = explode_byte((u8)c);
#if ARCH(I386)
        asm volatile(
            "rep stosl\n"
            : "=D"(dest)
            : "D"(dest), "c"(size_ts), "a"(expanded_c)
            : "memory");
#else
        asm volatile(
            "rep stosq\n"
            : "=D"(dest)
            : "D"(dest), "c"(size_ts), "a"(expanded_c)
            : "memory");
#endif
        n -= size_ts * sizeof(size_t);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep stosb\n"
        : "=D"(dest), "=c"(n)
        : "0"(dest), "1"(n), "a"(c)
        : "memory");
    return dest_ptr;
}

void* memmove(void* dest, const void* src, size_t n)
{
    if (((FlatPtr)dest - (FlatPtr)src) >= n)
        return memcpy(dest, src, n);

    u8* pd = (u8*)dest;
    const u8* ps = (const u8*)src;
    for (pd += n, ps += n; n--;)
        *--pd = *--ps;
    return dest;
}

const void* memmem(const void* haystack, size_t haystack_length, const void* needle, size_t needle_length)
{
    return AK::memmem(haystack, haystack_length, needle, needle_length);
}

char* strcpy(char* dest, const char* src)
{
    char* original_dest = dest;
    while ((*dest++ = *src++) != '\0')
        ;
    return original_dest;
}

char* strncpy(char* dest, const char* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for (; i < n; ++i)
        dest[i] = '\0';
    return dest;
}

size_t strlcpy(char* dest, const char* src, size_t n)
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

char* strchr(const char* str, int c)
{
    char ch = c;
    for (;; ++str) {
        if (*str == ch)
            return const_cast<char*>(str);
        if (!*str)
            return nullptr;
    }
}

char* strchrnul(const char* str, int c)
{
    char ch = c;
    for (;; ++str) {
        if (*str == ch || !*str)
            return const_cast<char*>(str);
    }
}

void* memchr(const void* ptr, int c, size_t size)
{
    char ch = c;
    auto* cptr = (const char*)ptr;
    for (size_t i = 0; i < size; ++i) {
        if (cptr[i] == ch)
            return const_cast<char*>(cptr + i);
    }
    return nullptr;
}

char* strrchr(const char* str, int ch)
{
    char* last = nullptr;
    char c;
    for (; (c = *str); ++str) {
        if (c == ch)
            last = const_cast<char*>(str);
    }
    return last;
}

char* strcat(char* dest, const char* src)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0; src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

char* strncat(char* dest, const char* src, size_t n)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

const char* const sys_errlist[] = {
    "Success (not an error)",
    "Operation not permitted",
    "No such file or directory",
    "No such process",
    "Interrupted syscall",
    "I/O error",
    "No such device or address",
    "Argument list too long",
    "Exec format error",
    "Bad fd number",
    "No child processes",
    "Try again",
    "Out of memory",
    "Permission denied",
    "Bad address",
    "Block device required",
    "Device or resource busy",
    "File already exists",
    "Cross-device link",
    "No such device",
    "Not a directory",
    "Is a directory",
    "Invalid argument",
    "File table overflow",
    "Too many open files",
    "Not a TTY",
    "Text file busy",
    "File too large",
    "No space left on device",
    "Illegal seek",
    "Read-only filesystem",
    "Too many links",
    "Broken pipe",
    "Range error",
    "Name too long",
    "Too many symlinks",
    "Overflow",
    "Operation not supported",
    "No such syscall",
    "Not implemented",
    "Address family not supported",
    "Not a socket",
    "Address in use",
    "Failed without setting an error code (bug!)",
    "Directory not empty",
    "Math argument out of domain",
    "Connection refused",
    "Host is down",
    "Address not available",
    "Already connected",
    "Connection aborted",
    "Connection already in progress",
    "Connection reset",
    "Destination address required",
    "Host unreachable",
    "Illegal byte sequence",
    "Message size",
    "Network down",
    "Network unreachable",
    "Network reset",
    "No buffer space",
    "No lock available",
    "No message",
    "No protocol option",
    "Not connected",
    "Transport endpoint has shutdown",
    "Too many references",
    "Protocol not supported",
    "Socket type not supported",
    "Resource deadlock would occur",
    "Timed out",
    "Wrong protocol type",
    "Operation in progress",
    "No such thread",
    "Protocol error",
    "Not supported",
    "Protocol family not supported",
    "Cannot make directory a subdirectory of itself",
    "Quota exceeded",
    "State not recoverable",
    "The highest errno +1 :^)",
};
static_assert(array_size(sys_errlist) == (EMAXERRNO + 1));

int sys_nerr = EMAXERRNO;

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

char* strerror(int errnum)
{
    if (errnum < 0 || errnum >= EMAXERRNO) {
        return const_cast<char*>("Unknown error");
    }
    return const_cast<char*>(sys_errlist[errnum]);
}

char* strsignal(int signum)
{
    if (signum >= NSIG) {
        dbgln("strsignal() missing string for signum={}", signum);
        return const_cast<char*>("Unknown signal");
    }
    return const_cast<char*>(sys_siglist[signum]);
}

char* strstr(const char* haystack, const char* needle)
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

char* strpbrk(const char* s, const char* accept)
{
    while (*s)
        if (strchr(accept, *s++))
            return const_cast<char*>(--s);
    return nullptr;
}

char* strtok_r(char* str, const char* delim, char** saved_str)
{
    if (!str) {
        if (!saved_str)
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

    if (str[token_start] == '\0')
        return nullptr;

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

char* strtok(char* str, const char* delim)
{
    static char* saved_str;
    return strtok_r(str, delim, &saved_str);
}

int strcoll(const char* s1, const char* s2)
{
    return strcmp(s1, s2);
}

size_t strxfrm(char* dest, const char* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for (; i < n; ++i)
        dest[i] = '\0';
    return i;
}

void explicit_bzero(void* ptr, size_t size)
{
    secure_zero(ptr, size);
}
}
