#pragma once

#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWidget.h>

class CTimer;

class ProcessStacksWidget final : public GWidget {
    C_OBJECT(ProcessStacksWidget)
public:
    explicit ProcessStacksWidget(GWidget* parent);
    virtual ~ProcessStacksWidget() override;

    void set_pid(pid_t);
    void refresh();

private:
    pid_t m_pid { -1 };
    RefPtr<GTextEditor> m_stacks_editor;
    RefPtr<CTimer> m_timer;
};
