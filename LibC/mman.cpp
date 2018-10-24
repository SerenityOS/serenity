#include "mman.h"
#include <Kernel/Syscall.h>

extern "C" {

void* mmap(void* addr, size_t size)
{
    return (void*)Syscall::invoke(Syscall::PosixMmap, (dword)addr, (dword)size);
}

int munmap(void* addr, size_t size)
{
    return Syscall::invoke(Syscall::PosixMunmap, (dword)addr, (dword)size);
}

}
