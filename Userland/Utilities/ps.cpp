/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/sysmacros.h>
#include <unistd.h>

static ErrorOr<String> determine_tty_pseudo_name()
{
    auto tty_stat = TRY(Core::System::fstat(STDIN_FILENO));
    int tty_device_major = major(tty_stat.st_rdev);
    int tty_device_minor = minor(tty_stat.st_rdev);

    if (tty_device_major == 201) {
        return String::formatted("pts:{}", tty_device_minor);
    }

    if (tty_device_major == 4) {
        return String::formatted("tty:{}", tty_device_minor);
    }
    return "n/a"_short_string;
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
    Vector<pid_t> pid_list;

    Core::ArgsParser args_parser;
    args_parser.add_option(every_terminal_process_flag, "Show every process associated with terminals", nullptr, 'a');
    args_parser.add_option(every_process_flag, "Show every process", nullptr, 'A');
    args_parser.add_option(every_process_flag, "Show every process (Equivalent to -A)", nullptr, 'e');
    args_parser.add_option(full_format_flag, "Full format", nullptr, 'f');
    args_parser.add_option(make_list_option(pid_list, "A comma- or space-separated list of PIDs. Only processes matching those PIDs will be selected", nullptr, 'q', "pid-list", [](StringView pid_string) {
        auto pid = pid_string.to_int();
        if (!pid.has_value())
            warnln("Could not parse '{}' as a PID.", pid_string);
        return pid;
    }));
    args_parser.parse(arguments);

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

    if (!pid_list.is_empty()) {
        every_process_flag = true;

        processes.remove_all_matching([&](auto& a) { return !pid_list.contains_slow(a.pid); });

        auto processes_sort_predicate = [&pid_list](auto& a, auto& b) {
            return pid_list.find_first_index(a.pid).value() < pid_list.find_first_index(b.pid).value();
        };
        quick_sort(processes, processes_sort_predicate);
    } else {
        quick_sort(processes, [](auto& a, auto& b) { return a.pid < b.pid; });
    }

    Vector<Vector<String>> rows;
    TRY(rows.try_ensure_capacity(1 + processes.size()));

    Vector<String> header;
    TRY(header.try_ensure_capacity(columns.size()));
    for (auto& column : columns)
        header.unchecked_append(column.title);
    rows.unchecked_append(move(header));

    for (auto const& process : processes) {
        auto tty = TRY(String::from_deprecated_string(process.tty));
        if (every_process_flag) {
            // Don't skip any.
        } else if (every_terminal_process_flag) {
            if (tty.is_empty())
                continue;
        } else if (tty != this_pseudo_tty_name) {
            continue;
        }

        Vector<String> row;
        TRY(row.try_resize(columns.size()));

        if (tty == "")
            tty = "n/a"_short_string;

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
            row[*tty_column] = tty;
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
