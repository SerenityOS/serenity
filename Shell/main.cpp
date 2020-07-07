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
#include <sys/wait.h>

RefPtr<Line::Editor> editor;
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

    signal(SIGINT, [](int) {
        if (!s_shell->is_accepting_signals())
            return;
        editor->interrupted();
    });

    signal(SIGWINCH, [](int) {
        editor->resized();
    });

    signal(SIGTTIN, [](int) {});
    signal(SIGTTOU, [](int) {});

    signal(SIGHUP, [](int) {
        if (!s_shell->is_accepting_signals())
            return;
        s_shell->save_history();
    });

    signal(SIGCHLD, [](int) {
        auto& jobs = s_shell->jobs;
        Vector<u64> disowned_jobs;
        for (auto& job : jobs) {
            int wstatus = 0;
            auto child_pid = waitpid(job.value->pid(), &wstatus, WNOHANG);
            if (child_pid < 0) {
                if (errno == ECHILD) {
                    // The child process went away before we could process its death, just assume it exited all ok.
                    // FIXME: This should never happen, the child should stay around until we do the waitpid above.
                    dbg() << "Child process gone, cannot get exit code for " << job.key;
                    child_pid = job.value->pid();
                } else {
                    ASSERT_NOT_REACHED();
                }
            }
#ifndef __serenity__
            if (child_pid == 0) {
                // Linux: if child didn't "change state", but existed.
                child_pid = job.value->pid();
            }
#endif
            if (child_pid == job.value->pid()) {
                if (WIFEXITED(wstatus)) {
                    job.value->set_has_exit(WEXITSTATUS(wstatus));
                } else if (WIFSIGNALED(wstatus) && !WIFSTOPPED(wstatus)) {
                    job.value->set_has_exit(126);
                }
            }
            if (job.value->should_be_disowned())
                disowned_jobs.append(job.key);
        }
        for (auto key : disowned_jobs)
            jobs.remove(key);
    });

    // Ignore SIGTSTP as the shell should not be suspended with ^Z.
    signal(SIGTSTP, [](auto) {});

#ifndef __serenity__
    sigset_t blocked;
    sigemptyset(&blocked);
    sigaddset(&blocked, SIGTTOU);
    pthread_sigmask(SIG_BLOCK, &blocked, NULL);
#endif
#ifdef __serenity__
    if (pledge("stdio rpath wpath cpath proc exec tty accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    editor = Line::Editor::construct(Line::Configuration { Line::Configuration::UnescapedSpaces });

    auto shell = Shell::construct();
    s_shell = shell.ptr();

    editor->initialize();
    shell->termios = editor->termios();
    shell->default_termios = editor->default_termios();

    editor->on_display_refresh = [&](auto& editor) {
        editor.strip_styles();
        shell->highlight(editor);
    };
    editor->on_tab_complete = [&](const Line::Editor& editor) {
        return shell->complete(editor);
    };

    const char* command_to_run = nullptr;
    const char* file_to_read_from = nullptr;
    bool skip_rc_files = false;

    Core::ArgsParser parser;
    parser.add_option(command_to_run, "String to read commands from", "command-string", 'c', "command-string");
    parser.add_positional_argument(file_to_read_from, "File to read commands from", "file", Core::ArgsParser::Required::No);
    parser.add_option(skip_rc_files, "Skip running shellrc files", "skip-shellrc", 0);

    parser.parse(argc, argv);

    if (!skip_rc_files) {
        auto run_rc_file = [&](auto& name) {
            String file_path = name;
            if (file_path.starts_with('~'))
                file_path = shell->expand_tilde(file_path);
            if (Core::File::exists(file_path)) {
                shell->run_file(file_path, false);
            }
        };
        run_rc_file(Shell::global_init_file_path);
        run_rc_file(Shell::local_init_file_path);
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

    editor->on_interrupt_handled = [&] {
        editor->finish();
    };

    shell->add_child(*editor);

    Core::EventLoop::current().post_event(*shell, make<Core::CustomEvent>(Shell::ShellEventType::ReadLine));

    return loop.exec();
}
