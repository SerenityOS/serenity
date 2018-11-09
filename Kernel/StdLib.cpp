#include "types.h"
#include "Assertions.h"
#include "kmalloc.h"
#include <AK/Types.h>

extern "C" {

void memcpy(void *dest, const void *src, DWORD n)
{
    BYTE* bdest = (BYTE*)dest;
    const BYTE* bsrc = (const BYTE*)src;
    for (; n; --n)
        *(bdest++) = *(bsrc++);
}

void strcpy(char* dest, const char *src)
{
    while ((*dest++ = *src++) != '\0');
}

void* memset(void* dest, BYTE c, DWORD n)
{
    BYTE *bdest = (BYTE *)dest;
    for (; n; --n)
        *(bdest++) = c;
    return dest;
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
    auto* s1 = (const byte*)v1;
    auto* s2 = (const byte*)v2;
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

void __cxa_pure_virtual() NORETURN;
void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

}
