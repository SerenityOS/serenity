#include <assert.h>
#include <errno.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <Kernel/Syscall.h>

extern "C" {

int tcgetattr(int fd, struct termios* t)
{
    return ioctl(fd, TCGETS, t);
}

int tcsetattr(int fd, int optional_actions, const struct termios* t)
{
    switch (optional_actions) {
    case TCSANOW:
        return ioctl(fd, TCSETS, t);
    case TCSADRAIN:
        return ioctl(fd, TCSETSW, t);
    case TCSAFLUSH:
        return ioctl(fd, TCSETSF, t);
    }
    errno = EINVAL;
    return -1;
}

int tcflow(int fd, int action)
{
    (void) fd;
    (void) action;
    assert(false);
}

}

