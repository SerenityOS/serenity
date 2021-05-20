/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

int tcflow([[maybe_unused]] int fd, [[maybe_unused]] int action)
{
    errno = EINVAL;
    return -1;
}

int tcflush(int fd, int queue_selector)
{
    return ioctl(fd, TCFLSH, queue_selector);
}

speed_t cfgetispeed(const struct termios* tp)
{
    return tp->c_ispeed;
}

speed_t cfgetospeed(const struct termios* tp)
{
    return tp->c_ospeed;
}

// We do not have to check for validity in the cfset{i,o}speed functions.
// POSIX says it's okay if we only fail after calling tcsetattr.
int cfsetispeed(struct termios* tp, speed_t speed)
{
    tp->c_ispeed = speed;
    errno = 0;
    return 0;
}

int cfsetospeed(struct termios* tp, speed_t speed)
{
    tp->c_ospeed = speed;
    errno = 0;
    return 0;
}

void cfmakeraw(struct termios* tp)
{
    if (!tp)
        return;

    auto& termios = *tp;
    termios.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
    termios.c_lflag &= ~OPOST;
    termios.c_cflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
    termios.c_cflag |= CS8;
}
}
