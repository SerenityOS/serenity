/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/sysmacros.h>
#include <unistd.h>

static ErrorOr<Optional<String>> tty_stat_to_pseudo_name(struct stat tty_stat)
{
    int tty_device_major = major(tty_stat.st_rdev);
    int tty_device_minor = minor(tty_stat.st_rdev);

    if (tty_device_major == 201)
        return String::formatted("pts:{}", tty_device_minor);

    if (tty_device_major == 4)
        return String::formatted("tty:{}", tty_device_minor);

    return OptionalNone {};
}

static ErrorOr<String> determine_tty_pseudo_name()
{
    auto tty_stat = TRY(Core::System::fstat(STDIN_FILENO));
    auto maybe_tty_pseudo_name = TRY(tty_stat_to_pseudo_name(tty_stat));
    return maybe_tty_pseudo_name.value_or("n/a"_short_string);
}

static ErrorOr<String> parse_tty_pseudo_name(StringView tty_name)
{
    auto tty_name_parts = tty_name.split_view(":"sv, AK::SplitBehavior::KeepEmpty);
    String tty_full_name;
    StringView tty_full_name_view;
    if (tty_name_parts.size() == 1) {
        tty_full_name_view = tty_name;
    } else {
        if (tty_name_parts.size() != 2)
            return Error::from_errno(ENOTTY);

        auto tty_device_type = tty_name_parts[0];
        auto tty_number = tty_name_parts[1];
        if (tty_device_type == "tty"sv)
            tty_full_name = TRY(String::formatted("/dev/tty{}", tty_number));
        else if (tty_device_type == "pts"sv)
            tty_full_name = TRY(String::formatted("/dev/pts/{}", tty_number));
        else
            return Error::from_errno(ENOTTY);

        tty_full_name_view = tty_full_name.bytes_as_string_view();
    }

    auto tty_stat = TRY(Core::System::stat(tty_full_name_view));
    auto maybe_tty_pseudo_name = TRY(tty_stat_to_pseudo_name(tty_stat));
    if (!maybe_tty_pseudo_name.has_value())
        return Error::from_errno(ENOTTY);

    return maybe_tty_pseudo_name.release_value();
}

template<typename Value, typename ParseValue>
Core::ArgsParser::Option make_list_option(Vector<Value>& value_list, char const* help_string, char const* long_name, char short_name, char const* value_name, ParseValue parse_value)
{
    return Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = help_string,
        .long_name = long_name,
        .short_name = short_name,
        .value_name = value_name,
        .accept_value = [&](StringView s) {
            auto parts = s.split_view_if([](char c) { return c == ',' || c == ' '; });
            for (auto const& part : parts) {
                auto value = parse_value(part);
                if (!value.has_value())
                    return false;
                value_list.append(value.value());
            }
            return true;
        },
    };
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty"));

    auto this_pseudo_tty_name = TRY(determine_tty_pseudo_name());

    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil("/dev/", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    enum class Alignment {
        Left,
        Right,
    };

    struct Column {
        String title;
        Alignment alignment { Alignment::Left };
        int width { 0 };
        String buffer;
    };

    bool every_process_flag = false;
    bool every_terminal_process_flag = false;
    bool full_format_flag = false;
    bool provided_filtering_option = false;
    bool provided_quick_pid_list = false;
    Vector<pid_t> pid_list;
    Vector<pid_t> parent_pid_list;
    Vector<DeprecatedString> tty_list;
    Vector<uid_t> uid_list;

    Core::ArgsParser args_parser;
    args_parser.add_option(every_terminal_process_flag, "Show every process associated with terminals", nullptr, 'a');
    args_parser.add_option(every_process_flag, "Show every process", nullptr, 'A');
    args_parser.add_option(every_process_flag, "Show every process (Equivalent to -A)", nullptr, 'e');
    args_parser.add_option(full_format_flag, "Full format", nullptr, 'f');
    args_parser.add_option(make_list_option(pid_list, "Show processes with a matching PID. (Comma- or space-separated list)", nullptr, 'p', "pid-list", [&](StringView pid_string) {
        provided_filtering_option = true;
        auto pid = pid_string.to_int();
        if (!pid.has_value())
            warnln("Could not parse '{}' as a PID.", pid_string);
        return pid;
    }));
    args_parser.add_option(make_list_option(parent_pid_list, "Show processes with a matching PPID. (Comma- or space-separated list.)", "ppid", {}, "pid-list", [&](StringView pid_string) {
        provided_filtering_option = true;
        auto pid = pid_string.to_int();
        if (!pid.has_value())
            warnln("Could not parse '{}' as a PID.", pid_string);
        return pid;
    }));
    args_parser.add_option(make_list_option(pid_list, "Show processes with a matching PID. (Comma- or space-separated list.) Processes will be listed in the order given.", nullptr, 'q', "pid-list", [&](StringView pid_string) {
        provided_quick_pid_list = true;
        auto pid = pid_string.to_int();
        if (!pid.has_value())
            warnln("Could not parse '{}' as a PID.", pid_string);
        return pid;
    }));
    args_parser.add_option(make_list_option(tty_list, "Show processes associated with the given terminal. (Comma- or space-separated list.) The short TTY name or the full device path may be used.", "tty", 't', "tty-list", [&](StringView tty_string) -> Optional<DeprecatedString> {
        provided_filtering_option = true;
        auto tty_pseudo_name_or_error = parse_tty_pseudo_name(tty_string);
        if (tty_pseudo_name_or_error.is_error()) {
            warnln("Could not parse '{}' as a TTY", tty_string);
            return {};
        }
        return tty_pseudo_name_or_error.release_value().to_deprecated_string();
    }));
    args_parser.add_option(make_list_option(uid_list, "Show processes with a matching user ID or login name. (Comma- or space-separated list.)", nullptr, 'u', "user-list", [&](StringView user_string) -> Optional<uid_t> {
        provided_filtering_option = true;
        if (auto uid = user_string.to_uint<uid_t>(); uid.has_value()) {
            return uid.value();
        }

        auto maybe_account = Core::Account::from_name(user_string, Core::Account::Read::PasswdOnly);
        if (maybe_account.is_error()) {
            warnln("Could not find user '{}': {}", user_string, maybe_account.error());
            return {};
        }
        return maybe_account.value().uid();
    }));
    args_parser.parse(arguments);

    if (provided_filtering_option && provided_quick_pid_list) {
        warnln("The -q option cannot be combined with other filtering options.");
        return 1;
    }

    Vector<Column> columns;

    Optional<size_t> uid_column;
    Optional<size_t> pid_column;
    Optional<size_t> ppid_column;
    Optional<size_t> pgid_column;
    Optional<size_t> sid_column;
    Optional<size_t> state_column;
    Optional<size_t> tty_column;
    Optional<size_t> cmd_column;

    auto add_column = [&](auto title, auto alignment) {
        columns.unchecked_append({ title, alignment, 0, {} });
        return columns.size() - 1;
    };

    if (full_format_flag) {
        TRY(columns.try_ensure_capacity(8));
        uid_column = add_column("UID"_short_string, Alignment::Left);
        pid_column = add_column("PID"_short_string, Alignment::Right);
        ppid_column = add_column("PPID"_short_string, Alignment::Right);
        pgid_column = add_column("PGID"_short_string, Alignment::Right);
        sid_column = add_column("SID"_short_string, Alignment::Right);
        state_column = add_column("STATE"_short_string, Alignment::Left);
        tty_column = add_column("TTY"_short_string, Alignment::Left);
        cmd_column = add_column("CMD"_short_string, Alignment::Left);
    } else {
        TRY(columns.try_ensure_capacity(3));
        pid_column = add_column("PID"_short_string, Alignment::Right);
        tty_column = add_column("TTY"_short_string, Alignment::Left);
        cmd_column = add_column("CMD"_short_string, Alignment::Left);
    }

    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());

    auto& processes = all_processes.processes;

    // Filter
    Vector<Core::ProcessStatistics> filtered_processes;
    if (provided_quick_pid_list) {
        for (auto pid : pid_list) {
            auto maybe_process = processes.first_matching([=](auto const& process) { return process.pid == pid; });
            if (maybe_process.has_value())
                filtered_processes.append(maybe_process.release_value());
        }

        processes = move(filtered_processes);
    } else if (!every_process_flag) {
        for (auto const& process : processes) {
            // Default is to show processes from the current TTY
            if ((!provided_filtering_option && process.tty == this_pseudo_tty_name.bytes_as_string_view())
                || (!pid_list.is_empty() && pid_list.contains_slow(process.pid))
                || (!parent_pid_list.is_empty() && parent_pid_list.contains_slow(process.ppid))
                || (!uid_list.is_empty() && uid_list.contains_slow(process.uid))
                || (!tty_list.is_empty() && tty_list.contains_slow(process.tty))
                || (every_terminal_process_flag && !process.tty.is_empty())) {
                filtered_processes.append(process);
            }
        }

        processes = move(filtered_processes);
    }

    // Sort
    if (!provided_quick_pid_list)
        quick_sort(processes, [](auto& a, auto& b) { return a.pid < b.pid; });

    Vector<Vector<String>> rows;
    TRY(rows.try_ensure_capacity(1 + processes.size()));

    Vector<String> header;
    TRY(header.try_ensure_capacity(columns.size()));
    for (auto& column : columns)
        header.unchecked_append(column.title);
    rows.unchecked_append(move(header));

    for (auto const& process : processes) {
        Vector<String> row;
        TRY(row.try_resize(columns.size()));

        if (uid_column.has_value())
            row[*uid_column] = TRY(String::from_deprecated_string(process.username));
        if (pid_column.has_value())
            row[*pid_column] = TRY(String::number(process.pid));
        if (ppid_column.has_value())
            row[*ppid_column] = TRY(String::number(process.ppid));
        if (pgid_column.has_value())
            row[*pgid_column] = TRY(String::number(process.pgid));
        if (sid_column.has_value())
            row[*sid_column] = TRY(String::number(process.sid));
        if (tty_column.has_value())
            row[*tty_column] = process.tty == "" ? "n/a"_short_string : TRY(String::from_deprecated_string(process.tty));
        if (state_column.has_value())
            row[*state_column] = process.threads.is_empty()
                ? "Zombie"_short_string
                : TRY(String::from_deprecated_string(process.threads.first().state));
        if (cmd_column.has_value())
            row[*cmd_column] = TRY(String::from_deprecated_string(process.name));

        rows.unchecked_append(move(row));
    }

    for (size_t i = 0; i < columns.size(); i++) {
        auto& column = columns[i];
        for (auto& row : rows)
            column.width = max(column.width, static_cast<int>(row[i].code_points().length()));
    }

    for (auto& row : rows) {
        for (size_t i = 0; i < columns.size(); i++) {
            auto& column = columns[i];
            auto& cell_text = row[i];
            if (!column.width) {
                out("{}", cell_text);
                continue;
            }
            if (column.alignment == Alignment::Right)
                out("{1:>{0}} ", column.width, cell_text);
            else
                out("{1:{0}} ", column.width, cell_text);
            if (i != columns.size() - 1)
                out(" ");
        }
        outln();
    }

    return 0;
}
