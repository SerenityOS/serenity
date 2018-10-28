#pragma once

#include "types.h"

extern "C" {

void* malloc(size_t);
void free(void*);
void* calloc(size_t nmemb, size_t);
void* realloc(void *ptr, size_t);

void exit(int status);
void abort();

}

