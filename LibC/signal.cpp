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

sighandler_t signal(int signum, sighandler_t handler)
{
    sighandler_t old_handler = (sighandler_t)Syscall::invoke(Syscall::PosixSignal, (dword)signum, (dword)handler);
    if (old_handler == SIG_ERR) {
        errno = EINVAL;
        return SIG_ERR;
    }
    errno = 0;
    return old_handler;
}

int sigaction(int signum, const struct sigaction* act, struct sigaction* old_act)
{
    int rc = Syscall::invoke(Syscall::Sigaction, (dword)signum, (dword)act, (dword)old_act);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

