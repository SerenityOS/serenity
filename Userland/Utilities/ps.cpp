/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    String this_tty = ttyname(STDIN_FILENO);

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc/all", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

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

    Core::ArgsParser args_parser;
    args_parser.add_option(every_process_flag, "Show every process", nullptr, 'e');
    args_parser.add_option(full_format_flag, "Full format", nullptr, 'f');
    args_parser.parse(argc, argv);

    Vector<Column> columns;

    int uid_column = -1;
    int pid_column = -1;
    int ppid_column = -1;
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
        state_column = add_column("STATE", Alignment::Left);
        tty_column = add_column("TTY", Alignment::Left);
        cmd_column = add_column("CMD", Alignment::Left);
    } else {
        pid_column = add_column("PID", Alignment::Right);
        tty_column = add_column("TTY", Alignment::Left);
        cmd_column = add_column("CMD", Alignment::Left);
    }

    auto processes = Core::ProcessStatisticsReader::get_all();
    if (!processes.has_value())
        return 1;

    quick_sort(processes.value(), [](auto& a, auto& b) { return a.pid < b.pid; });

    Vector<Vector<String>> rows;
    rows.ensure_capacity(1 + processes.value().size());

    Vector<String> header;
    header.ensure_capacity(columns.size());
    for (auto& column : columns)
        header.append(column.title);
    rows.append(move(header));

    for (auto const& process : processes.value()) {
        auto tty = process.tty;

        if (!every_process_flag && tty != this_tty)
            continue;

        if (tty.starts_with("/dev/"))
            tty = tty.characters() + 5;
        else
            tty = "n/a";

        auto* state = process.threads.is_empty() ? "Zombie" : process.threads.first().state.characters();

        Vector<String> row;
        row.resize(columns.size());

        if (uid_column != -1)
            row[uid_column] = process.username;
        if (pid_column != -1)
            row[pid_column] = String::number(process.pid);
        if (ppid_column != -1)
            row[ppid_column] = String::number(process.ppid);
        if (tty_column != -1)
            row[tty_column] = tty;
        if (state_column != -1)
            row[state_column] = state;
        if (cmd_column != -1)
            row[cmd_column] = process.name;

        rows.append(move(row));
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
