/*
 * Copyright (c) 2019-2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

static void spawn_command(const Vector<const char*>& command, const ByteBuffer& data, const char* state)
{
    int pipefd[2];
    if (pipe(pipefd) < 0) {
        perror("pipe");
        return;
    }

    pid_t pid = fork();
    if (pid < 0) {
        perror("fork");
        return;
    } else if (pid == 0) {
        // We're the child.
        dup2(pipefd[0], 0);
        close(pipefd[0]);
        close(pipefd[1]);
        setenv("CLIPBOARD_STATE", state, true);
        execvp(command[0], const_cast<char**>(command.data()));
        perror("exec");
        exit(1);
    }

    // We're the parent.
    close(pipefd[0]);
    FILE* f = fdopen(pipefd[1], "w");
    fwrite(data.data(), data.size(), 1, f);
    if (ferror(f))
        warnln("failed to write data to the pipe: {}", strerror(ferror(f)));
    fclose(f);

    if (wait(nullptr) < 0)
        perror("wait");
}

int main(int argc, char* argv[])
{
    bool print_type = false;
    bool no_newline = false;
    bool watch = false;
    Vector<const char*> watch_command;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Paste from the clipboard to stdout.");
    args_parser.add_option(print_type, "Display the copied type", "print-type", 0);
    args_parser.add_option(no_newline, "Do not append a newline", "no-newline", 'n');
    args_parser.add_option(watch, "Run a command when clipboard data changes", "watch", 'w');
    args_parser.add_positional_argument(watch_command, "Command to run in watch mode", "command", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    auto& clipboard = GUI::Clipboard::the();

    if (watch) {
        watch_command.append(nullptr);

        clipboard.on_change = [&](const String&) {
            // Technically there's a race here...
            auto data_and_type = clipboard.fetch_data_and_type();
            if (data_and_type.mime_type.is_null()) {
                spawn_command(watch_command, {}, "clear");
            } else {
                spawn_command(watch_command, data_and_type.data, "data");
            }
        };

        // Trigger it the first time immediately.
        clipboard.on_change({});

        return app->exec();
    }

    auto data_and_type = clipboard.fetch_data_and_type();

    if (data_and_type.mime_type.is_null()) {
        warnln("Nothing copied");
        return 1;
    }

    if (!print_type) {
        out("{}", StringView(data_and_type.data));
        // Append a newline to text contents, unless the caller says otherwise.
        if (data_and_type.mime_type.starts_with("text/") && !no_newline)
            outln();
    } else {
        outln("{}", data_and_type.mime_type);
    }

    return 0;
}
