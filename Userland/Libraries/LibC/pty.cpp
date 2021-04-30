/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <fcntl.h>
#include <pty.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

int openpty(int* amaster, int* aslave, char* name, const struct termios* termp, const struct winsize* winp)
{
    *amaster = posix_openpt(O_RDWR);
    if (*amaster < 0) {
        return -1;
    }
    if (grantpt(*amaster) < 0) {
        int error = errno;
        close(*amaster);
        errno = error;
        return -1;
    }
    if (unlockpt(*amaster) < 0) {
        int error = errno;
        close(*amaster);
        errno = error;
        return -1;
    }

    const char* tty_name = ptsname(*amaster);
    if (!tty_name) {
        int error = errno;
        close(*amaster);
        errno = error;
        return -1;
    }

    if (name) {
        /* The spec doesn't say how large name has to be. Good luck. */
        [[maybe_unused]] auto rc = strlcpy(name, tty_name, 128);
    }

    *aslave = open(tty_name, O_RDWR | O_NOCTTY);
    if (*aslave < 0) {
        int error = errno;
        close(*amaster);
        errno = error;
        return -1;
    }
    if (termp) {
        // FIXME: error handling
        tcsetattr(*aslave, TCSAFLUSH, termp);
    }
    if (winp) {
        // FIXME: error handling
        ioctl(*aslave, TIOCGWINSZ, winp);
    }

    dbgln("openpty, master={}, slave={}, tty_name={}", *amaster, *aslave, tty_name);

    return 0;
}

pid_t forkpty(int* amaster, int* aslave, char* name, const struct termios* termp, const struct winsize* winp)
{
    int rc = openpty(amaster, aslave, name, termp, winp);
    if (rc < 0)
        return rc;
    rc = fork();
    if (rc < 0) {
        close(*amaster);
        close(*aslave);
        return -1;
    }
    if (rc == 0)
        rc = login_tty(*aslave);
    return rc;
}

int login_tty(int fd)
{
    setsid();

    close(0);
    close(1);
    close(2);

    int rc = dup2(fd, 0);
    if (rc < 0)
        return rc;
    rc = dup2(fd, 1);
    if (rc < 0)
        return -1;
    rc = dup2(fd, 2);
    if (rc < 0)
        return rc;
    rc = close(fd);
    if (rc < 0)
        return rc;
    rc = ioctl(0, TIOCSCTTY);
    if (rc < 0)
        return rc;
    return 0;
}
