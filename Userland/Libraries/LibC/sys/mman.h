/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/mman.h>

__BEGIN_DECLS

void* mmap(void* addr, size_t, int prot, int flags, int fd, off_t);
void* mmap_with_name(void* addr, size_t, int prot, int flags, int fd, off_t, const char* name);
void* serenity_mmap(void* addr, size_t, int prot, int flags, int fd, off_t, size_t alignment, const char* name);
void* mremap(void* old_address, size_t old_size, size_t new_size, int flags);
int munmap(void*, size_t);
int mprotect(void*, size_t, int prot);
int set_mmap_name(void*, size_t, const char*);
int madvise(void*, size_t, int advice);
int posix_madvise(void*, size_t, int advice);
void* allocate_tls(const char* initial_data, size_t);
int mlock(const void*, size_t);
int munlock(const void*, size_t);
int msync(void*, size_t, int flags);

__END_DECLS
