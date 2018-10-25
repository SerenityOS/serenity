#include "mman.h"
#include "errno.h"
#include <Kernel/Syscall.h>

extern "C" {

void* mmap(void* addr, size_t size)
{
    int rc = Syscall::invoke(Syscall::PosixMmap, (dword)addr, (dword)size);
    __RETURN_WITH_ERRNO(rc, (void*)rc, (void*)-1);
}

int munmap(void* addr, size_t size)
{
    int rc = Syscall::invoke(Syscall::PosixMunmap, (dword)addr, (dword)size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
