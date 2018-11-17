#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define EXIT_SUCCESS 0
#define EXIT_FAILURE 1

void* malloc(size_t) __MALLOC;
void free(void*);
void* calloc(size_t nmemb, size_t);
void* realloc(void *ptr, size_t);
char* getenv(const char* name);
int atoi(const char*);
long atol(const char*);
void qsort(void* base, size_t nmemb, size_t size, int (*compar)(const void *, const void *));
void exit(int status) __NORETURN;
void abort() __NORETURN;

__END_DECLS

