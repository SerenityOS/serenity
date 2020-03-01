/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
        uid_column = add_column("UID", Alignment::Left, 8);
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

    auto all_processes = Core::ProcessStatisticsReader::get_all();

    for (const auto& it : all_processes) {
        const auto& proc = it.value;
        auto tty = proc.tty;

        if (!every_process_flag && tty != this_tty)
            continue;

        if (tty.starts_with("/dev/"))
            tty = tty.characters() + 5;
        else
            tty = "n/a";

        auto* state = proc.threads.is_empty() ? "Zombie" : proc.threads.first().state.characters();

        if (uid_column != -1)
            columns[uid_column].buffer = proc.username;
        if (pid_column != -1)
            columns[pid_column].buffer = String::number(proc.pid);
        if (ppid_column != -1)
            columns[ppid_column].buffer = String::number(proc.ppid);
        if (tty_column != -1)
            columns[tty_column].buffer = tty;
        if (state_column != -1)
            columns[state_column].buffer = state;
        if (cmd_column != -1)
            columns[cmd_column].buffer = proc.name;

        for (auto& column : columns)
            print_column(column, column.buffer);
        printf("\n");
    }

    return 0;
}
