/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Zachary Penn <zack@sysdevs.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
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

static ErrorOr<int> kill_all(StringView process_name, unsigned const signum)
{
    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    for (auto& process : all_processes.processes) {
        if (process.name == process_name) {
            if (auto maybe_error = Core::System::kill(process.pid, signum); maybe_error.is_error())
                warnln("{}", maybe_error.release_error());
        }
    }

    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int signum = SIGTERM;
    int name_argi = 1;

    if (arguments.argc != 2 && arguments.argc != 3)
        print_usage_and_exit();

    if (arguments.argc == 3) {
        name_argi = 2;

        if (arguments.argv[1][0] != '-')
            print_usage_and_exit();

        StringView signal_name_view(arguments.argv[1] + 1, strlen(arguments.argv[1] + 1));
        ByteString signal_name = signal_name_view.starts_with("SIG"sv) ? signal_name_view.substring_view(3) : signal_name_view;
        if (str2sig(signal_name.characters(), &signum) != 0) {
            warnln("'{}' is not a valid signal name or number", &arguments.argv[1][1]);
            return 2;
        }
    }

    return kill_all(arguments.strings[name_argi], signum);
}
