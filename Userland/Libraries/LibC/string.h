/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

// A few C Standard Libraries include this header in <string.h>, and hence expect
// `strcasecmp` etcetera to be available as part of a <string.h> include, so let's
// do the same here to maintain compatibility
#include <strings.h>

size_t strlen(const char*);
size_t strnlen(const char*, size_t maxlen);

int strcmp(const char*, const char*);
int strncmp(const char*, const char*, size_t);

int memcmp(const void*, const void*, size_t);
void* memcpy(void*, const void*, size_t);
void* memmove(void*, const void*, size_t);
void* memchr(const void*, int c, size_t);
const void* memmem(const void* haystack, size_t, const void* needle, size_t);

void* memset(void*, int, size_t);
void explicit_bzero(void*, size_t) __attribute__((nonnull(1)));

__attribute__((malloc)) char* strdup(const char*);
__attribute__((malloc)) char* strndup(const char*, size_t);

char* strcpy(char* dest, const char* src);
char* strncpy(char* dest, const char* src, size_t);
__attribute__((warn_unused_result)) size_t strlcpy(char* dest, const char* src, size_t);

char* strchr(const char*, int c);
char* strchrnul(const char*, int c);
char* strstr(const char* haystack, const char* needle);
char* strrchr(const char*, int c);

char* index(const char* str, int ch);
char* rindex(const char* str, int ch);

char* strcat(char* dest, const char* src);
char* strncat(char* dest, const char* src, size_t);

size_t strspn(const char*, const char* accept);
size_t strcspn(const char*, const char* reject);
int strerror_r(int, char*, size_t);
char* strerror(int errnum);
char* strsignal(int signum);
char* strpbrk(const char*, const char* accept);
char* strtok_r(char* str, const char* delim, char** saved_str);
char* strtok(char* str, const char* delim);
int strcoll(const char* s1, const char* s2);
size_t strxfrm(char* dest, const char* src, size_t n);
char* strsep(char** str, char const* delim);

__END_DECLS
