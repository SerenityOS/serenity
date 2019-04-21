#include <sched.h>
#include <errno.h>
#include <Kernel/Syscall.h>

extern "C" {

int sched_yield()
{
    int rc = syscall(SC_yield);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

