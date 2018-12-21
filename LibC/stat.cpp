#include <sys/stat.h>
#include <errno.h>
#include <Kernel/Syscall.h>

extern "C" {

mode_t umask(mode_t mask)
{
    return syscall(SC_umask, mask);
}

int mkdir(const char* pathname, mode_t mode)
{
    int rc = syscall(SC_mkdir, pathname, mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

