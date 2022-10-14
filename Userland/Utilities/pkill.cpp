/*
 * Copyright (c) 2022, Maxwell Trussell <maxtrussell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibRegex/Regex.h>
#include <LibRegex/RegexOptions.h>
#include <signal.h>
#include <sys/types.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio proc rpath"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool case_insensitive = false;
    bool echo = false;
    char const* pattern = nullptr;
    int signal = SIGTERM;

    Core::ArgsParser args_parser;
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", nullptr, 'i');
    args_parser.add_option(echo, "Display what is killed", "echo", 'e');
    args_parser.add_option(signal, "Signal number to send", "signal", 's', "number");
    args_parser.add_positional_argument(pattern, "Process name to search for", "process-name");
    args_parser.parse(args);

    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.has_value()) {
        return 1;
    }

    PosixOptions options {};
    if (case_insensitive) {
        options |= PosixFlags::Insensitive;
    }

    Regex<PosixExtended> re(pattern, options);
    if (re.parser_result.error != regex::Error::NoError) {
        return 1;
    }

    Vector<Core::ProcessStatistics> matched_processes;
    for (auto& process : all_processes.value().processes) {
        auto result = re.match(process.name, PosixFlags::Global);
        if (result.success) {
            matched_processes.append(process);
        }
    }

    if (matched_processes.is_empty()) {
        return 1;
    }

    for (auto& process : matched_processes) {
        if (echo) {
            outln("{} killed (pid {})", process.name, process.pid);
        }
        kill(process.pid, signal);
    }
    return 0;
}
