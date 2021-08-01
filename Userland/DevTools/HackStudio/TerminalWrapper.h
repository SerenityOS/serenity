/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibVT/TerminalWidget.h>

namespace HackStudio {

class TerminalWrapper final : public GUI::Widget {
    C_OBJECT(TerminalWrapper)
public:
    virtual ~TerminalWrapper() override;

    void run_command(const String&);
    void kill_running_command();
    void clear_including_history();

    bool user_spawned() const { return m_user_spawned; }
    VT::TerminalWidget& terminal() { return *m_terminal_widget; }

    Function<void()> on_command_exit;

private:
    explicit TerminalWrapper(bool user_spawned = true);

    RefPtr<VT::TerminalWidget> m_terminal_widget;
    pid_t m_pid { -1 };
    bool m_user_spawned { true };
};

}
