/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <fcntl.h>
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

    auto add_column = [&](auto title, auto alignment, auto width) {
        columns.append({ title, alignment, width, {} });
        return columns.size() - 1;
    };

    if (full_format_flag) {
        uid_column = add_column("UID", Alignment::Left, 9);
        pid_column = add_column("PID", Alignment::Right, 5);
        ppid_column = add_column("PPID", Alignment::Right, 5);
        state_column = add_column("STATE", Alignment::Left, 12);
        tty_column = add_column("TTY", Alignment::Left, 6);
        cmd_column = add_column("CMD", Alignment::Left, 0);
    } else {
        pid_column = add_column("PID", Alignment::Right, 5);
        tty_column = add_column("TTY", Alignment::Left, 6);
        cmd_column = add_column("CMD", Alignment::Left, 0);
    }

    auto print_column = [](auto& column, auto& string) {
        if (!column.width) {
            printf("%s", string.characters());
            return;
        }
        if (column.alignment == Alignment::Right)
            printf("%*s ", column.width, string.characters());
        else
            printf("%-*s ", column.width, string.characters());
    };

    for (auto& column : columns)
        print_column(column, column.title);
    printf("\n");

    auto processes = Core::ProcessStatisticsReader::get_all();
    if (!processes.has_value())
        return 1;

    quick_sort(processes.value(), [](auto& a, auto& b) { return a.pid < b.pid; });

    for (auto const& process : processes.value()) {
        auto tty = process.tty;

        if (!every_process_flag && tty != this_tty)
            continue;

        if (tty.starts_with("/dev/"))
            tty = tty.characters() + 5;
        else
            tty = "n/a";

        auto* state = process.threads.is_empty() ? "Zombie" : process.threads.first().state.characters();

        if (uid_column != -1)
            columns[uid_column].buffer = process.username;
        if (pid_column != -1)
            columns[pid_column].buffer = String::number(process.pid);
        if (ppid_column != -1)
            columns[ppid_column].buffer = String::number(process.ppid);
        if (tty_column != -1)
            columns[tty_column].buffer = tty;
        if (state_column != -1)
            columns[state_column].buffer = state;
        if (cmd_column != -1)
            columns[cmd_column].buffer = process.name;

        for (auto& column : columns)
            print_column(column, column.buffer);
        printf("\n");
    }

    return 0;
}
