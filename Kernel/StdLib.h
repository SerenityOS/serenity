#pragma once

#include "types.h"

void memcpy(void *, const void *, DWORD);
void strcpy(char *, const char *);
int strcmp(char const *, const char *);
DWORD strlen(const char *) PURE;
void *memset(void *, BYTE, DWORD);
char *strdup(const char *);
