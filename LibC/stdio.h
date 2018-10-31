#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#ifndef EOF
#define EOF (-1)
#endif

struct __STDIO_FILE {
    int fd;
};

typedef struct __STDIO_FILE FILE;

extern FILE* stdin;
extern FILE* stdout;
extern FILE* stderr;

int fprintf(FILE*, const char* fmt, ...);
int printf(const char* fmt, ...);
int sprintf(char* buffer, const char* fmt, ...);
int putchar(int ch);
void perror(const char*);

__END_DECLS

