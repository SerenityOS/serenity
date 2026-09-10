/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/string.h.html
#include <stddef.h>

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

// Let libcxx and libstdc++ know that we provide the C++ version of strchr and friends.
// Note that libcxx is not consistent between string.h and wchar.h, and it does not
// require us to define _LIBCPP_STRING_H_HAS_CONST_OVERLOADS here.
#ifdef __cplusplus
#    define __CORRECT_ISO_CPP_STRING_H_PROTO
#endif

// A few C Standard Libraries include this header in <string.h>, and hence expect
// `strcasecmp` etcetera to be available as part of a <string.h> include, so let's
// do the same here to maintain compatibility
#include <strings.h>

#if defined(__cplusplus) && !defined(__STRING_FORCE_C_DECLARATION)
#    define __MAKE_C_OR_CPP_DECLARATION(type, name, ...)                    \
        extern "C++" type const* name(type const*, __VA_ARGS__) asm(#name); \
        extern "C++" type* name(type*, __VA_ARGS__) asm(#name)
#else
#    define __MAKE_C_OR_CPP_DECLARATION(type, name, ...) \
        type* name(type const*, __VA_ARGS__)
#endif

size_t strlen(char const*);
size_t strnlen(char const*, size_t maxlen);

int strcmp(char const*, char const*);
int strncmp(char const*, char const*, size_t);

int memcmp(void const*, void const*, size_t);
int timingsafe_memcmp(void const*, void const*, size_t);
void* memcpy(void*, void const*, size_t);
void* memccpy(void*, void const*, int, size_t);
void* memmove(void*, void const*, size_t);
__MAKE_C_OR_CPP_DECLARATION(void, memchr, int c, size_t n);
void* memmem(void const* haystack, size_t, void const* needle, size_t);

void* memset(void*, int, size_t);
void explicit_bzero(void*, size_t) __attribute__((nonnull(1)));

__attribute__((malloc)) char* strdup(char const*);
__attribute__((malloc)) char* strndup(char const*, size_t);

char* strcpy(char* dest, char const* src);
char* stpcpy(char* dest, char const* src);
char* strncpy(char* dest, char const* src, size_t);
__attribute__((warn_unused_result)) size_t strlcpy(char* dest, char const* src, size_t);

__MAKE_C_OR_CPP_DECLARATION(char, strchr, int c);
char* strchrnul(char const*, int c);
__MAKE_C_OR_CPP_DECLARATION(char, strstr, char const* needle);
char* strcasestr(char const* haystack, char const* needle);
__MAKE_C_OR_CPP_DECLARATION(char, strrchr, int c);

char* index(char const* str, int ch);
char* rindex(char const* str, int ch);

char* strcat(char* dest, char const* src);
char* strncat(char* dest, char const* src, size_t);

size_t strspn(char const*, char const* accept);
size_t strcspn(char const*, char const* reject);
int strerror_r(int, char*, size_t);
char* strerror(int errnum);
char* strsignal(int signum);
__MAKE_C_OR_CPP_DECLARATION(char, strpbrk, char const* accept);
char* strtok_r(char* str, char const* delim, char** saved_str);
char* strtok(char* str, char const* delim);
int strcoll(char const* s1, char const* s2);
size_t strxfrm(char* dest, char const* src, size_t n);
char* strsep(char** str, char const* delim);

__END_DECLS

#undef __MAKE_C_OR_CPP_DECLARATION
