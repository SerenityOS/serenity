/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>

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
    u64 event_mask = PERF_EVENT_MMAP | PERF_EVENT_MUNMAP | PERF_EVENT_PROCESS_CREATE
        | PERF_EVENT_PROCESS_EXEC | PERF_EVENT_PROCESS_EXIT | PERF_EVENT_THREAD_CREATE | PERF_EVENT_THREAD_EXIT
        | PERF_EVENT_SIGNPOST;
    bool seen_event_type_arg = false;

    args_parser.add_option(pid_argument, "Target PID", nullptr, 'p', "PID");
    args_parser.add_option(all_processes, "Profile all processes (super-user only)", nullptr, 'a');
    args_parser.add_option(enable, "Enable", nullptr, 'e');
    args_parser.add_option(disable, "Disable", nullptr, 'd');
    args_parser.add_option(free, "Free the profiling buffer for the associated process(es).", nullptr, 'f');
    args_parser.add_option(wait, "Enable profiling and wait for user input to disable.", nullptr, 'w');
    args_parser.add_option(cmd_argument, "Command", nullptr, 'c', "command");
    args_parser.add_option(Core::ArgsParser::Option {
        true, "Enable tracking specific event type", nullptr, 't', "event_type",
        [&](String event_type) {
            seen_event_type_arg = true;
            if (event_type == "sample")
                event_mask |= PERF_EVENT_SAMPLE;
            else if (event_type == "context_switch")
                event_mask |= PERF_EVENT_CONTEXT_SWITCH;
            else if (event_type == "kmalloc")
                event_mask |= PERF_EVENT_KMALLOC;
            else if (event_type == "kfree")
                event_mask |= PERF_EVENT_KFREE;
            else if (event_type == "page_fault")
                event_mask |= PERF_EVENT_PAGE_FAULT;
            else if (event_type == "syscall")
                event_mask |= PERF_EVENT_SYSCALL;
            else {
                warnln("Unknown event type '{}' specified.", event_type);
                exit(1);
            }
            return true;
        } });

    auto print_types = [] {
        outln();
        outln("Event type can be one of: sample, context_switch, page_fault, syscall, kmalloc and kfree.");
    };

    if (!args_parser.parse(argc, argv, Core::ArgsParser::FailureBehavior::PrintUsage)) {
        print_types();
        exit(0);
    }

    if (!pid_argument && !cmd_argument && !all_processes) {
        args_parser.print_usage(stdout, argv[0]);
        print_types();
        return 0;
    }

    if (!seen_event_type_arg)
        event_mask |= PERF_EVENT_SAMPLE;

    if (pid_argument || all_processes) {
        if (!(enable ^ disable ^ wait ^ free)) {
            warnln("-p <PID> requires -e xor -d xor -w xor -f.");
            return 1;
        }

        pid_t pid = all_processes ? -1 : atoi(pid_argument);

        if (wait || enable) {
            if (profiling_enable(pid, event_mask) < 0) {
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
    profiling_enable(getpid(), event_mask);
    if (execvp(cmd_argv[0], const_cast<char**>(cmd_argv.data())) < 0) {
        perror("execv");
        return 1;
    }

    return 0;
}
