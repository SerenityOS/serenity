#include <Kernel/Syscall.h>
#include <errno.h>
#include <mman.h>
#include <stdio.h>

extern "C" {

void* mmap(void* addr, size_t size, int prot, int flags, int fd, off_t offset)
{
    Syscall::SC_mmap_params params { (u32)addr, size, prot, flags, fd, offset, nullptr };
    int rc = syscall(SC_mmap, &params);
    if (rc < 0 && -rc < EMAXERRNO) {
        errno = -rc;
        return (void*)-1;
    }
    return (void*)rc;
}

void* mmap_with_name(void* addr, size_t size, int prot, int flags, int fd, off_t offset, const char* name)
{
    Syscall::SC_mmap_params params { (u32)addr, size, prot, flags, fd, offset, name };
    int rc = syscall(SC_mmap, &params);
    if (rc < 0 && -rc < EMAXERRNO) {
        errno = -rc;
        return (void*)-1;
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
    int rc = syscall(SC_set_mmap_name, addr, size, name);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int shm_open(const char* name, int flags, mode_t mode)
{
    int rc = syscall(SC_shm_open, name, flags, mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int shm_unlink(const char* name)
{
    int rc = syscall(SC_shm_unlink, name);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
