#pragma once

#include <AK/Types.h>

extern "C" {

static_assert(sizeof(size_t) == 4);

void* memcpy(void*, const void*, size_t);
char* strcpy(char*, const char*);
char* strncpy(char*, const char*, size_t);
int strcmp(char const*, const char*);
size_t strlen(const char*);
size_t strnlen(const char*, size_t);
void* memset(void*, int, size_t);
char* strdup(const char*);
int memcmp(const void*, const void*, size_t);
char* strrchr(const char* str, int ch);
void* memmove(void* dest, const void* src, size_t n);

inline u16 ntohs(u16 w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }
inline u16 htons(u16 w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }
}
