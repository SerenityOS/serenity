#pragma once

#include "types.h"

extern "C" {

void memcpy(void*, const void*, DWORD);
void strcpy(char*, const char*);
int strcmp(char const*, const char*);
DWORD strlen(const char*);
void *memset(void*, BYTE, DWORD);
char *strdup(const char*);
int memcmp(const void*, const void*, size_t);
char* strrchr(const char* str, int ch);

}
