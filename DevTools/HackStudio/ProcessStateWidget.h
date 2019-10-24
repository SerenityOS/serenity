#pragma once

#include <LibGUI/GWidget.h>

class CTimer;
class GLabel;

class ProcessStateWidget final : public GWidget {
    C_OBJECT(ProcessStateWidget)
public:
    virtual ~ProcessStateWidget() override;

    void set_pid(pid_t);

private:
    explicit ProcessStateWidget(GWidget* parent);

    void refresh();

    RefPtr<GLabel> m_pid_label;
    RefPtr<GLabel> m_state_label;
    RefPtr<GLabel> m_cpu_label;
    RefPtr<GLabel> m_memory_label;

    RefPtr<CTimer> m_timer;

    pid_t m_pid { -1 };
};
