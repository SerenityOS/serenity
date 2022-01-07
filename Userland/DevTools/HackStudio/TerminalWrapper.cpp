/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalWrapper.h"
#include <AK/String.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/Application.h>
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

void TerminalWrapper::run_command(const String& command, Optional<String> working_directory, WaitForExit wait_for_exit)
{
    if (m_pid != -1) {
        GUI::MessageBox::show(window(),
            "A command is already running in this TerminalWrapper",
            "Can't run command",
            GUI::MessageBox::Type::Error);
        return;
    }

    auto ptm_res = setup_master_pseudoterminal();
    if (ptm_res.is_error()) {
        perror("setup_master_pseudoterminal");
        return;
    }

    int ptm_fd = ptm_res.value();
    m_child_exited = false;
    m_child_exit_status.clear();

    m_pid = fork();
    if (m_pid < 0) {
        perror("fork");
        return;
    }

    if (m_pid > 0) {
        if (wait_for_exit == WaitForExit::Yes) {
            GUI::Application::the()->event_loop().spin_until([this]() {
                return m_child_exited;
            });
        }
        return;
    }

    if (working_directory.has_value()) {
        if (chdir(working_directory->characters())) {
            perror("chdir");
            exit(1);
        }
    }

    if (setup_slave_pseudoterminal(ptm_fd).is_error()) {
        perror("setup_pseudoterminal");
        exit(1);
    }

    auto parts = command.split(' ');
    VERIFY(!parts.is_empty());
    const char** args = (const char**)calloc(parts.size() + 1, sizeof(const char*));
    for (size_t i = 0; i < parts.size(); i++) {
        args[i] = parts[i].characters();
    }
    auto rc = execvp(args[0], const_cast<char**>(args));
    if (rc < 0) {
        perror("execve");
        exit(1);
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<int> TerminalWrapper::setup_master_pseudoterminal(WaitForChildOnExit wait_for_child)
{
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
    m_terminal_widget->on_command_exit = [this, wait_for_child] {
        if (wait_for_child == WaitForChildOnExit::Yes) {
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

            m_child_exit_status = WEXITSTATUS(wstatus);
            m_child_exited = true;
        }
        m_pid = -1;

        if (on_command_exit)
            on_command_exit();
    };

    terminal().scroll_to_bottom();

    return ptm_fd;
}

ErrorOr<void> TerminalWrapper::setup_slave_pseudoterminal(int master_fd)
{
    setsid();

    const char* tty_name = ptsname(master_fd);
    if (!tty_name)
        return Error::from_errno(errno);

    close(master_fd);
    int pts_fd = open(tty_name, O_RDWR);
    if (pts_fd < 0)
        return Error::from_errno(errno);

    tcsetpgrp(pts_fd, getpid());

    // NOTE: It's okay if this fails.
    int rc = ioctl(0, TIOCNOTTY);

    close(0);
    close(1);
    close(2);

    rc = dup2(pts_fd, 0);
    if (rc < 0)
        return Error::from_errno(errno);

    rc = dup2(pts_fd, 1);
    if (rc < 0)
        return Error::from_errno(errno);

    rc = dup2(pts_fd, 2);
    if (rc < 0)
        return Error::from_errno(errno);

    rc = close(pts_fd);
    if (rc < 0)
        return Error::from_errno(errno);

    rc = ioctl(0, TIOCSCTTY);
    if (rc < 0)
        return Error::from_errno(errno);

    setenv("TERM", "xterm", true);

    return {};
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

int TerminalWrapper::child_exit_status() const
{
    VERIFY(m_child_exit_status.has_value());
    return m_child_exit_status.value();
}

}
