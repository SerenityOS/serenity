#include <Kernel/Syscall.h>
#include <assert.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

extern "C" {

mode_t umask(mode_t mask)
{
    return syscall(SC_umask, mask);
}

int mkdir(const char* pathname, mode_t mode)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_mkdir, pathname, strlen(pathname), mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int chmod(const char* pathname, mode_t mode)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_chmod, pathname, strlen(pathname), mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fchmod(int fd, mode_t mode)
{
    int rc = syscall(SC_fchmod, fd, mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
