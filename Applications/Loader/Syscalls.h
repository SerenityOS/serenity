#pragma once

#include <AK/Types.h>
#include <Kernel/Syscall.h>
#include <Kernel/UnixTypes.h>
#include <LibC/bits/stdint.h>
#include <stdarg.h>

typedef uint16_t mode_t;

extern "C" {
void exit(int code);
void dbgputstr(const char* str, int len);
void dbgputc(char c);
int open(const char* path, int options, ...);
int sleep(unsigned seconds);
void* serenity_mmap(void* addr, size_t size, int prot,
    int flags, int fd, off_t offset,
    size_t alignment, const char* name);
int fstat(int fd, struct stat* statbuf);
int close(int fd);
int munmap(void* addr, size_t size);
int mprotect(void*, size_t, int prot);
}
