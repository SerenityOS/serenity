#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <Kernel/Syscall.h>

extern "C" {

int kill(pid_t pid, int sig)
{
    int rc = Syscall::invoke(Syscall::SC_kill, (dword)pid, (dword)sig);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

sighandler_t signal(int signum, sighandler_t handler)
{
    sighandler_t old_handler = (sighandler_t)Syscall::invoke(Syscall::SC_signal, (dword)signum, (dword)handler);
    if (old_handler == SIG_ERR) {
        errno = EINVAL;
        return SIG_ERR;
    }
    errno = 0;
    return old_handler;
}

int sigaction(int signum, const struct sigaction* act, struct sigaction* old_act)
{
    int rc = Syscall::invoke(Syscall::SC_sigaction, (dword)signum, (dword)act, (dword)old_act);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int sigemptyset(sigset_t* set)
{
    *set = 0;
    return 0;
}

int sigfillset(sigset_t* set)
{
    *set = 0xffffffff;
    return 0;
}

int sigaddset(sigset_t* set, int sig)
{
    if (sig < 1 || sig > 32) {
        errno = EINVAL;
        return -1;
    }
    *set |= 1 << (sig);
    return 0;
}

int sigdelset(sigset_t* set, int sig)
{
    if (sig < 1 || sig > 32) {
        errno = EINVAL;
        return -1;
    }
    *set &= ~(1 << (sig));
    return 0;
}

int sigismember(const sigset_t* set, int sig)
{
    if (sig < 1 || sig > 32) {
        errno = EINVAL;
        return -1;
    }
    if (*set & (1 << (sig)))
        return 1;
    return 0;
}

const char* sys_siglist[NSIG] = {
#undef __SIGNAL
#define __SIGNAL(a, b) b,
    __ENUMERATE_ALL_SIGNALS
#undef __SIGNAL
};


}
