#include <Kernel/Syscall.h>
#include <errno.h>
#include <stdio.h>
#include <sys/select.h>

extern "C" {

int select(int nfds, fd_set* readfds, fd_set* writefds, fd_set* exceptfds, struct timeval* timeout)
{
    Syscall::SC_select_params params { nfds, readfds, writefds, exceptfds, timeout };
    int rc = syscall(SC_select, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
