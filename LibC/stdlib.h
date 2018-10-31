#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

void* malloc(size_t);
void free(void*);
void* calloc(size_t nmemb, size_t);
void* realloc(void *ptr, size_t);
char* getenv(const char* name);
int atoi(const char*);

void exit(int status);
void abort();

__END_DECLS

