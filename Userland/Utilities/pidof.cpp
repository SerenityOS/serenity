/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int pid_of(const String& process_name, bool single_shot, bool omit_pid, pid_t pid)
{
    bool displayed_at_least_one = false;

    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.has_value())
        return 1;

    for (auto& it : all_processes.value().processes) {
        if (it.name == process_name) {
            if (!omit_pid || it.pid != pid) {
                out(displayed_at_least_one ? " {}" : "{}", it.pid);
                displayed_at_least_one = true;

                if (single_shot)
                    break;
            }
        }
    }

    if (displayed_at_least_one)
        outln();

    return 0;
}

int main(int argc, char** argv)
{
    bool single_shot = false;
    const char* omit_pid_value = nullptr;
    const char* process_name = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(single_shot, "Only return one pid", nullptr, 's');
    args_parser.add_option(omit_pid_value, "Omit the given PID, or the parent process if the special value %PPID is passed", nullptr, 'o', "pid");
    args_parser.add_positional_argument(process_name, "Process name to search for", "process-name");

    args_parser.parse(argc, argv);

    pid_t pid_to_omit = 0;
    if (omit_pid_value) {
        if (!strcmp(omit_pid_value, "%PPID")) {
            pid_to_omit = getppid();
        } else {
            auto number = StringView(omit_pid_value).to_uint();
            if (!number.has_value()) {
                warnln("Invalid value for -o");
                args_parser.print_usage(stderr, argv[0]);
                return 1;
            }
            pid_to_omit = number.value();
        }
    }
    return pid_of(process_name, single_shot, omit_pid_value != nullptr, pid_to_omit);
}
