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

SavedFileDescriptors::SavedFileDescriptors(const NonnullRefPtrVector<AST::Rewiring>& intended_rewirings)
{
    for (auto& rewiring : intended_rewirings) {
        int new_fd = dup(rewiring.source_fd);
        if (new_fd < 0) {
            if (errno != EBADF)
                perror("dup");
            // The fd that will be overwritten isn't open right now,
            // it will be cleaned up by the exec()-side collector
            // and we have nothing to do here, so just ignore this error.
            continue;
        }

        auto flags = fcntl(new_fd, F_GETFL);
        auto rc = fcntl(new_fd, F_SETFL, flags | FD_CLOEXEC);
        ASSERT(rc == 0);

        m_saves.append({ rewiring.source_fd, new_fd });
        m_collector.add(new_fd);
    }
}

SavedFileDescriptors::~SavedFileDescriptors()
{
    for (auto& save : m_saves) {
        if (dup2(save.saved, save.original) < 0) {
            perror("dup2(~SavedFileDescriptors)");
            continue;
        }
    }
}

void Shell::setup_signals()
{
    Core::EventLoop::register_signal(SIGCHLD, [](int) {
        auto& jobs = s_shell->jobs;
        Vector<u64> disowned_jobs;
        for (auto& it : jobs) {
            auto job_id = it.key;
            auto& job = *it.value;
            int wstatus = 0;
            auto child_pid = waitpid(job.pid(), &wstatus, WNOHANG | WUNTRACED);
            if (child_pid < 0) {
                if (errno == ECHILD) {
                    // The child process went away before we could process its death, just assume it exited all ok.
                    // FIXME: This should never happen, the child should stay around until we do the waitpid above.
                    dbg() << "Child process gone, cannot get exit code for " << job_id;
                    child_pid = job.pid();
                } else {
                    ASSERT_NOT_REACHED();
                }
            }
#ifndef __serenity__
            if (child_pid == 0) {
                // Linux: if child didn't "change state", but existed.
                continue;
            }
#endif
            if (child_pid == job.pid()) {
                if (WIFSIGNALED(wstatus) && !WIFSTOPPED(wstatus)) {
                    job.set_signalled(WTERMSIG(wstatus));
                } else if (WIFEXITED(wstatus)) {
                    job.set_has_exit(WEXITSTATUS(wstatus));
                } else if (WIFSTOPPED(wstatus)) {
                    job.unblock();
                    job.set_is_suspended(true);
                }
            }
            if (job.should_be_disowned())
                disowned_jobs.append(job_id);
        }
        for (auto job_id : disowned_jobs)
            jobs.remove(job_id);
    });

    Core::EventLoop::register_signal(SIGTSTP, [](auto) {
        auto job = s_shell->current_job();
        s_shell->kill_job(job, SIGTSTP);
        if (job) {
            job->set_is_suspended(true);
            job->unblock();
        }
    });
}

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

    editor = Line::Editor::construct();

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
    Vector<const char*> script_args;
    bool skip_rc_files = false;

    Core::ArgsParser parser;
    parser.add_option(command_to_run, "String to read commands from", "command-string", 'c', "command-string");
    parser.add_option(skip_rc_files, "Skip running shellrc files", "skip-shellrc", 0);
    parser.add_positional_argument(file_to_read_from, "File to read commands from", "file", Core::ArgsParser::Required::No);
    parser.add_positional_argument(script_args, "Extra argumets to pass to the script (via $* and co)", "argument", Core::ArgsParser::Required::No);

    parser.parse(argc, argv);

    if (getsid(getpid()) == 0) {
        if (setsid() < 0) {
            perror("setsid");
            // Let's just hope that it's ok.
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
        run_rc_file(Shell::global_init_file_path);
        run_rc_file(Shell::local_init_file_path);
    }

    {
        Vector<String> args;
        for (auto* arg : script_args)
            args.empend(arg);
        shell->set_local_variable("ARGV", adopt(*new AST::ListValue(move(args))));
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

    Core::EventLoop::current().post_event(*shell, make<Core::CustomEvent>(Shell::ShellEventType::ReadLine));

    return loop.exec();
}
