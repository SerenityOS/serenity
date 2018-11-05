#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#ifndef EOF
#define EOF (-1)
#endif

#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2

struct __STDIO_FILE {
    int fd;
    int eof;
};

typedef struct __STDIO_FILE FILE;

extern FILE* stdin;
extern FILE* stdout;
extern FILE* stderr;

char* fgets(char* buffer, int size, FILE*);
int fileno(FILE*);
int fgetc(FILE*);
int getc(FILE*);
int getchar();
FILE* fopen(const char* pathname, const char* mode);
int fclose(FILE*);
void rewind(FILE*);
void clearerr(FILE*);
int feof(FILE*);
int fflush(FILE*);
size_t fread(void* ptr, size_t size, size_t nmemb, FILE*);
size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE*);
int fprintf(FILE*, const char* fmt, ...);
int printf(const char* fmt, ...);
int sprintf(char* buffer, const char* fmt, ...);
int putchar(int ch);
void perror(const char*);
int sscanf (const char* buf, const char* fmt, ...);
int fscanf(FILE*, const char* fmt, ...);

__END_DECLS

