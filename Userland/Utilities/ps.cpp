/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copytight (c) 2021, Maxime Friess <M4x1me@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Tuple.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <stdio.h>
#include <unistd.h>

#define TREE_SPACING 2

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
    bool show_as_tree = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(every_process_flag, "Show every process", nullptr, 'e');
    args_parser.add_option(full_format_flag, "Full format", nullptr, 'f');
    args_parser.add_option(show_as_tree, "Show processes as a tree", nullptr, 't');
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

    Vector<Vector<String>> rows;

    auto add_row = [&](auto process, auto offset) {
        auto tty = process.tty;

        if (!every_process_flag && tty != this_tty)
            return false;

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
            row[cmd_column] = String::formatted("{1:>{0}}", TREE_SPACING * offset + process.name.length(), process.name);

        rows.append(move(row));
        return true;
    };

    Vector<String> header;
    header.ensure_capacity(columns.size());
    for (auto& column : columns)
        header.append(column.title);

    if (show_as_tree) {
        auto processes = Core::ProcessStatisticsReader::get_all_tree();
        if (!processes.has_value())
            return 1;

        processes.value().root().sort([](auto& a, auto& b) { return a.pid < b.pid; });

        rows.ensure_capacity(1 + processes.value().root().size());
        rows.append(move(header));

        size_t min_depth = SIZE_MAX;
        if (!every_process_flag) {
            // We first have to iterate once, to find the minimum depth that will appear, to compensate for it
            // in the display.
            // TODO: I think we could do better.
            Function<void(TreeNode<Core::ProcessStatistics, 0>&, size_t)> find_min = [&](TreeNode<Core::ProcessStatistics, 0>& node, size_t depth) {
                auto tty = node.value().tty;

                if (every_process_flag || tty == this_tty)
                    min_depth = min(min_depth, depth);

                for (size_t i = 0; i < node.num_children(); i++) {
                    find_min(*node.child_at(i), depth + 1);
                }
            };
            find_min(processes.value().root(), 0);
        } else {
            min_depth = 0;
        }

        // We have to use Function here, because the lambda calls itself.
        // TODO: Improve that ?
        Function<void(TreeNode<Core::ProcessStatistics, 0>&, size_t)> add_treenode = [&](TreeNode<Core::ProcessStatistics, 0>& node, size_t depth) {
            add_row(node.value(), depth - min_depth);

            for (size_t i = 0; i < node.num_children(); i++) {
                add_treenode(*node.child_at(i), depth + 1);
            }
        };

        add_treenode(processes.value().root(), 0);

    } else {
        auto processes = Core::ProcessStatisticsReader::get_all();
        if (!processes.has_value())
            return 1;

        quick_sort(processes.value(), [](auto& a, auto& b) { return a.pid < b.pid; });

        rows.ensure_capacity(1 + processes.value().size());
        rows.append(move(header));

        for (auto const& process : processes.value()) {
            add_row(process, 0);
        }
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
