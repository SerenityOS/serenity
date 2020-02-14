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

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

size_t strlen(const char*);
size_t strnlen(const char*, size_t maxlen);
int strcmp(const char*, const char*);
int strncmp(const char*, const char*, size_t);
int strcasecmp(const char*, const char*);
int strncasecmp(const char*, const char*, size_t);
int memcmp(const void*, const void*, size_t);
void* memcpy(void*, const void*, size_t);
void* memmove(void*, const void*, size_t);
void* memchr(const void*, int c, size_t);
void bzero(void*, size_t);
void bcopy(const void*, void*, size_t);
void* memset(void*, int, size_t);
char* strdup(const char*);
char* strndup(const char*, size_t);
char* strcpy(char* dest, const char* src);
char* strncpy(char* dest, const char* src, size_t);
char* strchr(const char*, int c);
char* strchrnul(const char*, int c);
char* strstr(const char* haystack, const char* needle);
char* strrchr(const char*, int c);
char* strcat(char* dest, const char* src);
char* strncat(char* dest, const char* src, size_t);
size_t strspn(const char*, const char* accept);
size_t strcspn(const char*, const char* reject);
char* strerror(int errnum);
char* strsignal(int signum);
char* strpbrk(const char*, const char* accept);
char* strtok_r(char* str, const char* delim, char** saved_str);
char* strtok(char* str, const char* delim);
int strcoll(const char* s1, const char* s2);
size_t strxfrm(char* dest, const char* src, size_t n);

__END_DECLS
