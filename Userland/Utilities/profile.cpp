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

static Optional<pid_t> determine_pid_to_profile(StringView pid_argument, bool all_processes);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView pid_argument {};
    Vector<StringView> command;
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
    args_parser.add_option(all_processes, "Profile all processes (super-user only), result at /sys/kernel/profile", nullptr, 'a');
    args_parser.add_option(enable, "Enable", nullptr, 'e');
    args_parser.add_option(disable, "Disable", nullptr, 'd');
    args_parser.add_option(free, "Free the profiling buffer for the associated process(es).", nullptr, 'f');
    args_parser.add_option(wait, "Enable profiling and wait for user input to disable.", nullptr, 'w');
    args_parser.add_option(Core::ArgsParser::Option {
        Core::ArgsParser::OptionArgumentMode::Required,
        "Enable tracking specific event type", nullptr, 't', "event_type",
        [&](ByteString event_type) {
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
            else if (event_type == "filesystem")
                event_mask |= PERF_EVENT_FILESYSTEM;
            else {
                warnln("Unknown event type '{}' specified.", event_type);
                exit(1);
            }
            return true;
        } });
    args_parser.add_positional_argument(command, "Command to profile", "command", Core::ArgsParser::Required::No);
    args_parser.set_stop_on_first_non_option(true);

    auto print_types = [] {
        outln();
        outln("Event type can be one of: sample, context_switch, page_fault, syscall, filesystem, kmalloc and kfree.");
    };

    if (!args_parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage)) {
        print_types();
        exit(0);
    }

    if (pid_argument.is_empty() && command.is_empty() && !all_processes) {
        args_parser.print_usage(stdout, arguments.strings[0]);
        print_types();
        return 0;
    }

    if (!seen_event_type_arg)
        event_mask |= PERF_EVENT_SAMPLE;

    if (!pid_argument.is_empty() || all_processes) {
        if (!(enable ^ disable ^ wait ^ free)) {
            warnln("-a and -p <PID> requires -e xor -d xor -w xor -f.");
            return 1;
        }

        auto pid_opt = determine_pid_to_profile(pid_argument, all_processes);
        if (!pid_opt.has_value()) {
            warnln("-p <PID> requires an integer value.");
            return 1;
        }

        pid_t pid = pid_opt.value();
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

    dbgln("Enabling profiling for PID {}", getpid());
    TRY(Core::System::profiling_enable(getpid(), event_mask));
    TRY(Core::System::exec(command[0], command, Core::System::SearchInPath::Yes));

    return 0;
}

static Optional<pid_t> determine_pid_to_profile(StringView pid_argument, bool all_processes)
{
    if (all_processes) {
        return { -1 };
    }

    // pid_argument is guaranteed to have a value
    return pid_argument.to_number<pid_t>();
}
