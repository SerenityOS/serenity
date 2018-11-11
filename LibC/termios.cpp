#include <errno.h>
#include <termios.h>
#include <Kernel/Syscall.h>

extern "C" {

int tcgetattr(int fd, struct termios* t)
{
    int rc = Syscall::invoke(Syscall::SC_tcgetattr, (dword)fd, (dword)t);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int tcsetattr(int fd, int optional_actions, const struct termios* t)
{
    int rc = Syscall::invoke(Syscall::SC_tcsetattr, (dword)fd, (dword)optional_actions, (dword)t);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

