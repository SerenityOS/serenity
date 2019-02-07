#pragma once

#include "types.h"

extern "C" {

void memcpy(void*, const void*, dword);
void strcpy(char*, const char*);
char* strncpy(char*, const char*, size_t);
int strcmp(char const*, const char*);
size_t strlen(const char*);
void *memset(void*, byte, dword);
char *strdup(const char*);
int memcmp(const void*, const void*, size_t);
char* strrchr(const char* str, int ch);

}
