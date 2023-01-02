/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
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

    char tty_name[32];
    int rc = ptsname_r(*amaster, tty_name, sizeof(tty_name));
    if (rc < 0) {
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
        if (tcsetattr(*aslave, TCSAFLUSH, termp) == -1) {
            int error = errno;
            close(*aslave);
            close(*amaster);
            errno = error;
            return -1;
        }
    }
    if (winp) {
        if (ioctl(*aslave, TIOCGWINSZ, winp) == -1) {
            int error = errno;
            close(*aslave);
            close(*amaster);
            errno = error;
            return -1;
        }
    }
    return 0;
}

pid_t forkpty(int* amaster, char* name, const struct termios* termp, const struct winsize* winp)
{
    int master;
    int slave;
    if (openpty(&master, &slave, name, termp, winp) < 0)
        return -1;
    pid_t pid = fork();
    if (pid < 0) {
        close(master);
        close(slave);
        return -1;
    }
    *amaster = master;
    if (pid == 0) {
        close(master);
        if (login_tty(slave) < 0)
            _exit(1);
        return 0;
    }
    close(slave);
    return pid;
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
