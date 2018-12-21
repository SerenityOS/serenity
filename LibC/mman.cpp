#include <mman.h>
#include <errno.h>
#include <stdio.h>
#include <Kernel/Syscall.h>

extern "C" {

void* mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset)
{
    Syscall::SC_mmap_params params { (dword)addr, size, prot, flags, fd, offset };
    int rc = syscall(SC_mmap, &params);
    __RETURN_WITH_ERRNO(rc, (void*)rc, (void*)-1);
}

int munmap(void* addr, size_t size)
{
    int rc = syscall(SC_munmap, addr, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_mmap_name(void* addr, size_t size, const char* name)
{
    int rc = syscall(SC_set_mmap_name, addr, size, name);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
