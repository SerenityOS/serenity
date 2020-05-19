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
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>

Line::Editor editor { Line::Configuration { Line::Configuration::UnescapedSpaces } };
Shell* s_shell;

void FileDescriptionCollector::collect()
{
    for (auto fd : m_fds)
        close(fd);
    m_fds.clear();
}

FileDescriptionCollector::~FileDescriptionCollector()
{
    collect();
}

void FileDescriptionCollector::add(int fd)
{
    m_fds.append(fd);
}

int main(int argc, char** argv)
{
    Core::EventLoop loop;

    if (pledge("stdio rpath wpath cpath proc exec tty accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto shell = Shell::construct();
    s_shell = shell.ptr();

    editor.initialize();
    shell->termios = editor.termios();
    shell->default_termios = editor.default_termios();

    editor.on_display_refresh = [&](auto& editor) {
        editor.strip_styles();
        shell->highlight(editor);
    };
    editor.on_tab_complete = [&](const Line::Editor& editor) {
        return shell->complete(editor);
    };

    signal(SIGINT, [](int) {
        editor.interrupted();
    });

    signal(SIGWINCH, [](int) {
        editor.resized();
    });

    signal(SIGHUP, [](int) {
        s_shell->save_history();
    });

    signal(SIGCHLD, [](int) {
        auto& jobs = s_shell->jobs;
        for (auto& job : jobs) {
            int wstatus = 0;
            auto child_pid = waitpid(job.value->pid(), &wstatus, WNOHANG);
            if (child_pid == job.value->pid()) {
                if (WIFEXITED(wstatus) || WIFSIGNALED(wstatus)) {
                    Core::EventLoop::current().post_event(*s_shell, make<Core::CustomEvent>(Shell::ShellEventType::ChildExited, job.value));
                }
            }
        }
    });

    if (argc > 2 && !strcmp(argv[1], "-c")) {
        dbgprintf("sh -c '%s'\n", argv[2]);
        shell->run_command(argv[2]);
        return 0;
    }

    if (argc == 2 && argv[1][0] != '-') {
        auto file = Core::File::construct(argv[1]);
        if (!file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Failed to open %s: %s\n", file->filename().characters(), file->error_string());
            return 1;
        }
        for (;;) {
            auto line = file->read_line(4096);
            if (line.is_null())
                break;
            shell->run_command(String::copy(line, Chomp));
        }
        return 0;
    }

    editor.on_interrupt_handled = [&] {
        if (!shell->should_read_more()) {
            shell->finish_command();
            editor.finish();
        }
    };

    Core::EventLoop::current().post_event(*shell, make<Core::CustomEvent>(Shell::ShellEventType::ReadLine));

    return loop.exec();
}
