/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/API/Syscall.h>
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
}
