#pragma once

#include <stddef.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define EXIT_SUCCESS 0
#define EXIT_FAILURE 1
#define MB_CUR_MAX 1

__attribute__((malloc)) __attribute__((alloc_size(1))) void* malloc(size_t);
__attribute__((malloc)) __attribute__((alloc_size(1, 2))) void* calloc(size_t nmemb, size_t);
size_t malloc_size(void*);
void free(void*);
void* realloc(void* ptr, size_t);
char* getenv(const char* name);
int putenv(char*);
int unsetenv(const char*);
int atoi(const char*);
long atol(const char*);
long long atoll(const char*);
double strtod(const char*, char** endptr);
float strtof(const char*, char** endptr);
long strtol(const char*, char** endptr, int base);
unsigned long strtoul(const char*, char** endptr, int base);
void qsort(void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*));
void qsort_r(void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*, void*), void* arg);
int atexit(void (*function)());
__attribute__((noreturn)) void exit(int status);
__attribute__((noreturn)) void abort();
char* ptsname(int fd);
int ptsname_r(int fd, char* buffer, size_t);
int abs(int);
long labs(long);
double atof(const char*);
int system(const char* command);
char* mktemp(char*);
void* bsearch(const void* key, const void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*));

#define RAND_MAX 32767
int rand();
void srand(unsigned seed);

long int random();
void srandom(unsigned seed);

typedef struct {
    int quot;
    int rem;
} div_t;
div_t div(int, int);
typedef struct {
    long quot;
    long rem;
} ldiv_t;
ldiv_t ldiv(long, long);

__END_DECLS
