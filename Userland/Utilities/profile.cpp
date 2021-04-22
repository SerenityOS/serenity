/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;

    const char* pid_argument = nullptr;
    const char* cmd_argument = nullptr;
    bool wait = false;
    bool free = false;
    bool enable = false;
    bool disable = false;
    bool all_processes = false;

    args_parser.add_option(pid_argument, "Target PID", nullptr, 'p', "PID");
    args_parser.add_option(all_processes, "Profile all processes (super-user only)", nullptr, 'a');
    args_parser.add_option(enable, "Enable", nullptr, 'e');
    args_parser.add_option(disable, "Disable", nullptr, 'd');
    args_parser.add_option(free, "Free the profiling buffer for the associated process(es).", nullptr, 'f');
    args_parser.add_option(wait, "Enable profiling and wait for user input to disable.", nullptr, 'w');
    args_parser.add_option(cmd_argument, "Command", nullptr, 'c', "command");

    args_parser.parse(argc, argv);

    if (!pid_argument && !cmd_argument && !all_processes) {
        args_parser.print_usage(stdout, argv[0]);
        return 0;
    }

    if (pid_argument || all_processes) {
        if (!(enable ^ disable ^ wait ^ free)) {
            fprintf(stderr, "-p <PID> requires -e xor -d xor -w xor -f.\n");
            return 1;
        }

        pid_t pid = all_processes ? -1 : atoi(pid_argument);

        if (wait || enable) {
            if (profiling_enable(pid) < 0) {
                perror("profiling_enable");
                return 1;
            }

            if (!wait)
                return 0;
        }

        if (wait) {
            outln("Profiling enabled, waiting for user input to disable...");
            (void)getchar();
        }

        if (wait || disable) {
            if (profiling_disable(pid) < 0) {
                perror("profiling_disable");
                return 1;
            }
            outln("Profiling disabled.");
        }

        if (free && profiling_free_buffer(pid) < 0) {
            perror("profiling_disable");
            return 1;
        }

        return 0;
    }

    auto cmd_parts = String(cmd_argument).split(' ');
    Vector<const char*> cmd_argv;

    for (auto& part : cmd_parts)
        cmd_argv.append(part.characters());

    cmd_argv.append(nullptr);

    dbgln("Enabling profiling for PID {}", getpid());
    profiling_enable(getpid());
    if (execvp(cmd_argv[0], const_cast<char**>(cmd_argv.data())) < 0) {
        perror("execv");
        return 1;
    }

    return 0;
}
