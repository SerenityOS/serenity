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

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Syscall.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int usage()
{
    printf("usage: strace [-p PID] [command...]\n");
    return 0;
}

int main(int argc, char** argv)
{
    if (argc == 1)
        return usage();

    pid_t pid = -1;
    bool pid_is_child = false;

    if (!strcmp(argv[1], "-p")) {
        if (argc != 3)
            return usage();
        pid = atoi(argv[2]);
    } else {
        pid_is_child = true;
        pid = fork();
        if (!pid) {
            kill(getpid(), SIGSTOP);
            int rc = execvp(argv[1], &argv[1]);
            if (rc < 0) {
                perror("execvp");
                exit(1);
            }
            ASSERT_NOT_REACHED();
        }
    }

    int fd = systrace(pid);
    if (fd < 0) {
        perror("systrace");
        return 1;
    }

    if (pid_is_child) {
        int rc = kill(pid, SIGCONT);
        if (rc < 0) {
            perror("kill(pid, SIGCONT)");
            return 1;
        }
    }

    for (;;) {
        u32 call[5];
        int nread = read(fd, &call, sizeof(call));
        if (nread == 0)
            break;
        if (nread < 0) {
            perror("read");
            return 1;
        }
        ASSERT(nread == sizeof(call));
        fprintf(stderr, "%s(%#x, %#x, %#x) = %#x\n", Syscall::to_string((Syscall::Function)call[0]), call[1], call[2], call[3], call[4]);
    }

    int rc = close(fd);
    if (rc < 0) {
        perror("close");
        return 1;
    }

    return 0;
}
