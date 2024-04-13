/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <bits/pthread_cancel.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <syscall.h>

extern "C" {

void* serenity_mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset, size_t alignment, char const* name)
{
    Syscall::SC_mmap_params params { addr, size, alignment, prot, flags, fd, offset, { name, name ? strlen(name) : 0 } };
    ptrdiff_t rc = syscall(SC_mmap, &params);
    if (rc < 0 && rc > -EMAXERRNO) {
        errno = -rc;
        return MAP_FAILED;
    }
    return (void*)rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mmap.html
void* mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset)
{
    return serenity_mmap(addr, size, prot, flags, fd, offset, PAGE_SIZE, nullptr);
}

void* mmap_with_name(void* addr, size_t size, int prot, int flags, int fd, off_t offset, char const* name)
{
    return serenity_mmap(addr, size, prot, flags, fd, offset, PAGE_SIZE, name);
}

void* mremap(void* old_address, size_t old_size, size_t new_size, int flags)
{
    Syscall::SC_mremap_params params { old_address, old_size, new_size, flags };
    ptrdiff_t rc = syscall(SC_mremap, &params);
    if (rc < 0 && rc > -EMAXERRNO) {
        errno = -rc;
        return MAP_FAILED;
    }
    return (void*)rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/munmap.html
int munmap(void* addr, size_t size)
{
    int rc = syscall(SC_munmap, addr, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mprotect.html
int mprotect(void* addr, size_t size, int prot)
{
    int rc = syscall(SC_mprotect, addr, size, prot);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_mmap_name(void* addr, size_t size, char const* name)
{
    if (!name) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_set_mmap_name_params params { addr, size, { name, strlen(name) } };
    int rc = syscall(SC_set_mmap_name, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int madvise(void* address, size_t size, int advice)
{
    int rc = syscall(SC_madvise, address, size, advice);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_madvise.html
int posix_madvise(void* address, size_t len, int advice)
{
    return madvise(address, len, advice);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mlock.html
int mlock(void const*, size_t)
{
    dbgln("FIXME: Implement mlock()");
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/munlock.html
int munlock(void const*, size_t)
{
    dbgln("FIXME: Implement munlock()");
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/msync.html
int msync(void* address, size_t size, int flags)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_msync, address, size, flags);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
