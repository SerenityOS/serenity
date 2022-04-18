/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    char const* pid_argument = nullptr;
    char const* cmd_argument = nullptr;
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
    args_parser.add_option(all_processes, "Profile all processes (super-user only), result at /proc/profile", nullptr, 'a');
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
            else if (event_type == "read")
                event_mask |= PERF_EVENT_READ;
            else {
                warnln("Unknown event type '{}' specified.", event_type);
                exit(1);
            }
            return true;
        } });

    auto print_types = [] {
        outln();
        outln("Event type can be one of: sample, context_switch, page_fault, syscall, read, kmalloc and kfree.");
    };

    if (!args_parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage)) {
        print_types();
        exit(0);
    }

    if (!pid_argument && !cmd_argument && !all_processes) {
        args_parser.print_usage(stdout, arguments.argv[0]);
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
            TRY(Core::System::profiling_enable(pid, event_mask));

            if (!wait)
                return 0;
        }

        if (wait) {
            outln("Profiling enabled, waiting for user input to disable...");
            (void)getchar();
        }

        if (wait || disable)
            TRY(Core::System::profiling_disable(pid));

        if (free)
            TRY(Core::System::profiling_free_buffer(pid));

        return 0;
    }

    auto cmd_parts = StringView(cmd_argument).split_view(' ');

    dbgln("Enabling profiling for PID {}", getpid());
    TRY(Core::System::profiling_enable(getpid(), event_mask));
    TRY(Core::System::exec(cmd_parts[0], cmd_parts, Core::System::SearchInPath::Yes));

    return 0;
}
