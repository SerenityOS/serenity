#include <sys/stat.h>
#include <errno.h>
#include <Kernel/Syscall.h>

extern "C" {

mode_t umask(mode_t mask)
{
    return Syscall::invoke(Syscall::SC_umask, (dword)mask);
}

int mkdir(const char* pathname, mode_t mode)
{
    int rc = Syscall::invoke(Syscall::SC_mkdir, (dword)pathname, (dword)mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

