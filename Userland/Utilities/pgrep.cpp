/*
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibRegex/Regex.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool case_insensitive = false;
    bool invert_match = false;
    char const* pattern = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", nullptr, 'i');
    args_parser.add_option(invert_match, "Select non-matching lines", "invert-match", 'v');
    args_parser.add_positional_argument(pattern, "Process name to search for", "process-name");
    args_parser.parse(argc, argv);

    PosixOptions options {};
    if (case_insensitive)
        options |= PosixFlags::Insensitive;

    Regex<PosixExtended> re(pattern, options);
    if (re.parser_result.error != regex::Error::NoError) {
        return 1;
    }

    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.has_value())
        return 1;

    Vector<pid_t> matches;
    for (auto& it : all_processes.value().processes) {
        auto result = re.match(it.name, PosixFlags::Global);
        if (result.success ^ invert_match) {
            matches.append(it.pid);
        }
    }

    quick_sort(matches);

    for (auto& match : matches) {
        outln("{}", match);
    }

    return 0;
}
