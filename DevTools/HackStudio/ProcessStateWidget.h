#pragma once

#include <LibGUI/GWidget.h>

class CTimer;
class GLabel;

class ProcessStateWidget final : public GWidget {
    C_OBJECT(ProcessStateWidget)
public:
    virtual ~ProcessStateWidget() override;

    void set_tty_fd(int);

private:
    explicit ProcessStateWidget(GWidget* parent);

    void refresh();

    RefPtr<GLabel> m_pid_label;
    RefPtr<GLabel> m_state_label;
    RefPtr<GLabel> m_cpu_label;
    RefPtr<GLabel> m_memory_label;

    RefPtr<CTimer> m_timer;

    int m_tty_fd { -1 };
};
