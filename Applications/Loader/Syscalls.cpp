#include "Syscalls.h"
#include "AK/Types.h"
#include "Utils.h"
#include <LibC/errno_numbers.h>

inline u32 invoke(unsigned int function)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function)
                 : "memory");
    return result;
}

template<typename T1>
inline u32 invoke(unsigned int function, T1 arg1)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)arg1)
                 : "memory");
    return result;
}

template<typename T1, typename T2>
inline u32 invoke(unsigned int function, T1 arg1, T2 arg2)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)arg1), "c"((u32)arg2)
                 : "memory");
    return result;
}

template<typename T1, typename T2, typename T3>
inline u32 invoke(unsigned int function, T1 arg1, T2 arg2, T3 arg3)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)arg1), "c"((u32)arg2), "b"((u32)arg3)
                 : "memory");
    return result;
}

extern "C" {

[[noreturn]] void exit(int code)
{
    constexpr unsigned int function = Kernel::SC_exit;
    unsigned int result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"(code)
                 : "memory");
    while (1) {
    }
}

void dbgputstr(const char* str, int len)
{
    constexpr unsigned int function = Kernel::SC_dbgputch;
    for (int i = 0; i < len; ++i) {
        unsigned int result;
        asm volatile("int $0x82"
                     : "=a"(result)
                     : "a"(function), "d"((u32)str[i])
                     : "memory");
    }
}

void dbgputc(char c)
{
    constexpr unsigned int function = Kernel::SC_dbgputch;
    unsigned int result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"(c)
                 : "memory");
}
int openat_with_path_length(int dirfd, const char* path, size_t path_length, int options, mode_t mode)
{
    if (!path) {
        return -1;
    }
    if (path_length > INT32_MAX) {
        return -1;
    }
    Syscall::SC_open_params params { dirfd, { path, path_length }, options, mode };
    unsigned int function = SC_open;
    int result = 0;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)&params)
                 : "memory");
    return result;
}

int open(const char* path, int options, ...)
{
    if (!path) {
        return -1;
    }
    va_list ap;
    va_start(ap, options);
    auto mode = (mode_t)va_arg(ap, unsigned);
    va_end(ap);
    if (!path) {
        return -1;
    }
    uint32_t path_length = strlen(path);
    if (path_length > INT32_MAX) {
        return -1;
    }
    Syscall::SC_open_params params { AT_FDCWD, { path, path_length }, options, mode };
    unsigned int function = SC_open;
    int result = 0;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)&params)
                 : "memory");
    return result;
}

int sleep(unsigned seconds)
{
    unsigned int function = SC_sleep;
    int result = 0;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"(seconds)
                 : "memory");
    return result;
}
}

void* serenity_mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset, size_t alignment, const char* name)
{
    Syscall::SC_mmap_params params { (u32)addr, size, alignment, prot, flags, fd, offset, { name, name ? strlen(name) : 0 } };
    int rc = syscall(SC_mmap, &params);
    if (rc < 0 && -rc < EMAXERRNO) {
        return nullptr;
    }
    return (void*)rc;
}

int fstat(int fd, struct stat* statbuf)
{
    int rc = syscall(SC_fstat, fd, statbuf);
    return rc;
}

int close(int fd)
{
    return syscall(SC_close, fd);
}

int munmap(void* addr, size_t size)
{
    return syscall(SC_munmap, addr, size);
}

int mprotect(void* address, size_t size, int prot)
{
    return syscall(SC_mprotect, address, size, prot);
}
