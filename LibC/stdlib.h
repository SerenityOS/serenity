#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define EXIT_SUCCESS 0
#define EXIT_FAILURE 1
#define MB_CUR_MAX 1

[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1)]] void* malloc(size_t);
[[gnu::malloc, gnu::returns_nonnull, gnu::alloc_size(1, 2)]] void* calloc(size_t nmemb, size_t);
void free(void*);
[[gnu::returns_nonnull]] void* realloc(void *ptr, size_t);
char* getenv(const char* name);
int atoi(const char*);
long atol(const char*);
void qsort(void* base, size_t nmemb, size_t size, int (*compar)(const void *, const void *));
[[noreturn]] void exit(int status);
[[noreturn]] void abort();
char* ptsname(int fd);
int ptsname_r(int fd, char* buffer, size_t);
int abs(int);
int system(const char* command);
char* mktemp(char*);
void* bsearch(const void* key, const void* base, size_t nmemb, size_t size, int (*compar)(const void *, const void *));

#define RAND_MAX 32767
int rand();
void srand(unsigned seed);

long int random();
void srandom(unsigned seed);

typedef struct { int quot; int rem; } div_t;
div_t div(int, int);
typedef struct { long quot; long rem; } ldiv_t;
ldiv_t ldiv(long, long);

__END_DECLS

