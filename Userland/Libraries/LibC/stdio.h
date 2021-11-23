/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define _STDIO_H // Make GMP believe we exist.

#include <Kernel/API/POSIX/stdio.h>
#include <bits/FILE.h>
#include <limits.h>
#include <stdarg.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#define FILENAME_MAX 1024
#define FOPEN_MAX 1024

__BEGIN_DECLS
#ifndef EOF
#    define EOF (-1)
#endif

#define _IOFBF 0
#define _IOLBF 1
#define _IONBF 2

#define L_tmpnam 256
#define P_tmpdir "/tmp"

extern FILE* stdin;
extern FILE* stdout;
extern FILE* stderr;

typedef off_t fpos_t;

int fseek(FILE*, long offset, int whence);
int fseeko(FILE*, off_t offset, int whence);
int fgetpos(FILE*, fpos_t*);
int fsetpos(FILE*, const fpos_t*);
long ftell(FILE*);
off_t ftello(FILE*);
char* fgets(char* buffer, int size, FILE*);
int fputc(int ch, FILE*);
int fileno(FILE*);
int fgetc(FILE*);
int fgetc_unlocked(FILE*);
int getc(FILE*);
int getc_unlocked(FILE* stream);
int getchar();
ssize_t getdelim(char**, size_t*, int, FILE*);
ssize_t getline(char**, size_t*, FILE*);
int ungetc(int c, FILE*);
int remove(const char* pathname);
FILE* fdopen(int fd, const char* mode);
FILE* fopen(const char* pathname, const char* mode);
FILE* freopen(const char* pathname, const char* mode, FILE*);
void flockfile(FILE* filehandle);
void funlockfile(FILE* filehandle);
int fclose(FILE*);
void rewind(FILE*);
void clearerr(FILE*);
int ferror(FILE*);
int feof(FILE*);
int fflush(FILE*);
size_t fread(void* ptr, size_t size, size_t nmemb, FILE*);
size_t fread_unlocked(void* ptr, size_t size, size_t nmemb, FILE*);
size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE*);
int vprintf(const char* fmt, va_list) __attribute__((format(printf, 1, 0)));
int vfprintf(FILE*, const char* fmt, va_list) __attribute__((format(printf, 2, 0)));
int vasprintf(char** strp, const char* fmt, va_list) __attribute__((format(printf, 2, 0)));
int vsprintf(char* buffer, const char* fmt, va_list) __attribute__((format(printf, 2, 0)));
int vsnprintf(char* buffer, size_t, const char* fmt, va_list) __attribute__((format(printf, 3, 0)));
int fprintf(FILE*, const char* fmt, ...) __attribute__((format(printf, 2, 3)));
int printf(const char* fmt, ...) __attribute__((format(printf, 1, 2)));
void dbgputstr(const char*, size_t);
int sprintf(char* buffer, const char* fmt, ...) __attribute__((format(printf, 2, 3)));
int asprintf(char** strp, const char* fmt, ...) __attribute__((format(printf, 2, 3)));
int snprintf(char* buffer, size_t, const char* fmt, ...) __attribute__((format(printf, 3, 4)));
int putchar(int ch);
int putc(int ch, FILE*);
int puts(const char*);
int fputs(const char*, FILE*);
void perror(const char*);
int scanf(const char* fmt, ...) __attribute__((format(scanf, 1, 2)));
int sscanf(const char* str, const char* fmt, ...) __attribute__((format(scanf, 2, 3)));
int fscanf(FILE*, const char* fmt, ...) __attribute__((format(scanf, 2, 3)));
int vscanf(const char*, va_list) __attribute__((format(scanf, 1, 0)));
int vfscanf(FILE*, const char*, va_list) __attribute__((format(scanf, 2, 0)));
int vsscanf(const char*, const char*, va_list) __attribute__((format(scanf, 2, 0)));
int setvbuf(FILE*, char* buf, int mode, size_t);
void setbuf(FILE*, char* buf);
void setlinebuf(FILE*);
int rename(const char* oldpath, const char* newpath);
FILE* tmpfile();
char* tmpnam(char*);
FILE* popen(const char* command, const char* type);
int pclose(FILE*);

__END_DECLS
