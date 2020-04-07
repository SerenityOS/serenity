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
#include <AK/ByteBuffer.h>
#include <LibC/sys/arch/i386/regs.h>
#include <LibCore/File.h>
#include <LibELF/ELFImage.h>
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

    g_pid = pid;

    if (waitpid(pid, nullptr, WSTOPPED) != pid) {
        perror("waitpid");
        exit(1);
    }

    if (ptrace(PT_ATTACH, g_pid, 0, 0) == -1) {
        perror("attach");
        exit(1);
    }

    if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
        perror("waitpid");
        exit(1);
    }

    dbg() << "debugee should continue until before execve exit";
    if (ptrace(PT_CONTINUE, g_pid, 0, 0) == -1) {
        perror("continue");
    }

    // we want to continue until the exit from the 'execve' sycsall
    // we do this to ensure that when we start debugging the process,
    // it executes the target image, and not the forked image of the debugger
    // NOTE: we only need to do this when we are debugging a new process (i.e not attaching to a process that's already running!)
    // if (ptrace(PT_SYSCALL, g_pid, 0, 0) == -1) {
    //     perror("syscall");
    //     exit(1);
    // }

    if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
        perror("wait_pid");
        exit(1);
    }
    // dbg() << "debugee should continue until after execve exit";
    // sleep(3);

    // if (ptrace(PT_CONTINUE, g_pid, 0, 0) == -1) {
    //     perror("continue");
    // }

    // sleep(10);

    // if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
    //     perror("wait_pid");
    //     exit(1);
    // }

    // dbg() << "debugee should already be running";
    // if (ptrace(PT_CONTINUE, g_pid, 0, 0) == -1) {
    //     perror("continue");
    // }
}

VirtualAddress get_entry_point(int pid)
{
    auto path = String::format("/proc/%d/exe", pid);
    dbg() << "path: " << path;
    auto file = Core::File::construct(path);
    if (!file->open(Core::File::ReadOnly)) {
        fprintf(stderr, "Failed to open Debugged executable");
        exit(1);
    }
    auto data = file->read_all();
    dbg() << "data size:" << data.size();
    ELFImage elf(data.data(), data.size());
    return elf.entry();
}

int main(int argc, char** argv)
{
    // TODO: pledge & unveil
    // TOOD: check that we didn't somehow hurt performance. boot seems slower? (or it's just laptop battey)
    if (argc == 1)
        return usage();

    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    run_child_and_attach(argv);

    dbg() << "pid:" << g_pid;
    auto entry_point = get_entry_point(g_pid);
    dbg() << "entry point:" << entry_point;

    const uint32_t original_instruction_data = ptrace(PT_PEEK, g_pid, (void*)entry_point.as_ptr(), 0);

    dbg() << "peeked data:" << (void*)original_instruction_data;

    if (ptrace(PT_POKE, g_pid, (void*)entry_point.as_ptr(), (original_instruction_data & ~(uint32_t)0xff) | 0xcc) < 0) {
        perror("poke");
        return 1;
    }

    dbg() << "continuting";

    if (ptrace(PT_CONTINUE, g_pid, 0, 0) == -1) {
        perror("continue");
    }
    dbg() << "continued";

    // wait for breakpoint
    if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
        perror("waitpid");
        return 1;
    }

    printf("hit breakpoint\n");

    if (ptrace(PT_POKE, g_pid, (void*)entry_point.as_ptr(), original_instruction_data) < 0) {
        perror("poke");
        return 1;
    }

    PtraceRegisters regs;
    if (ptrace(PT_GETREGS, g_pid, &regs, 0) < 0) {
        perror("getregs");
        return 1;
    }

    dbg() << "eip after breakpoint: " << (void*)regs.eip;

    regs.eip = reinterpret_cast<u32>(entry_point.as_ptr());
    dbg() << "settings eip back to:" << (void*)regs.eip;
    if (ptrace(PT_SETREGS, g_pid, &regs, 0) < 0) {
        perror("setregs");
        return 1;
    }

    dbg() << "continuig";

    if (ptrace(PT_CONTINUE, g_pid, 0, 0) == -1) {
        perror("continue");
    }

    // wait for end

    if (waitpid(g_pid, nullptr, WSTOPPED) != g_pid) {
        perror("waitpid");
        return 1;
    }
}
