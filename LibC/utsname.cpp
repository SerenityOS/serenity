#include "utsname.h"
#include "errno.h"
#include <Kernel/Syscall.h>

extern "C" {

int uname(struct utsname* buf)
{
    int rc = Syscall::invoke(Syscall::PosixUname, (dword)buf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

