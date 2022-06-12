/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <bits/pthread_cancel.h>
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

// https://pubs.opengroup.org/onlinepubs/009695399/functions/tcsendbreak.html
int tcsendbreak([[maybe_unused]] int fd, [[maybe_unused]] int duration)
{
    // FIXME: Implement this for real.
    return 0;
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

// https://pubs.opengroup.org/onlinepubs/009695399/functions/tcdrain.html
int tcdrain([[maybe_unused]] int fd)
{
    __pthread_maybe_cancel();

    // FIXME: Implement this for real.
    return 0;
}

speed_t cfgetispeed(const struct termios* tp)
{
    return tp->c_ispeed;
}

speed_t cfgetospeed(const struct termios* tp)
{
    return tp->c_ospeed;
}

static int baud_rate_from_speed(speed_t speed)
{
    int rate = -EINVAL;
    switch (speed) {
    case B0:
        rate = 0;
        break;
    case B50:
        rate = 50;
        break;
    case B75:
        rate = 75;
        break;
    case B110:
        rate = 110;
        break;
    case B134:
        rate = 134;
        break;
    case B150:
        rate = 150;
        break;
    case B200:
        rate = 200;
        break;
    case B300:
        rate = 300;
        break;
    case B600:
        rate = 600;
        break;
    case B1200:
        rate = 1200;
        break;
    case B1800:
        rate = 1800;
        break;
    case B2400:
        rate = 2400;
        break;
    case B4800:
        rate = 4800;
        break;
    case B9600:
        rate = 9600;
        break;
    case B19200:
        rate = 19200;
        break;
    case B38400:
        rate = 38400;
        break;
    }

    return rate;
}

int cfsetispeed(struct termios* tp, speed_t speed)
{
    auto ispeed = baud_rate_from_speed(speed);
    if (ispeed > 0) {
        tp->c_ispeed = ispeed;
    }
    __RETURN_WITH_ERRNO(ispeed, 0, -1);
}

int cfsetospeed(struct termios* tp, speed_t speed)
{
    auto ospeed = baud_rate_from_speed(speed);
    if (ospeed > 0) {
        tp->c_ispeed = ospeed;
    }
    __RETURN_WITH_ERRNO(ospeed, 0, -1);
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
