/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalWrapper.h"
#include <AK/ByteString.h>
#include <LibCore/System.h>
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

ErrorOr<void> TerminalWrapper::run_command(ByteString const& command, Optional<ByteString> working_directory, WaitForExit wait_for_exit, Optional<StringView> failure_message)
{
    if (m_pid != -1) {
        GUI::MessageBox::show(window(),
            "A command is already running in this TerminalWrapper"sv,
            "Can't run command"sv,
            GUI::MessageBox::Type::Error);
        return {};
    }

    auto ptm_fd = TRY(setup_master_pseudoterminal());

    m_child_exited = false;
    m_child_exit_status.clear();

    m_pid = TRY(Core::System::fork());

    if (m_pid > 0) {
        m_terminal_widget->set_startup_process_id(m_pid);

        if (wait_for_exit == WaitForExit::Yes) {
            GUI::Application::the()->event_loop().spin_until([this]() {
                return m_child_exited;
            });

            VERIFY(m_child_exit_status.has_value());
            if (m_child_exit_status.value() != 0)
                return Error::from_string_view(failure_message.value_or("Command execution failed"sv));
        }

        return {};
    }

    if (working_directory.has_value())
        TRY(Core::System::chdir(working_directory->view()));

    TRY(setup_slave_pseudoterminal(ptm_fd));

    auto args = command.split_view(' ');
    VERIFY(!args.is_empty());
    TRY(Core::System::exec(args[0], args, Core::System::SearchInPath::Yes));
    VERIFY_NOT_REACHED();
}

ErrorOr<int> TerminalWrapper::setup_master_pseudoterminal(WaitForChildOnExit wait_for_child)
{
    int ptm_fd = TRY(Core::System::posix_openpt(O_RDWR | O_CLOEXEC));
    bool error_happened = true;

    ScopeGuard close_ptm { [&]() {
        if (error_happened) {
            if (auto result = Core::System::close(ptm_fd); result.is_error())
                warnln("{}", result.release_error());
        }
    } };

    TRY(Core::System::grantpt(ptm_fd));
    TRY(Core::System::unlockpt(ptm_fd));

    m_terminal_widget->set_pty_master_fd(ptm_fd);
    m_terminal_widget->on_command_exit = [this, wait_for_child] {
        if (wait_for_child == WaitForChildOnExit::Yes) {
            auto result = Core::System::waitpid(m_pid, 0);
            if (result.is_error()) {
                warnln("{}", result.error());
                VERIFY_NOT_REACHED();
            }
            int wstatus = result.release_value().status;

            if (WIFEXITED(wstatus)) {
                m_terminal_widget->inject_string(ByteString::formatted("\033[{};1m(Command exited with code {})\033[0m\r\n", wstatus == 0 ? 32 : 31, WEXITSTATUS(wstatus)));
            } else if (WIFSTOPPED(wstatus)) {
                m_terminal_widget->inject_string("\033[34;1m(Command stopped!)\033[0m\r\n"sv);
            } else if (WIFSIGNALED(wstatus)) {
                m_terminal_widget->inject_string(ByteString::formatted("\033[34;1m(Command signaled with {}!)\033[0m\r\n", strsignal(WTERMSIG(wstatus))));
            }

            m_child_exit_status = WEXITSTATUS(wstatus);
            m_child_exited = true;
        }
        m_pid = -1;

        if (on_command_exit)
            on_command_exit();
    };

    terminal().scroll_to_bottom();

    error_happened = false;

    return ptm_fd;
}

ErrorOr<void> TerminalWrapper::setup_slave_pseudoterminal(int master_fd)
{
    setsid();

    auto tty_name = TRY(Core::System::ptsname(master_fd));

    close(master_fd);

    int pts_fd = TRY(Core::System::open(tty_name, O_RDWR));

    tcsetpgrp(pts_fd, getpid());

    // NOTE: It's okay if this fails.
    ioctl(0, TIOCNOTTY);

    close(0);
    close(1);
    close(2);

    TRY(Core::System::dup2(pts_fd, 0));
    TRY(Core::System::dup2(pts_fd, 1));
    TRY(Core::System::dup2(pts_fd, 2));

    TRY(Core::System::close(pts_fd));

    TRY(Core::System::ioctl(0, TIOCSCTTY));

    setenv("TERM", "xterm", true);

    return {};
}

ErrorOr<void> TerminalWrapper::kill_running_command()
{
    VERIFY(m_pid != -1);

    // Kill our child process and its whole process group.
    TRY(Core::System::killpg(m_pid, SIGTERM));
    return {};
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
    if (user_spawned) {
        auto maybe_error = run_command("Shell");
        if (maybe_error.is_error())
            warnln("{}", maybe_error.release_error());
    }
}

int TerminalWrapper::child_exit_status() const
{
    VERIFY(m_child_exit_status.has_value());
    return m_child_exit_status.value();
}

}
