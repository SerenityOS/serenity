/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <ctype.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>

static void print_usage_and_exit()
{
    warnln("usage: killall [-signal] process_name");
    exit(1);
}

static int kill_all(const String& process_name, const unsigned signum)
{
    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.has_value())
        return 1;

    for (auto& process : all_processes.value().processes) {
        if (process.name == process_name) {
            int ret = kill(process.pid, signum);
            if (ret < 0)
                perror("kill");
        }
    }

    return 0;
}

int main(int argc, char** argv)
{
    unsigned signum = SIGTERM;
    int name_argi = 1;

    if (argc != 2 && argc != 3)
        print_usage_and_exit();

    if (argc == 3) {
        name_argi = 2;

        if (argv[1][0] != '-')
            print_usage_and_exit();

        Optional<unsigned> number;

        if (isalpha(argv[1][1])) {
            int value = getsignalbyname(&argv[1][1]);
            if (value >= 0 && value < NSIG)
                number = value;
        }

        if (!number.has_value())
            number = String(&argv[1][1]).to_uint();

        if (!number.has_value()) {
            warnln("'{}' is not a valid signal name or number", &argv[1][1]);
            return 2;
        }
        signum = number.value();
    }

    return kill_all(argv[name_argi], signum);
}
