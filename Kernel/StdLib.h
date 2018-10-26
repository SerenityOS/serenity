#pragma once

#include "types.h"

#if 0
inline void memcpy(void *dest, const void *src, DWORD n)
{
    BYTE* bdest = (BYTE*)dest;
    const BYTE* bsrc = (const BYTE*)src;
    for (; n; --n)
        *(bdest++) = *(bsrc++);
}
#else
void memcpy(void*, const void*, DWORD);
#endif
void strcpy(char*, const char*);
int strcmp(char const*, const char*);
DWORD strlen(const char*);
void *memset(void*, BYTE, DWORD);
char *strdup(const char*);
int memcmp(const void*, const void*, size_t);
