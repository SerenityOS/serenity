/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#define _STDIO_H // Make GMP believe we exist.

#include <limits.h>
#include <stdarg.h>
#include <bits/FILE.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#define FILENAME_MAX 1024

__BEGIN_DECLS
#ifndef EOF
#    define EOF (-1)
#endif

#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2

#define _IOFBF 0
#define _IOLBF 1
#define _IONBF 2

#define L_tmpnam 256

extern FILE* stdin;
extern FILE* stdout;
extern FILE* stderr;

typedef long fpos_t;

int fseek(FILE*, long offset, int whence);
int fgetpos(FILE*, fpos_t*);
int fsetpos(FILE*, const fpos_t*);
long ftell(FILE*);
char* fgets(char* buffer, int size, FILE*);
int fputc(int ch, FILE*);
int fileno(FILE*);
int fgetc(FILE*);
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
size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE*);
int vprintf(const char* fmt, va_list);
int vfprintf(FILE*, const char* fmt, va_list);
int vsprintf(char* buffer, const char* fmt, va_list);
int vsnprintf(char* buffer, size_t, const char* fmt, va_list);
int fprintf(FILE*, const char* fmt, ...);
int printf(const char* fmt, ...);
int dbgprintf(const char* fmt, ...);
void dbgputch(char);
int dbgputstr(const char*, ssize_t);
int sprintf(char* buffer, const char* fmt, ...);
int snprintf(char* buffer, size_t, const char* fmt, ...);
int putchar(int ch);
int putc(int ch, FILE*);
int puts(const char*);
int fputs(const char*, FILE*);
void perror(const char*);
int scanf(const char* fmt, ...);
int sscanf(const char* str, const char* fmt, ...);
int fscanf(FILE*, const char* fmt, ...);
int vfscanf(FILE*, const char*, va_list);
int vsscanf(const char*, const char*, va_list);
int setvbuf(FILE*, char* buf, int mode, size_t);
void setbuf(FILE*, char* buf);
void setlinebuf(FILE*);
int rename(const char* oldpath, const char* newpath);
FILE* tmpfile();
char* tmpnam(char*);
FILE* popen(const char* command, const char* type);
int pclose(FILE*);

__END_DECLS
