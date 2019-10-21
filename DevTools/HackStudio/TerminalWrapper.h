#pragma once

#include <LibGUI/GWidget.h>

class TerminalWidget;

class TerminalWrapper final : public GWidget {
    C_OBJECT(TerminalWrapper)
public:
    virtual ~TerminalWrapper() override;

private:
    explicit TerminalWrapper(GWidget* parent);

    RefPtr<TerminalWidget> m_terminal_widget;
};
