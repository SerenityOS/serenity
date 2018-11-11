#include <string.h>
#include <errno.h>
#include <stdio.h>
#include <signal.h>
#include <assert.h>

extern "C" {

void bzero(void* dest, size_t n)
{
    memset(dest, 0, n);
}

void bcopy(const void* src, void* dest, size_t n)
{
    memmove(dest, src, n);
}

void* memset(void* dest, int c, size_t n)
{
    uint8_t* bdest = (uint8_t*)dest;
    for (; n; --n)
        *(bdest++) = c;
    return dest;
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

void memcpy(void* dest, const void* src, size_t n)
{
    auto* bdest = (unsigned char*)dest;
    auto* bsrc = (const unsigned char*)src;
    for (; n; --n)
        *(bdest++) = *(bsrc++);
}

void memmove(void* dest, const void* src, size_t n)
{
    if (dest < src)
        return memcpy(dest, src, n);
    // FIXME: Implement backwards copy.
    assert(false);
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
    size_t destLength = strlen(dest);
    size_t i;
    for (i = 0 ; src[i] != '\0' ; i++)
        dest[destLength + i] = src[i];
    dest[destLength + i] = '\0';
    return dest;
}

char* strncat(char *dest, const char *src, size_t n)
{
    size_t destLength = strlen(dest);
    size_t i;
    for (i = 0 ; i < n && src[i] != '\0' ; i++)
        dest[destLength + i] = src[i];
    dest[destLength + i] = '\0';
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

}
