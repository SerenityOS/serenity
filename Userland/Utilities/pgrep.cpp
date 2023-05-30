/*
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibRegex/Regex.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool display_number_of_matches = false;
    auto pid_delimiter = "\n"sv;
    bool case_insensitive = false;
    bool invert_match = false;
    StringView pattern;

    Core::ArgsParser args_parser;
    args_parser.add_option(display_number_of_matches, "Suppress normal output and print the number of matching processes", "count", 'c');
    args_parser.add_option(pid_delimiter, "Set the string used to delimit multiple pids", "delimiter", 'd', nullptr);
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", "ignore-case", 'i');
    args_parser.add_option(invert_match, "Select non-matching lines", "invert-match", 'v');
    args_parser.add_positional_argument(pattern, "Process name to search for", "process-name");
    args_parser.parse(args);

    PosixOptions options {};
    if (case_insensitive)
        options |= PosixFlags::Insensitive;

    Regex<PosixExtended> re(pattern, options);
    if (re.parser_result.error != regex::Error::NoError) {
        return 1;
    }

    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    Vector<pid_t> matches;
    for (auto& it : all_processes.processes) {
        auto result = re.match(it.name, PosixFlags::Global);
        if (result.success ^ invert_match) {
            matches.append(it.pid);
        }
    }

    if (display_number_of_matches) {
        outln("{}", matches.size());
    } else {
        quick_sort(matches);
        auto displayed_at_least_one = false;
        for (auto& match : matches) {
            if (displayed_at_least_one)
                out("{}{}"sv, pid_delimiter, match);
            else
                out("{}"sv, match);

            displayed_at_least_one = true;
        }

        if (displayed_at_least_one)
            outln();
    }

    return matches.size() > 0 ? 0 : 1;
}
