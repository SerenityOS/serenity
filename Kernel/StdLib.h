#pragma once

#include <AK/Types.h>

extern "C" {

static_assert(sizeof(size_t) == 4);

void* memcpy(void*, const void*, size_t);
char* strcpy(char*, const char*);
char* strncpy(char*, const char*, size_t);
int strcmp(char const*, const char*);
size_t strlen(const char*);
void* memset(void*, int, size_t);
char *strdup(const char*);
int memcmp(const void*, const void*, size_t);
char* strrchr(const char* str, int ch);
void* memmove(void* dest, const void* src, size_t n);

inline word ntohs(word w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }
inline word htons(word w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }

}
