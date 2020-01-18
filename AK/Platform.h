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

#ifdef __i386__
#define AK_ARCH_I386 1
#endif

#ifdef __x86_64__
#define AK_ARCH_X86_64 1
#endif

#define ARCH(arch) (defined(AK_ARCH_##arch) && AK_ARCH_##arch)

#ifdef __clang__
#    define CONSUMABLE(initial_state) __attribute__((consumable(initial_state)))
#    define CALLABLE_WHEN(...) __attribute__((callable_when(__VA_ARGS__)))
#    define SET_TYPESTATE(state) __attribute__((set_typestate(state)))
#    define RETURN_TYPESTATE(state) __attribute__((return_typestate(state)))
#else
#    define CONSUMABLE(initial_state)
#    define CALLABLE_WHEN(...)
#    define SET_TYPESTATE(state)
#    define RETURN_TYPESTATE(state)
#endif

#ifndef __serenity__
#define PAGE_SIZE sysconf(_SC_PAGESIZE)

#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
inline int open_with_path_length(const char* path, size_t path_length, int options, mode_t mode)
{
    auto* tmp = (char*)malloc(path_length + 1);
    memcpy(tmp, path, path_length);
    tmp[path_length] = '\0';
    int fd = open(tmp, options, mode);
    int saved_errno = errno;
    free(tmp);
    errno = saved_errno;
    return fd;
}
#endif

