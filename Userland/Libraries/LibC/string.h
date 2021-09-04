/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

size_t strlen(char const*);
size_t strnlen(char const*, size_t maxlen);

int strcmp(char const*, char const*);
int strncmp(char const*, char const*, size_t);

int memcmp(void const*, void const*, size_t);
void* memcpy(void*, void const*, size_t);
void* memmove(void*, void const*, size_t);
void* memchr(void const*, int c, size_t);
void const* memmem(void const* haystack, size_t, void const* needle, size_t);

void* memset(void*, int, size_t);
void explicit_bzero(void*, size_t) __attribute__((nonnull(1)));

__attribute__((malloc)) char* strdup(char const*);
__attribute__((malloc)) char* strndup(char const*, size_t);

__attribute__((deprecated("use strlcpy or String::copy_characters_to_buffer"))) char* strcpy(char* dest, char const* src);
__attribute__((deprecated("use strlcpy or String::copy_characters_to_buffer"))) char* strncpy(char* dest, char const* src, size_t);
__attribute__((warn_unused_result)) size_t strlcpy(char* dest, char const* src, size_t);

char* strchr(char const*, int c);
char* strchrnul(char const*, int c);
char* strstr(char const* haystack, char const* needle);
char* strrchr(char const*, int c);

__attribute__((deprecated("use strncat"))) char* strcat(char* dest, char const* src);
char* strncat(char* dest, char const* src, size_t);

size_t strspn(char const*, char const* accept);
size_t strcspn(char const*, char const* reject);
int strerror_r(int, char*, size_t);
char* strerror(int errnum);
char* strsignal(int signum);
char* strpbrk(char const*, char const* accept);
char* strtok_r(char* str, char const* delim, char** saved_str);
char* strtok(char* str, char const* delim);
int strcoll(char const* s1, char const* s2);
size_t strxfrm(char* dest, char const* src, size_t n);

__END_DECLS
