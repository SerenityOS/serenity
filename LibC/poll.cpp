#include <poll.h>
#include <Kernel/Syscall.h>
#include <errno.h>

extern "C" {

int poll(struct pollfd* fds, int nfds, int timeout)
{
    int rc = syscall(SC_poll, fds, nfds, timeout);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

