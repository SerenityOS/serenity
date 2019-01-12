#include "types.h"
#include "Assertions.h"
#include "kmalloc.h"
#include <AK/Types.h>

extern "C" {

void memcpy(void *dest, const void *src, dword n)
{
    byte* bdest = (byte*)dest;
    const byte* bsrc = (const byte*)src;
    for (; n; --n)
        *(bdest++) = *(bsrc++);
}

void strcpy(char* dest, const char *src)
{
    while ((*dest++ = *src++) != '\0');
}

void* memset(void* dest_ptr, byte c, dword n)
{
    dword dest = (dword)dest_ptr;
    asm volatile(
        "cld\n"
        "rep stosb\n"
        : "=D" (dest), "=c" (n)
        : "0" (dest), "1" (n), "a" (c)
        : "cc", "memory"
    );
    return dest_ptr;
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

dword strlen(const char* str)
{
    dword len = 0;
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
    return *(const byte*)s1 < *(const byte*)s2 ? -1 : 1;
}

char* strdup(const char *str)
{
    dword len = strlen(str);
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
