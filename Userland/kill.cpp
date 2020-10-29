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

#include <AK/Optional.h>
#include <AK/String.h>
#include <ctype.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static void print_usage_and_exit()
{
    printf("usage: kill [-signal] <PID>\n");
    exit(1);
}

static const char* signal_names[] = {
    "INVAL",
    "HUP",
    "INT",
    "QUIT",
    "ILL",
    "TRAP",
    "ABRT",
    "BUS",
    "FPE",
    "KILL",
    "USR1",
    "SEGV",
    "USR2",
    "PIPE",
    "ALRM",
    "TERM",
    "STKFLT",
    "CHLD",
    "CONT",
    "STOP",
    "TSTP",
    "TTIN",
    "TTOU",
    "URG",
    "XCPU",
    "XFSZ",
    "VTALRM",
    "PROF",
    "WINCH",
    "IO",
    "INFO",
    "SYS"
};

static_assert(sizeof(signal_names) == sizeof(const char*) * 32);

int getsignalbyname(const char* name)
{
    ASSERT(name);
    for (size_t i = 0; i < NSIG; ++i) {
        auto* signal_name = signal_names[i];
        if (!strcmp(signal_name, name))
            return i;
    }
    errno = EINVAL;
    return -1;
}

int main(int argc, char** argv)
{
    if (pledge("stdio proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (argc != 2 && argc != 3)
        print_usage_and_exit();
    unsigned signum = SIGTERM;
    int pid_argi = 1;
    if (argc == 3) {
        pid_argi = 2;
        if (argv[1][0] != '-')
            print_usage_and_exit();

        Optional<unsigned> number;

        if (isalpha(argv[1][1])) {
            int value = getsignalbyname(&argv[1][1]);
            if (value >= 0 && value < NSIG)
                number = value;
        }

        if (!number.has_value())
            number = StringView(&argv[1][1]).to_uint();

        if (!number.has_value()) {
            printf("'%s' is not a valid signal name or number\n", &argv[1][1]);
            return 2;
        }
        signum = number.value();
    }
    auto pid_opt = String(argv[pid_argi]).to_int();
    if (!pid_opt.has_value()) {
        printf("'%s' is not a valid PID\n", argv[pid_argi]);
        return 3;
    }
    pid_t pid = pid_opt.value();

    int rc = kill(pid, signum);
    if (rc < 0)
        perror("kill");
    return 0;
}
