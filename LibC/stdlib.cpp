#include "stdlib.h"
#include "mman.h"

extern "C" {

void* malloc(size_t size)
{
    if (size > 4096) {
        volatile char* crashme = (char*)0xc007d00d;
        *crashme = 0;
    }
    void* ptr = mmap(nullptr, 4096);
    return ptr;
}

void free(void* ptr)
{
    if (!ptr)
        return;
    munmap(ptr, 4096);
}

}

