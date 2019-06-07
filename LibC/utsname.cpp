#include <Kernel/Syscall.h>
#include <errno.h>
#include <sys/utsname.h>

extern "C" {

int uname(struct utsname* buf)
{
    int rc = syscall(SC_uname, buf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
