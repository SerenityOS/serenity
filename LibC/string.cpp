#include <string.h>
#include <errno.h>
#include <stdio.h>
#include <signal.h>
#include <assert.h>
#include <stdlib.h>
#include <AK/Types.h>
#include <AK/StdLibExtras.h>

extern "C" {

void bzero(void* dest, size_t n)
{
    memset(dest, 0, n);
}

void bcopy(const void* src, void* dest, size_t n)
{
    memmove(dest, src, n);
}

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
        } while(rc);
    }
}

size_t strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

char* strdup(const char* str)
{
    size_t len = strlen(str);
    char* new_str = (char*)malloc(len + 1);
    strcpy(new_str, str);
    return new_str;
}

char* strndup(const char* str, size_t maxlen)
{
    size_t len = min(strlen(str), maxlen);
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
    if (n >= 1024)
        return mmx_memcpy(dest_ptr, src_ptr, n);

    dword dest = (dword)dest_ptr;
    dword src = (dword)src_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && !(src & 0x3) && n >= 12) {
        size_t dwords = n / sizeof(dword);
        asm volatile(
            "rep movsl\n"
            : "=S"(src), "=D"(dest)
            : "S"(src), "D"(dest), "c"(dwords)
            : "memory"
        );
        n -= dwords * sizeof(dword);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep movsb\n"
        :: "S"(src), "D"(dest), "c"(n)
        : "memory"
    );
    return dest_ptr;
}

void* memset(void* dest_ptr, int c, size_t n)
{
    dword dest = (dword)dest_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t dwords = n / sizeof(dword);
        dword expanded_c = (byte)c;
        expanded_c |= expanded_c << 8;
        expanded_c |= expanded_c << 16;
        asm volatile(
            "rep stosl\n"
            : "=D"(dest)
            : "D"(dest), "c"(dwords), "a"(expanded_c)
            : "memory"
        );
        n -= dwords * sizeof(dword);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep stosb\n"
        : "=D" (dest), "=c" (n)
        : "0" (dest), "1" (n), "a" (c)
        : "memory"
    );
    return dest_ptr;
}


void* memmove(void* dest, const void* src, size_t n)
{
    if (dest < src)
        return memcpy(dest, src, n);

    byte *pd = (byte*)dest;
    const byte *ps = (const byte*)src;
    for (pd += n, ps += n; n--;)
        *--pd = *--ps;
    return dest;
}

char* strcpy(char* dest, const char *src)
{
    char* originalDest = dest;
    while ((*dest++ = *src++) != '\0');
    return originalDest;
}

char* strncpy(char* dest, const char* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for ( ; i < n; ++i)
        dest[i] = '\0';
    return dest;
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

void* memchr(const void* ptr, int c, size_t size)
{
    char ch = c;
    char* cptr = (char*)ptr;
    for (size_t i = 0; i < size; ++i) {
        if (cptr[i] == ch)
            return cptr + i;
    }
    return nullptr;
}

char* strrchr(const char* str, int ch)
{
    char *last = nullptr;
    char c;
    for (; (c = *str); ++str) {
        if (c == ch)
            last = const_cast<char*>(str);
    }
    return last;
}

char* strcat(char *dest, const char *src)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0 ; src[i] != '\0' ; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

char* strncat(char *dest, const char *src, size_t n)
{
    size_t dest_length = strlen(dest);
    size_t i;
    for (i = 0 ; i < n && src[i] != '\0' ; i++)
        dest[dest_length + i] = src[i];
    dest[dest_length + i] = '\0';
    return dest;
}

const char* sys_errlist[] = {
#undef __ERROR
#define __ERROR(a, b) b,
    __ENUMERATE_ALL_ERRORS
#undef __ERROR
};
int sys_nerr = __errno_count;

char* strerror(int errnum)
{
    if (errnum >= __errno_count) {
        printf("strerror() missing string for errnum=%d\n", errnum);
        return const_cast<char*>("Unknown error");
    }
    return const_cast<char*>(sys_errlist[errnum]);
}

char* strsignal(int signum)
{
    if (signum >= __signal_count) {
        printf("strsignal() missing string for signum=%d\n", signum);
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
        if(strchr(accept, *s++))
            return (char*)--s;
    return nullptr;
}

}

