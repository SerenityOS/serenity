/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

extern "C" {
void* memcpy(void*, const void*, size_t);
[[nodiscard]] int strncmp(const char* s1, const char* s2, size_t n);
[[nodiscard]] char* strstr(const char* haystack, const char* needle);
[[nodiscard]] int strcmp(char const*, const char*);
[[nodiscard]] size_t strlen(const char*);
[[nodiscard]] size_t strnlen(const char*, size_t);
void* memset(void*, int, size_t);
[[nodiscard]] int memcmp(const void*, const void*, size_t);
void* memmove(void* dest, const void* src, size_t n);
const void* memmem(const void* haystack, size_t, const void* needle, size_t);
}
