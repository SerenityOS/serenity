#pragma once

#include "types.h"

extern "C" {

void memcpy(void*, const void*, dword);
void strcpy(char*, const char*);
int strcmp(char const*, const char*);
dword strlen(const char*);
void *memset(void*, byte, dword);
char *strdup(const char*);
int memcmp(const void*, const void*, size_t);
char* strrchr(const char* str, int ch);

}
