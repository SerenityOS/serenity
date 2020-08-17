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

#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

static int key_fd;

static void wait_for_key()
{
    printf("\033[7m--[ more ]--\033[0m");
    fflush(stdout);
    char dummy;
    (void)read(key_fd, &dummy, 1);
    printf("\n");
}

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

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
        printf("%s", str);
        ++lines_printed;
        if ((lines_printed % (ws.ws_row - 1)) == 0) {
            wait_for_key();
        }
    }

    close(key_fd);
    return 0;
}
