/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>
#include <fcntl.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/wait.h>

static NonnullRefPtrVector<Core::Notifier> notifiers;
static Vector<pid_t> started_job_ids;

static Vector<String> prepare_arguments(const Vector<const char*> arguments, const String& filename, const String& path)
{
    Vector<String> prepared_arguments;
    for (auto& arg : arguments) {
        if (StringView { "/_" } == arg)
            prepared_arguments.append(filename);
        else if (StringView { "//_" } == arg)
            prepared_arguments.append(path);
        else
            prepared_arguments.append(arg);
    }
    return prepared_arguments;
}

int main(int argc, char* argv[])
{
    const char* shell = getenv("SHELL");
    if (!shell)
        shell = "/bin/Shell";

    bool verbose = false;
    bool clear = false;
    bool force_restart = false;
    const char* executable_name = nullptr;
    Vector<const char*> arguments;

    Core::ArgsParser args_parser;
    args_parser.add_option(verbose, "Be verbose (print events)", "verbose", 'v');
    args_parser.add_option(clear, "Clear previous output before printing new output", "clear", 'c');
    args_parser.add_option(force_restart, "Forcefully kill previous instance (for a given file) when changes occur", "restart", 'r');
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = false,
        .help_string = "Execute the command in a shell",
        .long_name = "in-shell",
        .short_name = 's',
        .accept_value = [&](auto) {
            executable_name = shell;
            arguments.prepend("-c");
            return true;
        },
    });
    args_parser.add_positional_argument(arguments, "Command to execute", "command");
    args_parser.parse(argc, argv);

    if (executable_name == nullptr)
        executable_name = arguments.take_first();

    Vector<String> str_arguments;
    str_arguments.ensure_capacity(arguments.size());
    for (auto& arg : arguments)
        str_arguments.unchecked_append(arg);

    Core::EventLoop loop;

    auto file = Core::File::stdin();
    for (;;) {
        auto line = file->read_line(PATH_MAX);
        if (line.is_null())
            break;

        auto line_view = StringView { line.data(), line.size() };
        String filename = line_view.trim_whitespace(TrimMode::Right);
        auto real_path = Core::File::real_path_for(filename);

        if (verbose)
            warnln("[path] {} is {}", filename, real_path);

        auto watch_fd = watch_file(real_path.characters(), real_path.length());
        if (watch_fd < 0) {
            perror("watch_file");
            return 0;
        }
        fcntl(watch_fd, F_SETFD, FD_CLOEXEC);
        auto notifier = Core::Notifier::construct(watch_fd, Core::Notifier::Event::Read);
        if (verbose)
            warnln("[watch_file] watching {} with fd={}", filename, watch_fd);

        notifier->on_ready_to_read = [notifier, verbose, clear, force_restart, path = real_path, &executable_name, str_arguments = prepare_arguments(arguments, filename, real_path), m_old_pid = Optional<pid_t> {}] {
            auto& mutable_old_pid = const_cast<Optional<pid_t>&>(m_old_pid);
            if (clear)
                out("\x1b[H\x1b[2J\x1b[3J");
            char buffer[32];
            int rc = read(notifier->fd(), buffer, sizeof(buffer));
            ASSERT(rc >= 0);

            if (verbose)
                warnln("[notify] Detected change in {}", path);

            if (verbose) {
                StringBuilder builder;
                builder.join(' ', str_arguments);
                warnln("[exec] exec {} with ({})", executable_name, builder.string_view());
            }

            if (m_old_pid.has_value()) {
                if (force_restart) {
                    if (verbose)
                        warnln("[restart] Killing previous instance for {} with pid={}", path, m_old_pid.value());
                    kill(m_old_pid.value(), SIGTERM);
                }
                int wstatus = 0;
                waitpid(m_old_pid.value(), &wstatus, 0);
                mutable_old_pid.clear();
            }

            Vector<const char*> argv;
            argv.append(executable_name);
            for (auto arg : str_arguments)
                argv.append(arg.characters());
            argv.append(nullptr);

            pid_t pid;
            if ((errno = posix_spawnp(&pid, executable_name, nullptr, nullptr, const_cast<char**>(argv.data()), environ))) {
                perror("posix_spawn");
                exit(1);
            }
            started_job_ids.append(pid);
            mutable_old_pid = pid;
            if (verbose)
                warnln("[exec] created instance for {} with pid={}", path, pid);
        };

        notifiers.empend(move(notifier));
    }

    Core::EventLoop::register_signal(SIGCHLD, [&](auto) {
        Vector<pid_t> new_pids;
        for (auto& pid : started_job_ids) {
            int wstatus = 0;
            if (waitpid(pid, &wstatus, WNOWAIT) == pid) {
                if (WIFEXITED(wstatus) || WIFSIGNALED(wstatus)) {
                    if (verbose)
                        warnln("[exec] pid {} died with exit code {}", pid, WEXITSTATUS(wstatus));
                    continue;
                }
            }
            new_pids.append(pid);
        }
        started_job_ids = move(new_pids);
    });

    atexit([] {
        for (auto& notifier : notifiers) {
            notifier.set_enabled(false);
            close(notifier.fd());
            notifier.close();
        }
    });

    return loop.exec();
}
