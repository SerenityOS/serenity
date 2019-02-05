#pragma once

#include <LibGUI/GWidget.h>

class ClockWidget final : public GWidget {
public:
    explicit ClockWidget(GWidget* parent = nullptr);
    virtual ~ClockWidget() override;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void timer_event(GTimerEvent&) override;

    time_t m_last_time { 0 };
};

