#include <Kernel/Syscall.h>
#include <assert.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <termios.h>

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
    (void)fd;
    (void)action;
    ASSERT_NOT_REACHED();
}

int tcflush(int fd, int queue_selector)
{
    (void)fd;
    (void)queue_selector;
    ASSERT_NOT_REACHED();
}

speed_t cfgetispeed(const struct termios* tp)
{
    return tp->c_ispeed;
}

speed_t cfgetospeed(const struct termios* tp)
{
    return tp->c_ospeed;
}
}
