/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    warnln("usage: kill [-signal] <PID>");
    exit(1);
}

int main(int argc, char** argv)
{
    if (pledge("stdio proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (argc == 2 && !strcmp(argv[1], "-l")) {
        for (size_t i = 0; i < NSIG; ++i) {
            if (i && !(i % 5))
                outln("");
            out("{:2}) {:10}", i, getsignalname(i));
        }
        outln("");
        return 0;
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
            warnln("'{}' is not a valid signal name or number", &argv[1][1]);
            return 2;
        }
        signum = number.value();
    }
    auto pid_opt = String(argv[pid_argi]).to_int();
    if (!pid_opt.has_value()) {
        warnln("'{}' is not a valid PID", argv[pid_argi]);
        return 3;
    }
    pid_t pid = pid_opt.value();

    int rc = kill(pid, signum);
    if (rc < 0)
        perror("kill");
    return 0;
}
