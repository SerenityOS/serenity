#include <mman.h>
#include <errno.h>
#include <stdio.h>
#include <Kernel/Syscall.h>

extern "C" {

void* mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset)
{
    Syscall::SC_mmap_params params { (dword)addr, size, prot, flags, fd, offset };
    int rc = Syscall::invoke(Syscall::SC_mmap, (dword)&params);
    __RETURN_WITH_ERRNO(rc, (void*)rc, (void*)-1);
}

int munmap(void* addr, size_t size)
{
    int rc = Syscall::invoke(Syscall::SC_munmap, (dword)addr, (dword)size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_mmap_name(void* addr, size_t size, const char* name)
{
    int rc = Syscall::invoke(Syscall::SC_set_mmap_name, (dword)addr, (dword)size, (dword)name);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
