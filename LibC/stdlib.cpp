#include "stdlib.h"
#include "mman.h"
#include "stdio.h"
#include <Kernel/Syscall.h>
#include <AK/Assertions.h>

extern "C" {

void* malloc(size_t size)
{
    if (size > 4096) {
        volatile char* crashme = (char*)0xc007d00d;
        *crashme = 0;
    }
    void* ptr = mmap(nullptr, 4096);
    if (ptr) {
        int rc = set_mmap_name(ptr, 4096, "malloc");
        if (rc < 0) {
            perror("set_mmap_name failed");
        }
    }
    return ptr;
}

void free(void* ptr)
{
    if (!ptr)
        return;
    munmap(ptr, 4096);
}

void* calloc(size_t nmemb, size_t)
{
    ASSERT_NOT_REACHED();
    return nullptr;
}

void* realloc(void *ptr, size_t)
{
    ASSERT_NOT_REACHED();
    return nullptr;
}

void exit(int status)
{
    Syscall::invoke(Syscall::PosixExit, (dword)status);
}

void abort()
{
    // FIXME: Implement proper abort().
    exit(253);
}

}

