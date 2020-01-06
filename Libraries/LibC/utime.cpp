#include <Kernel/Syscall.h>
#include <errno.h>
#include <string.h>
#include <utime.h>

extern "C" {

int utime(const char* pathname, const struct utimbuf* buf)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_utime, pathname, strlen(pathname), buf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
