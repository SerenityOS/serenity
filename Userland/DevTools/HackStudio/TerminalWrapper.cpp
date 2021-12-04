/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalWrapper.h"
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

namespace HackStudio {

void TerminalWrapper::run_command(const String& command)
{
    if (m_pid != -1) {
        GUI::MessageBox::show(window(),
            "A command is already running in this TerminalWrapper",
            "Can't run command",
            GUI::MessageBox::Type::Error);
        return;
    }

    int ptm_fd = posix_openpt(O_RDWR | O_CLOEXEC);
    if (ptm_fd < 0) {
        perror("posix_openpt");
        VERIFY_NOT_REACHED();
    }
    if (grantpt(ptm_fd) < 0) {
        perror("grantpt");
        VERIFY_NOT_REACHED();
    }
    if (unlockpt(ptm_fd) < 0) {
        perror("unlockpt");
        VERIFY_NOT_REACHED();
    }

    m_terminal_widget->set_pty_master_fd(ptm_fd);
    m_terminal_widget->on_command_exit = [this] {
        int wstatus;
        int rc = waitpid(m_pid, &wstatus, 0);
        if (rc < 0) {
            perror("waitpid");
            VERIFY_NOT_REACHED();
        }
        if (WIFEXITED(wstatus)) {
            m_terminal_widget->inject_string(String::formatted("\033[{};1m(Command exited with code {})\033[0m\r\n", wstatus == 0 ? 32 : 31, WEXITSTATUS(wstatus)));
        } else if (WIFSTOPPED(wstatus)) {
            m_terminal_widget->inject_string("\033[34;1m(Command stopped!)\033[0m\r\n");
        } else if (WIFSIGNALED(wstatus)) {
            m_terminal_widget->inject_string(String::formatted("\033[34;1m(Command signaled with {}!)\033[0m\r\n", strsignal(WTERMSIG(wstatus))));
        }
        m_pid = -1;

        if (on_command_exit)
            on_command_exit();
    };

    m_pid = fork();
    if (m_pid < 0) {
        perror("fork");
        return;
    }

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

        tcsetpgrp(pts_fd, getpid());

        // NOTE: It's okay if this fails.
        int rc = ioctl(0, TIOCNOTTY);

        close(0);
        close(1);
        close(2);

        rc = dup2(pts_fd, 0);
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
        VERIFY(!parts.is_empty());
        const char** args = (const char**)calloc(parts.size() + 1, sizeof(const char*));
        for (size_t i = 0; i < parts.size(); i++) {
            args[i] = parts[i].characters();
        }
        rc = execvp(args[0], const_cast<char**>(args));
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        VERIFY_NOT_REACHED();
    }

    // (In parent process)
    terminal().scroll_to_bottom();
}

void TerminalWrapper::kill_running_command()
{
    VERIFY(m_pid != -1);

    // Kill our child process and its whole process group.
    [[maybe_unused]] auto rc = killpg(m_pid, SIGTERM);
}

void TerminalWrapper::clear_including_history()
{
    m_terminal_widget->clear_including_history();
}

TerminalWrapper::TerminalWrapper(bool user_spawned)
    : m_user_spawned(user_spawned)
{
    set_layout<GUI::VerticalBoxLayout>();

    m_terminal_widget = add<VT::TerminalWidget>(-1, false);

    if (user_spawned)
        run_command("Shell");
}

TerminalWrapper::~TerminalWrapper()
{
}

}
