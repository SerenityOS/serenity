#pragma once

#include <LibGUI/GWidget.h>

class ProcessStateWidget;
class TerminalWidget;

class TerminalWrapper final : public GWidget {
    C_OBJECT(TerminalWrapper)
public:
    virtual ~TerminalWrapper() override;

    void run_command(const String&);

    Function<void()> on_command_exit;

private:
    explicit TerminalWrapper(GWidget* parent);

    RefPtr<ProcessStateWidget> m_process_state_widget;
    RefPtr<TerminalWidget> m_terminal_widget;
    pid_t m_pid { -1 };
};
