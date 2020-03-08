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

#include "TerminalWrapper.h"
#include "ProcessStateWidget.h"
#include <AK/String.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/MessageBox.h>
#include <LibVT/TerminalWidget.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

void TerminalWrapper::run_command(const String& command)
{
    if (m_pid != -1) {
        GUI::MessageBox::show(
            "A command is already running in this TerminalWrapper",
            "Can't run command",
            GUI::MessageBox::Type::Error,
            GUI::MessageBox::InputType::OK,
            window());
        return;
    }

    int ptm_fd = posix_openpt(O_RDWR | O_CLOEXEC);
    if (ptm_fd < 0) {
        perror("posix_openpt");
        ASSERT_NOT_REACHED();
    }
    if (grantpt(ptm_fd) < 0) {
        perror("grantpt");
        ASSERT_NOT_REACHED();
    }
    if (unlockpt(ptm_fd) < 0) {
        perror("unlockpt");
        ASSERT_NOT_REACHED();
    }

    m_terminal_widget->set_pty_master_fd(ptm_fd);
    m_terminal_widget->on_command_exit = [this] {
        int wstatus;
        int rc = waitpid(m_pid, &wstatus, 0);
        if (rc < 0) {
            perror("waitpid");
            ASSERT_NOT_REACHED();
        }
        if (WIFEXITED(wstatus)) {
            m_terminal_widget->inject_string(String::format("\033[%d;1m(Command exited with code %d)\033[0m\n", wstatus == 0 ? 32 : 31, WEXITSTATUS(wstatus)));
        } else if (WIFSTOPPED(wstatus)) {
            m_terminal_widget->inject_string(String::format("\033[34;1m(Command stopped!)\033[0m\n"));
        } else if (WIFSIGNALED(wstatus)) {
            m_terminal_widget->inject_string(String::format("\033[34;1m(Command signaled with %s!)\033[0m\n", strsignal(WTERMSIG(wstatus))));
        }
        m_process_state_widget->set_tty_fd(-1);
        m_pid = -1;

        if (on_command_exit)
            on_command_exit();
    };

    m_pid = fork();
    if (m_pid == 0) {
        // Create a new process group.
        setsid();

        const char* tty_name = ptsname(ptm_fd);
        if (!tty_name) {
            perror("ptsname");
            exit(1);
        }
        close(ptm_fd);
        int pts_fd = open(tty_name, O_RDWR);
        if (pts_fd < 0) {
            perror("open");
            exit(1);
        }

        // NOTE: It's okay if this fails.
        (void)ioctl(0, TIOCNOTTY);

        close(0);
        close(1);
        close(2);

        int rc = dup2(pts_fd, 0);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = dup2(pts_fd, 1);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = dup2(pts_fd, 2);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = close(pts_fd);
        if (rc < 0) {
            perror("close");
            exit(1);
        }
        rc = ioctl(0, TIOCSCTTY);
        if (rc < 0) {
            perror("ioctl(TIOCSCTTY)");
            exit(1);
        }

        setenv("TERM", "xterm", true);

        auto parts = command.split(' ');
        ASSERT(!parts.is_empty());
        const char** args = (const char**)calloc(parts.size() + 1, sizeof(const char*));
        for (size_t i = 0; i < parts.size(); i++) {
            args[i] = parts[i].characters();
        }
        rc = execvp(args[0], const_cast<char**>(args));
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        ASSERT_NOT_REACHED();
    }

    // Parent process, cont'd.
    m_process_state_widget->set_tty_fd(ptm_fd);
}

void TerminalWrapper::kill_running_command()
{
    ASSERT(m_pid != -1);

    // Kill our child process and its whole process group.
    (void)killpg(m_pid, SIGTERM);
}

TerminalWrapper::TerminalWrapper()
{
    set_layout<GUI::VerticalBoxLayout>();

    RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("Terminal");
    m_terminal_widget = add<TerminalWidget>(-1, false, config);
    m_process_state_widget = add<ProcessStateWidget>();
}

TerminalWrapper::~TerminalWrapper()
{
}
