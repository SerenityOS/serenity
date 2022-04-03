/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/types.h>

__BEGIN_DECLS

ssize_t getxattr(char const* path, char const* name, void* value, size_t size);
ssize_t lgetxattr(char const* path, char const* name, void* value, size_t size);
ssize_t fgetxattr(int fd, char const* name, void* value, size_t size);

int setxattr(char const* path, char const* name, void const* value, size_t size, int flags);
int lsetxattr(char const* path, char const* name, void const* value, size_t size, int flags);
int fsetxattr(int fd, char const* name, void const* value, size_t size, int flags);

ssize_t listxattr(char const* path, char* list, size_t size);
ssize_t llistxattr(char const* path, char* list, size_t size);
ssize_t flistxattr(int fd, char* list, size_t size);

__END_DECLS
