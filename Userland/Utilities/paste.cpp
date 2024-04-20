/*
 * Copyright (c) 2019-2021, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Zachary Penn <zack@sysdevs.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Environment.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>

static void spawn_command(Span<StringView> command, ByteBuffer const& data, char const* state)
{
    auto pipefd = MUST(Core::System::pipe2(0));
    pid_t pid = MUST(Core::System::fork());

    if (pid == 0) {
        // We're the child.
        MUST(Core::System::dup2(pipefd[0], 0));
        MUST(Core::System::close(pipefd[0]));
        MUST(Core::System::close(pipefd[1]));
        MUST(Core::Environment::set("CLIPBOARD_STATE"sv, { state, strlen(state) }, Core::Environment::Overwrite::Yes));
        MUST(Core::System::exec(command[0], command, Core::System::SearchInPath::Yes));
        perror("exec");
        exit(1);
    }

    // We're the parent.
    MUST(Core::System::close(pipefd[0]));
    FILE* f = fdopen(pipefd[1], "w");
    fwrite(data.data(), data.size(), 1, f);

    if (ferror(f))
        warnln("failed to write data to the pipe: {}", strerror(ferror(f)));

    fclose(f);

    if (wait(nullptr) < 0)
        perror("wait");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool print_type = false;
    bool no_newline = false;
    bool watch = false;
    Vector<StringView> watch_command;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Paste from the clipboard to stdout.");
    args_parser.add_option(print_type, "Display the copied type", "print-type");
    args_parser.add_option(no_newline, "Do not append a newline", "no-newline", 'n');
    args_parser.add_option(watch, "Run a command when clipboard data changes", "watch", 'w');
    args_parser.add_positional_argument(watch_command, "Command to run in watch mode", "command", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));

    auto& clipboard = GUI::Clipboard::the();

    if (watch) {
        watch_command.append({});

        clipboard.on_change = [&](ByteString const&) {
            // Technically there's a race here...
            auto data_and_type = clipboard.fetch_data_and_type();
            if (data_and_type.mime_type.is_empty()) {
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

    if (data_and_type.mime_type.is_empty()) {
        warnln("Nothing copied");
        return 1;
    }

    if (!print_type) {
        out("{}", StringView(data_and_type.data));
        // Append a newline to text contents, unless the caller says otherwise.
        if (data_and_type.mime_type.starts_with("text/"sv) && !no_newline)
            outln();
    } else {
        outln("{}", data_and_type.mime_type);
    }

    return 0;
}
