/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define _STDIO_H // Make GMP believe we exist.

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/stdio.h.html
#include <stddef.h>

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

#define L_ctermid 9
#define L_tmpnam 256
#define P_tmpdir "/tmp"

extern FILE* stdin;
extern FILE* stdout;
extern FILE* stderr;

typedef off_t fpos_t;

int fseek(FILE*, long offset, int whence);
int fseeko(FILE*, off_t offset, int whence);
int fgetpos(FILE*, fpos_t*);
int fsetpos(FILE*, fpos_t const*);
long ftell(FILE*);
off_t ftello(FILE*);
char* fgets(char* buffer, int size, FILE*);
int fputc(int ch, FILE*);
int fileno(FILE*);
int fgetc(FILE*);
int fgetc_unlocked(FILE*);
int getc(FILE*);
int getc_unlocked(FILE* stream);
int getchar(void);
ssize_t getdelim(char**, size_t*, int, FILE*);
ssize_t getline(char**, size_t*, FILE*);
int ungetc(int c, FILE*);
int remove(char const* pathname);
FILE* fdopen(int fd, char const* mode);
FILE* fopen(char const* pathname, char const* mode);
FILE* freopen(char const* pathname, char const* mode, FILE*);
FILE* fmemopen(void* buf, size_t size, char const* mode);
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
size_t fwrite(void const* ptr, size_t size, size_t nmemb, FILE*);
int vprintf(char const* fmt, va_list) __attribute__((format(printf, 1, 0)));
int vfprintf(FILE*, char const* fmt, va_list) __attribute__((format(printf, 2, 0)));
int vasprintf(char** strp, char const* fmt, va_list) __attribute__((format(printf, 2, 0)));
int vsprintf(char* buffer, char const* fmt, va_list) __attribute__((format(printf, 2, 0)));
int vsnprintf(char* buffer, size_t, char const* fmt, va_list) __attribute__((format(printf, 3, 0)));
int vdprintf(int, char const* fmt, va_list) __attribute__((format(printf, 2, 0)));
int dprintf(int, char const* fmt, ...) __attribute__((format(printf, 2, 3)));
int fprintf(FILE*, char const* fmt, ...) __attribute__((format(printf, 2, 3)));
int printf(char const* fmt, ...) __attribute__((format(printf, 1, 2)));
void dbgputstr(char const*, size_t);
int sprintf(char* buffer, char const* fmt, ...) __attribute__((format(printf, 2, 3)));
int asprintf(char** strp, char const* fmt, ...) __attribute__((format(printf, 2, 3)));
int snprintf(char* buffer, size_t, char const* fmt, ...) __attribute__((format(printf, 3, 4)));
int putchar(int ch);
int putc(int ch, FILE*);
int puts(char const*);
int fputs(char const*, FILE*);
void perror(char const*);
int scanf(char const* fmt, ...) __attribute__((format(scanf, 1, 2)));
int sscanf(char const* str, char const* fmt, ...) __attribute__((format(scanf, 2, 3)));
int fscanf(FILE*, char const* fmt, ...) __attribute__((format(scanf, 2, 3)));
int vscanf(char const*, va_list) __attribute__((format(scanf, 1, 0)));
int vfscanf(FILE*, char const*, va_list) __attribute__((format(scanf, 2, 0)));
int vsscanf(char const*, char const*, va_list) __attribute__((format(scanf, 2, 0)));
int setvbuf(FILE*, char* buf, int mode, size_t);
void setbuf(FILE*, char* buf);
void setlinebuf(FILE*);
int rename(char const* oldpath, char const* newpath);
int renameat(int olddirfd, char const* oldpath, int newdirfd, char const* newpath);
FILE* tmpfile(void);
char* tmpnam(char*);
FILE* popen(char const* command, char const* type);
int pclose(FILE*);
char* ctermid(char* s);

__END_DECLS
