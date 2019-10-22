#pragma once

#include <LibGUI/GWidget.h>

class TerminalWidget;

class TerminalWrapper final : public GWidget {
    C_OBJECT(TerminalWrapper)
public:
    virtual ~TerminalWrapper() override;

    void run_command(const String&);

private:
    explicit TerminalWrapper(GWidget* parent);

    RefPtr<TerminalWidget> m_terminal_widget;
    pid_t m_pid { -1 };
};
