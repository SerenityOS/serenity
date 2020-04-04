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
#include <LibC/sys/arch/i386/regs.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

static int usage()
{
    printf("usage: sdb [command...]\n");
    return 1;
}

static int g_pid = -1;

static void handle_sigint(int)
{
    if (g_pid == -1)
        return;

    if (ptrace(PT_DETACH, g_pid, 0, 0) == -1) {
        perror("detach");
    }
}

void run_child_and_attach(char** argv)
{
    int pid = fork();

    if (!pid) {
        if (ptrace(PT_TRACE_ME, 0, 0, 0) == -1) {
            perror("traceme");
            return exit(1);
        }

        int rc = execvp(argv[1], &argv[1]);
        if (rc < 0) {
            perror("execvp");
            exit(1);
        }
        ASSERT_NOT_REACHED();
    }

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("waitpid");
        exit(1);
    }
    g_pid = pid;
}

int main(int argc, char** argv)
{
    if (argc == 1)
        return usage();

    run_child_and_attach(argv);

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    if (ptrace(PT_ATTACH, g_pid, 0, 0) == -1) {
        perror("attach");
        return 1;
    }
    if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
        perror("waitpid");
        return 1;
    }

    if (ptrace(PT_CONTINUE, g_pid, 0, 0) == -1) {
        perror("continue");
    }

    // wait for breakpoint
    if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
        perror("waitpid");
        return 1;
    }

    PtraceRegisters regs;
    if (ptrace(PT_GETREGS, g_pid, &regs, 0) == -1) {
        perror("getregs");
        return 1;
    }

    printf("hit breakpoint\n");
    printf("eip:0x%x\n", regs.eip);

    uint32_t data = ptrace(PT_PEEK, g_pid, (void*)regs.eip, 0);
    printf("data: 0x%x\n", data);

    if (ptrace(PT_CONTINUE, g_pid, 0, 0) == -1) {
        perror("continue");
    }

    if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
        perror("waitpid");
        return 1;
    }
}
