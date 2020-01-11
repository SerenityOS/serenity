#include <Kernel/Syscall.h>
#include <errno.h>
#include <mman.h>
#include <stdio.h>
#include <string.h>

extern "C" {

void* mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset)
{
    return mmap_with_name(addr, size, prot, flags, fd, offset, nullptr);
}

void* mmap_with_name(void* addr, size_t size, int prot, int flags, int fd, off_t offset, const char* name)
{
    Syscall::SC_mmap_params params { (u32)addr, size, prot, flags, fd, offset, { name, name ? strlen(name) : 0 } };
    int rc = syscall(SC_mmap, &params);
    if (rc < 0 && -rc < EMAXERRNO) {
        errno = -rc;
        return MAP_FAILED;
    }
    return (void*)rc;
}

int munmap(void* addr, size_t size)
{
    int rc = syscall(SC_munmap, addr, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int mprotect(void* addr, size_t size, int prot)
{
    int rc = syscall(SC_mprotect, addr, size, prot);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_mmap_name(void* addr, size_t size, const char* name)
{
    if (!name) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_set_mmap_name_params params { addr, size, { name, strlen(name) } };
    int rc = syscall(SC_set_mmap_name, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int madvise(void* address, size_t size, int advice)
{
    int rc = syscall(SC_madvise, address, size, advice);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
