#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <Kernel/Syscall.h>

extern "C" {

int kill(pid_t pid, int sig)
{
    int rc = Syscall::invoke(Syscall::PosixKill, (dword)pid, (dword)sig);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

