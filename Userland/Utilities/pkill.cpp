/*
 * Copyright (c) 2022, Maxwell Trussell <maxtrussell@gmail.com>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Vector.h>
#include <LibCore/Account.h>
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
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool display_number_of_matches;
    bool case_insensitive = false;
    bool echo = false;
    bool exact_match = false;
    StringView pattern;
    HashTable<uid_t> uids_to_filter_by;
    int signal = SIGTERM;

    Core::ArgsParser args_parser;
    args_parser.add_option(display_number_of_matches, "Display the number of matching processes", "count", 'c');
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", "ignore-case", 'i');
    args_parser.add_option(echo, "Display what is killed", "echo", 'e');
    args_parser.add_option(signal, "Signal number to send", "signal", 's', "number");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Select only processes whose UID is in the given comma-separated list. Login name or numerical user ID may be used",
        .long_name = "uid",
        .short_name = 'U',
        .value_name = "uid-list",
        .accept_value = [&uids_to_filter_by](StringView comma_separated_users) {
            for (auto user_string : comma_separated_users.split_view(',')) {
                auto maybe_uid = user_string.to_uint<uid_t>();
                if (maybe_uid.has_value()) {
                    uids_to_filter_by.set(maybe_uid.value());
                } else {
                    auto maybe_account = Core::Account::from_name(user_string, Core::Account::Read::PasswdOnly);
                    if (maybe_account.is_error()) {
                        warnln("Could not find user '{}': {}", user_string, maybe_account.error());
                        return false;
                    }
                    uids_to_filter_by.set(maybe_account.release_value().uid());
                }
            }

            return true;
        },
    });
    args_parser.add_option(exact_match, "Select only processes whose names match the given pattern exactly", "exact", 'x');
    args_parser.add_positional_argument(pattern, "Process name to search for", "process-name");
    args_parser.parse(args);

    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    PosixOptions options {};
    if (case_insensitive) {
        options |= PosixFlags::Insensitive;
    }

    StringBuilder exact_pattern_builder;
    if (exact_match) {
        exact_pattern_builder.appendff("^({})$", pattern);
        pattern = exact_pattern_builder.string_view();
    }

    Regex<PosixExtended> re(pattern, options);
    if (re.parser_result.error != regex::Error::NoError) {
        return 1;
    }

    Vector<Core::ProcessStatistics> matched_processes;
    for (auto& process : all_processes.processes) {
        auto result = re.match(process.name, PosixFlags::Global);
        if (result.success) {
            if (!uids_to_filter_by.is_empty() && !uids_to_filter_by.contains(process.uid))
                continue;

            matched_processes.append(process);
        }
    }

    for (auto& process : matched_processes) {
        auto result = Core::System::kill(process.pid, signal);
        if (result.is_error())
            warnln("Killing pid {} failed. {}", process.pid, result.release_error());
        else if (echo)
            outln("{} killed (pid {})", process.name, process.pid);
    }

    if (display_number_of_matches)
        outln("{}", matched_processes.size());

    return matched_processes.is_empty() ? 1 : 0;
}
