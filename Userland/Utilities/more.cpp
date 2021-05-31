/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

static int key_fd;

static void wait_for_key()
{
    out("\033[7m--[ more ]--\033[0m");
    fflush(stdout);
    char dummy;
    [[maybe_unused]] auto rc = read(key_fd, &dummy, 1);
    outln();
}

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    key_fd = STDOUT_FILENO;

    struct winsize ws;
    ioctl(1, TIOCGWINSZ, &ws);

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    unsigned lines_printed = 0;
    while (!feof(stdin)) {
        char buffer[BUFSIZ];
        auto* str = fgets(buffer, sizeof(buffer), stdin);
        if (!str)
            break;
        out("{}", str);
        ++lines_printed;
        if ((lines_printed % (ws.ws_row - 1)) == 0) {
            wait_for_key();
        }
    }

    close(key_fd);
    return 0;
}
