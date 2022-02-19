/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Zachary Penn <zack@sysdevs.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <signal.h>
#include <stdlib.h>

static void print_usage_and_exit()
{
    warnln("usage: killall [-signal] process_name");
    exit(1);
}

static ErrorOr<int> kill_all(const String& process_name, const unsigned signum)
{
    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.has_value())
        return 1;

    for (auto& process : all_processes.value().processes) {
        if (process.name == process_name) {
            TRY(Core::System::kill(process.pid, signum));
        }
    }

    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    unsigned signum = SIGTERM;
    int name_argi = 1;

    if (arguments.argc != 2 && arguments.argc != 3)
        print_usage_and_exit();

    if (arguments.argc == 3) {
        name_argi = 2;

        if (arguments.argv[1][0] != '-')
            print_usage_and_exit();

        Optional<unsigned> number;

        if (isalpha(arguments.argv[1][1])) {
            int value = getsignalbyname(&arguments.argv[1][1]);
            if (value >= 0 && value < NSIG)
                number = value;
        }

        if (!number.has_value())
            number = String(&arguments.argv[1][1]).to_uint();

        if (!number.has_value()) {
            warnln("'{}' is not a valid signal name or number", &arguments.argv[1][1]);
            return 2;
        }
        signum = number.value();
    }

    return kill_all(arguments.strings[name_argi], signum);
}
