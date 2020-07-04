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
#include <AK/LogStream.h>
#include <AK/Types.h>
#include <Kernel/API/Syscall.h>
#include <LibC/sys/arch/i386/regs.h>
#include <LibCore/ArgsParser.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

static int g_pid = -1;

static void handle_sigint(int)
{
    if (g_pid == -1)
        return;

    if (ptrace(PT_DETACH, g_pid, 0, 0) == -1) {
        perror("detach");
    }
}

int main(int argc, char** argv)
{
    Vector<const char*> child_argv;
    bool spawned_new_process = false;

    Core::ArgsParser parser;
    parser.add_option(g_pid, "Trace the given PID", "pid", 'p', "pid");
    parser.add_positional_argument(child_argv, "Arguments to exec", "argument", Core::ArgsParser::Required::No);

    parser.parse(argc, argv);

    if (g_pid == -1) {
        if (child_argv.is_empty()) {
            fprintf(stderr, "strace: Expected either a pid or some arguments\n");
            return 1;
        }

        child_argv.append(nullptr);
        spawned_new_process = true;
        int pid = fork();
        if (pid < 0) {
            perror("fork");
            return 1;
        }

        if (!pid) {
            if (ptrace(PT_TRACE_ME, 0, 0, 0) == -1) {
                perror("traceme");
                return 1;
            }
            int rc = execvp(child_argv.first(), const_cast<char**>(child_argv.data()));
            if (rc < 0) {
                perror("execvp");
                exit(1);
            }
            ASSERT_NOT_REACHED();
        }

        g_pid = pid;
        if (waitpid(pid, nullptr, WSTOPPED) != pid) {
            perror("waitpid");
            return 1;
        }
    }

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

    if (spawned_new_process) {

        if (ptrace(PT_CONTINUE, g_pid, 0, 0) < 0) {
            perror("continue");
            return 1;
        }
        if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
            perror("wait_pid");
            return 1;
        }
    }

    for (;;) {
        if (ptrace(PT_SYSCALL, g_pid, 0, 0) == -1) {
            if (errno == ESRCH)
                return 0;
            perror("syscall");
            return 1;
        }
        if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
            perror("wait_pid");
            return 1;
        }

        PtraceRegisters regs = {};
        if (ptrace(PT_GETREGS, g_pid, &regs, 0) == -1) {
            perror("getregs");
            return 1;
        }

        u32 syscall_index = regs.eax;
        u32 arg1 = regs.edx;
        u32 arg2 = regs.ecx;
        u32 arg3 = regs.ebx;

        // skip syscall exit
        if (ptrace(PT_SYSCALL, g_pid, 0, 0) == -1) {
            if (errno == ESRCH)
                return 0;
            perror("syscall");
            return 1;
        }
        if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
            perror("wait_pid");
            return 1;
        }

        if (ptrace(PT_GETREGS, g_pid, &regs, 0) == -1) {
            if (errno == ESRCH && syscall_index == SC_exit) {
                regs.eax = 0;
            } else {
                perror("getregs");
                return 1;
            }
        }

        u32 res = regs.eax;

        fprintf(stderr, "%s(0x%x, 0x%x, 0x%x)\t=%d\n",
            Syscall::to_string(
                (Syscall::Function)syscall_index),
            arg1,
            arg2,
            arg3,
            res);
    }

    return 0;
}
