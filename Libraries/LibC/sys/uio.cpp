#include <Kernel/Syscall.h>
#include <errno.h>
#include <sys/uio.h>

extern "C" {

ssize_t writev(int fd, const struct iovec* iov, int iov_count)
{
    int rc = syscall(SC_writev, fd, iov, iov_count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
