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
#include <LibCore/CArgsParser.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    int pid = -1;
    bool pid_is_child = false;
    Vector<const char*> command;

    Core::ArgsParser args_parser;
    args_parser.add_option(pid, "Trace the process with this PID", "pid", 'p', "PID");
    args_parser.add_positional_argument(command, "Command to trace", "command", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (pid == -1) {
        pid_is_child = true;
        pid = fork();
        if (pid < 0) {
            perror("fork");
            ASSERT_NOT_REACHED();
        } else if (pid == 0) {
            // Stop ourselves and wait for the parent to set up tracing.
            kill(getpid(), SIGSTOP);
            // When the parent SIGCONT's us, proceed.
            execvp(command[0], const_cast<char**>(command.data()));
            perror("execvp");
            return 1;
        }
    }

    ASSERT(pid > 0);
    ASSERT(pid != getpid());

    auto path = String::format("/proc/%d/systrace", pid);
    int fd = open(path.characters(), O_RDONLY);
    if (fd < 0) {
        int saved_errno = errno;
        String message = String::format("Failed to open %s", path.characters());
        errno = saved_errno;
        perror(message.characters());
        return 1;
    }

    if (pid_is_child) {
        int rc = kill(pid, SIGCONT);
        if (rc < 0) {
            perror("kill(pid, SIGCONT)");
            return 1;
        }
    }

    // FIXME: The following is broken, and we would need
    //        a streaming JSON parser to fix it.

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
