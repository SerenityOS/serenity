/*
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibRegex/Regex.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool display_number_of_matches = false;
    auto pid_delimiter = "\n"sv;
    bool case_insensitive = false;
    bool list_process_name = false;
    bool invert_match = false;
    bool exact_match = false;
    bool newest_only = false;
    bool oldest_only = false;
    Optional<UnixDateTime> display_if_older_than;
    HashTable<uid_t> uids_to_filter_by;
    StringView pattern;

    Core::ArgsParser args_parser;
    args_parser.add_option(display_number_of_matches, "Suppress normal output and print the number of matching processes", "count", 'c');
    args_parser.add_option(pid_delimiter, "Set the string used to delimit multiple pids", "delimiter", 'd', nullptr);
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", "ignore-case", 'i');
    args_parser.add_option(newest_only, "Select the most recently created process only", "newest", 'n');
    args_parser.add_option(oldest_only, "Select the least recently created process only", "oldest", 'o');
    args_parser.add_option(list_process_name, "List the process name in addition to its pid", "list-name", 'l');
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Select only processes older than the specified number of seconds",
        .long_name = "older",
        .short_name = 'O',
        .value_name = "seconds",
        .accept_value = [&display_if_older_than](StringView seconds_string) {
            auto number = seconds_string.to_number<u64>();

            if (number.has_value() && number.value() <= NumericLimits<i64>::max()) {
                auto now_time = UnixDateTime::now();
                display_if_older_than = now_time - Duration::from_seconds(static_cast<i64>(number.value()));
            }

            return display_if_older_than.has_value();
        },
    });
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Select only processes whose UID is in the given comma-separated list. Login name or numerical user ID may be used",
        .long_name = "uid",
        .short_name = 'U',
        .value_name = "uid-list",
        .accept_value = [&uids_to_filter_by](StringView comma_separated_users) {
            for (auto user_string : comma_separated_users.split_view(',')) {
                auto maybe_uid = user_string.to_number<uid_t>();
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
    args_parser.add_option(invert_match, "Select non-matching lines", "invert-match", 'v');
    args_parser.add_option(exact_match, "Select only processes whose names match the given pattern exactly", "exact", 'x');
    args_parser.add_positional_argument(pattern, "Process name to search for", "process-name");
    args_parser.parse(args);

    if (newest_only && oldest_only) {
        warnln("The -n and -o options are mutually exclusive");
        args_parser.print_usage(stderr, args.strings[0]);
        return 1;
    }

    PosixOptions options {};
    if (case_insensitive)
        options |= PosixFlags::Insensitive;

    StringBuilder exact_pattern_builder;
    if (exact_match) {
        exact_pattern_builder.appendff("^({})$", pattern);
        pattern = exact_pattern_builder.string_view();
    }

    Regex<PosixExtended> re(pattern, options);
    if (re.parser_result.error != regex::Error::NoError) {
        return 1;
    }

    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    Vector<Core::ProcessStatistics> matches;
    for (auto const& it : all_processes.processes) {
        auto result = re.match(it.name, PosixFlags::Global);
        if (result.success ^ invert_match) {
            if (!uids_to_filter_by.is_empty() && !uids_to_filter_by.contains(it.uid))
                continue;

            if (display_if_older_than.has_value() && it.creation_time >= display_if_older_than.value())
                continue;

            matches.append(it);
        }
    }

    if (display_number_of_matches) {
        outln("{}", matches.size());
    } else if (!matches.is_empty()) {
        quick_sort(matches, [](auto const& a, auto const& b) { return a.creation_time < b.creation_time; });
        if (newest_only)
            matches = { matches.last() };
        else if (oldest_only)
            matches = { matches.first() };

        auto displayed_at_least_one = false;
        for (auto& match : matches) {
            if (displayed_at_least_one)
                out("{}"sv, pid_delimiter);

            out("{}"sv, match.pid);

            if (list_process_name)
                out(" {}"sv, match.name);

            displayed_at_least_one = true;
        }

        if (displayed_at_least_one)
            outln();
    }

    return matches.is_empty() ? 1 : 0;
}
