#include "types.h"
#include "Assertions.h"
#include "kmalloc.h"

void memcpy(void *dest, const void *src, DWORD n)
{
    BYTE* bdest = (BYTE*)dest;
    const BYTE* bsrc = (const BYTE*)src;
    for (; n; --n)
        *(bdest++) = *(bsrc++);
}

void strcpy(char* dest, const char *src)
{
    while (*src)
        *(dest++) = *(src++);
}

void* memset(void* dest, BYTE c, DWORD n)
{
    BYTE *bdest = (BYTE *)dest;
    for (; n; --n)
        *(bdest++) = c;
    return dest;
}

DWORD strlen(const char* str)
{
    DWORD len = 0;
    while (*(str++))
        ++len;
    return len;
}

int strcmp(const char *s1, const char *s2)
{
    for (; *s1 == *s2; ++s1, ++s2) {
        if (*s1 == 0)
            return 0;
    }
    return *(const BYTE*)s1 < *(const BYTE*)s2 ? -1 : 1;
}

char* strdup(const char *str)
{
    DWORD len = strlen(str);
    char *s = (char*)kmalloc(len);
    memcpy(s, str, len);
    return s;
}

int memcmp(const void* v1, const void* v2, size_t n)
{
    size_t m;
    const char* s1 = (const char*)v1;
    const char* s2 = (const char*)v2;
    for (m = 0; m < n && *s1 == *s2; ++s1, ++s2);
    return m == n ? 0 : -1;
}

extern "C" void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}
