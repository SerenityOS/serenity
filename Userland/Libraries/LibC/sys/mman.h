/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/mman.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

void* mmap(void* addr, size_t, int prot, int flags, int fd, off_t);
void* mmap_with_name(void* addr, size_t, int prot, int flags, int fd, off_t, char const* name);
void* serenity_mmap(void* addr, size_t, int prot, int flags, int fd, off_t, size_t alignment, char const* name);
void* mremap(void* old_address, size_t old_size, size_t new_size, int flags);
int munmap(void*, size_t);
int mprotect(void*, size_t, int prot);
int set_mmap_name(void*, size_t, char const*);
int madvise(void*, size_t, int advice);
int posix_madvise(void*, size_t, int advice);
int mlock(void const*, size_t);
int munlock(void const*, size_t);
int msync(void*, size_t, int flags);

__END_DECLS
