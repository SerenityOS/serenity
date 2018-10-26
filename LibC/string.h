#pragma once

#include "types.h"

extern "C" {

size_t strlen(const char*);
int strcmp(const char*, const char*);
void memcpy(void*, const void*, size_t);
const char* strerror(int errnum);

}

