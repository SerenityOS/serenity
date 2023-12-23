/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

struct Options {
    bool single_shot { false };
    Optional<pid_t> pid_to_omit;
    StringView process_name;
    StringView pid_separator { " "sv };
};

static ErrorOr<int> pid_of(Options const& options)
{
    bool displayed_at_least_one = false;

    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    for (auto& it : all_processes.processes) {
        if (it.name != options.process_name || options.pid_to_omit == it.pid)
            continue;

        if (displayed_at_least_one)
            out("{}{}"sv, options.pid_separator, it.pid);
        else
            out("{}"sv, it.pid);

        displayed_at_least_one = true;

        if (options.single_shot)
            break;
    }

    if (displayed_at_least_one)
        outln();

    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Options options;

    Core::ArgsParser args_parser;
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Omit the given PID, or the parent process if the special value %PPID is passed",
        .short_name = 'o',
        .value_name = "pid",
        .accept_value = [&options](auto omit_pid_value) {
            if (omit_pid_value.is_empty())
                return false;

            if (omit_pid_value == "%PPID"sv) {
                options.pid_to_omit = getppid();
                return true;
            }

            auto number = omit_pid_value.template to_number<pid_t>();
            if (!number.has_value())
                return false;

            options.pid_to_omit = number.value();
            return true;
        },
    });
    args_parser.add_option(options.single_shot, "Only return one pid", nullptr, 's');
    args_parser.add_option(options.pid_separator, "Use `separator` to separate multiple pids", nullptr, 'S', "separator");
    args_parser.add_positional_argument(options.process_name, "Process name to search for", "process-name");

    args_parser.parse(args);
    return pid_of(options);
}
