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

#include "Execution.h"
#include "Shell.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

RefPtr<Line::Editor> editor;
Shell::Shell* s_shell;

int main(int argc, char** argv)
{
    Core::EventLoop loop;

    Core::EventLoop::register_signal(SIGINT, [](int) {
        s_shell->kill_job(s_shell->current_job(), SIGINT);
    });

    Core::EventLoop::register_signal(SIGWINCH, [](int) {
        s_shell->kill_job(s_shell->current_job(), SIGWINCH);
    });

    Core::EventLoop::register_signal(SIGTTIN, [](int) {});
    Core::EventLoop::register_signal(SIGTTOU, [](int) {});

    Core::EventLoop::register_signal(SIGHUP, [](int) {
        for (auto& it : s_shell->jobs)
            s_shell->kill_job(it.value.ptr(), SIGHUP);

        s_shell->save_history();
    });

    editor = Line::Editor::construct();

    auto shell = Shell::Shell::construct(*editor);
    s_shell = shell.ptr();

    s_shell->setup_signals();

#ifndef __serenity__
    sigset_t blocked;
    sigemptyset(&blocked);
    sigaddset(&blocked, SIGTTOU);
    sigaddset(&blocked, SIGTTIN);
    pthread_sigmask(SIG_BLOCK, &blocked, NULL);
#endif
#ifdef __serenity__
    if (pledge("stdio rpath wpath cpath proc exec tty accept sigaction unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    editor->initialize();
    shell->termios = editor->termios();
    shell->default_termios = editor->default_termios();

    editor->on_display_refresh = [&](auto& editor) {
        editor.strip_styles();
        if (shell->should_format_live()) {
            auto line = editor.line();
            ssize_t cursor = editor.cursor();
            editor.clear_line();
            editor.insert(shell->format(line, cursor));
            if (cursor >= 0)
                editor.set_cursor(cursor);
        }
        shell->highlight(editor);
    };
    editor->on_tab_complete = [&](const Line::Editor&) {
        return shell->complete();
    };

    const char* command_to_run = nullptr;
    const char* file_to_read_from = nullptr;
    Vector<const char*> script_args;
    bool skip_rc_files = false;
    const char* format = nullptr;
    bool should_format_live = false;

    Core::ArgsParser parser;
    parser.add_option(command_to_run, "String to read commands from", "command-string", 'c', "command-string");
    parser.add_option(skip_rc_files, "Skip running shellrc files", "skip-shellrc", 0);
    parser.add_option(format, "Format the given file into stdout and exit", "format", 0, "file");
    parser.add_option(should_format_live, "Enable live formatting", "live-formatting", 'f');
    parser.add_positional_argument(file_to_read_from, "File to read commands from", "file", Core::ArgsParser::Required::No);
    parser.add_positional_argument(script_args, "Extra argumets to pass to the script (via $* and co)", "argument", Core::ArgsParser::Required::No);

    parser.parse(argc, argv);

    shell->set_live_formatting(should_format_live);

    if (format) {
        auto file = Core::File::open(format, Core::IODevice::ReadOnly);
        if (file.is_error()) {
            fprintf(stderr, "Error: %s", file.error().characters());
            return 1;
        }

        ssize_t cursor = -1;
        puts(shell->format(file.value()->read_all(), cursor).characters());
        return 0;
    }

    auto pid = getpid();
    if (auto sid = getsid(pid); sid == 0) {
        if (setsid() < 0) {
            perror("setsid");
            // Let's just hope that it's ok.
        }
    } else if (sid != pid) {
        if (getpgid(pid) != pid) {
            dbgln("We were already in a session with sid={} (we are {}), let's do some gymnastics", sid, pid);
            if (setpgid(pid, sid) < 0) {
                auto strerr = strerror(errno);
                dbgln("couldn't setpgid: {}", strerr);
            }
            if (setsid() < 0) {
                auto strerr = strerror(errno);
                dbgln("couldn't setsid: {}", strerr);
            }
        }
    }

    shell->current_script = argv[0];

    if (!skip_rc_files) {
        auto run_rc_file = [&](auto& name) {
            String file_path = name;
            if (file_path.starts_with('~'))
                file_path = shell->expand_tilde(file_path);
            if (Core::File::exists(file_path)) {
                shell->run_file(file_path, false);
            }
        };
        run_rc_file(Shell::Shell::global_init_file_path);
        run_rc_file(Shell::Shell::local_init_file_path);
    }

    {
        Vector<String> args;
        for (auto* arg : script_args)
            args.empend(arg);
        shell->set_local_variable("ARGV", adopt(*new Shell::AST::ListValue(move(args))));
    }

    if (command_to_run) {
        dbgprintf("sh -c '%s'\n", command_to_run);
        shell->run_command(command_to_run);
        return 0;
    }

    if (file_to_read_from && StringView { "-" } != file_to_read_from) {
        if (shell->run_file(file_to_read_from))
            return 0;
        return 1;
    }

    shell->add_child(*editor);

    Core::EventLoop::current().post_event(*shell, make<Core::CustomEvent>(Shell::Shell::ShellEventType::ReadLine));

    return loop.exec();
}
