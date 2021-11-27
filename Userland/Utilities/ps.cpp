/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty"));
    String this_tty = ttyname(STDIN_FILENO);

    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/proc/all", "r"));
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
    bool full_format_flag = false;
    String pid_list;

    Core::ArgsParser args_parser;
    args_parser.add_option(every_process_flag, "Show every process", nullptr, 'e');
    args_parser.add_option(full_format_flag, "Full format", nullptr, 'f');
    args_parser.add_option(pid_list, "A comma-separated list of PIDs. Only processes matching those PIDs will be selected", nullptr, 'q', "pid-list");
    args_parser.parse(arguments);

    Vector<Column> columns;

    int uid_column = -1;
    int pid_column = -1;
    int ppid_column = -1;
    int pgid_column = -1;
    int sid_column = -1;
    int state_column = -1;
    int tty_column = -1;
    int cmd_column = -1;

    auto add_column = [&](auto title, auto alignment) {
        columns.append({ title, alignment, 0, {} });
        return columns.size() - 1;
    };

    if (full_format_flag) {
        uid_column = add_column("UID", Alignment::Left);
        pid_column = add_column("PID", Alignment::Right);
        ppid_column = add_column("PPID", Alignment::Right);
        pgid_column = add_column("PGID", Alignment::Right);
        sid_column = add_column("SID", Alignment::Right);
        state_column = add_column("STATE", Alignment::Left);
        tty_column = add_column("TTY", Alignment::Left);
        cmd_column = add_column("CMD", Alignment::Left);
    } else {
        pid_column = add_column("PID", Alignment::Right);
        tty_column = add_column("TTY", Alignment::Left);
        cmd_column = add_column("CMD", Alignment::Left);
    }

    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.has_value())
        return 1;

    auto& processes = all_processes.value().processes;

    if (!pid_list.is_empty()) {
        every_process_flag = true;
        auto string_parts = pid_list.split_view(',');
        Vector<pid_t> selected_pids;
        selected_pids.ensure_capacity(string_parts.size());

        for (size_t i = 0; i < string_parts.size(); i++) {
            auto pid = string_parts[i].to_int();

            if (!pid.has_value()) {
                warnln("Invalid value for -q: {}", pid_list);
                warnln("Could not parse '{}' as a PID.", string_parts[i]);
                return 1;
            }

            selected_pids.append(pid.value());
        }

        processes.remove_all_matching([&](auto& a) { return selected_pids.find(a.pid) == selected_pids.end(); });

        auto processes_sort_predicate = [&selected_pids](auto& a, auto& b) {
            return selected_pids.find_first_index(a.pid).value() < selected_pids.find_first_index(b.pid).value();
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
    rows.append(move(header));

    for (auto const& process : processes) {
        auto tty = process.tty;

        if (!every_process_flag && tty != this_tty)
            continue;

        if (tty.starts_with("/dev/"))
            tty = tty.characters() + 5;
        else
            tty = "n/a";

        auto* state = process.threads.is_empty() ? "Zombie" : process.threads.first().state.characters();

        Vector<String> row;
        TRY(row.try_resize(columns.size()));

        if (uid_column != -1)
            row[uid_column] = process.username;
        if (pid_column != -1)
            row[pid_column] = String::number(process.pid);
        if (ppid_column != -1)
            row[ppid_column] = String::number(process.ppid);
        if (pgid_column != -1)
            row[pgid_column] = String::number(process.pgid);
        if (sid_column != -1)
            row[sid_column] = String::number(process.sid);
        if (tty_column != -1)
            row[tty_column] = tty;
        if (state_column != -1)
            row[state_column] = state;
        if (cmd_column != -1)
            row[cmd_column] = process.name;

        TRY(rows.try_append(move(row)));
    }

    for (size_t i = 0; i < columns.size(); i++) {
        auto& column = columns[i];
        for (auto& row : rows)
            column.width = max(column.width, static_cast<int>(row[i].length()));
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
