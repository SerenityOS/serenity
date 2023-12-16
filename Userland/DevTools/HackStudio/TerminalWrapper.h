/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGUI/Widget.h>
#include <LibVT/TerminalWidget.h>

namespace HackStudio {

class TerminalWrapper final : public GUI::Widget {
    C_OBJECT(TerminalWrapper)
public:
    virtual ~TerminalWrapper() override = default;
    enum class WaitForExit {
        No,
        Yes
    };
    ErrorOr<void> run_command(ByteString const&, Optional<ByteString> working_directory = {}, WaitForExit = WaitForExit::No, Optional<StringView> failure_message = {});
    ErrorOr<void> kill_running_command();
    void clear_including_history();

    bool user_spawned() const { return m_user_spawned; }
    VT::TerminalWidget& terminal() { return *m_terminal_widget; }

    enum class WaitForChildOnExit {
        No,
        Yes,
    };
    ErrorOr<int> setup_master_pseudoterminal(WaitForChildOnExit = WaitForChildOnExit::Yes);
    static ErrorOr<void> setup_slave_pseudoterminal(int master_fd);
    int child_exit_status() const;

    Function<void()> on_command_exit;

private:
    explicit TerminalWrapper(bool user_spawned = true);

    RefPtr<VT::TerminalWidget> m_terminal_widget;
    pid_t m_pid { -1 };
    bool m_user_spawned { true };
    bool m_child_exited { false };
    Optional<int> m_child_exit_status;
};

}
