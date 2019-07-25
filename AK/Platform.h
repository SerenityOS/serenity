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

