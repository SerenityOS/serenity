#include <sys/utsname.h>
#include <errno.h>
#include <Kernel/Syscall.h>

extern "C" {

int uname(struct utsname* buf)
{
    int rc = syscall(SC_uname, buf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

