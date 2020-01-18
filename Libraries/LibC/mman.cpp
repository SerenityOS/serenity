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

#include <Kernel/Syscall.h>
#include <errno.h>
#include <mman.h>
#include <stdio.h>
#include <string.h>

extern "C" {

void* mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset)
{
    return mmap_with_name(addr, size, prot, flags, fd, offset, nullptr);
}

void* mmap_with_name(void* addr, size_t size, int prot, int flags, int fd, off_t offset, const char* name)
{
    Syscall::SC_mmap_params params { (u32)addr, size, prot, flags, fd, offset, { name, name ? strlen(name) : 0 } };
    int rc = syscall(SC_mmap, &params);
    if (rc < 0 && -rc < EMAXERRNO) {
        errno = -rc;
        return MAP_FAILED;
    }
    return (void*)rc;
}

int munmap(void* addr, size_t size)
{
    int rc = syscall(SC_munmap, addr, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int mprotect(void* addr, size_t size, int prot)
{
    int rc = syscall(SC_mprotect, addr, size, prot);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_mmap_name(void* addr, size_t size, const char* name)
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
}
