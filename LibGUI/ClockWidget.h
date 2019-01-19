#pragma once

#include "Widget.h"

class ClockWidget final : public Widget {
public:
    explicit ClockWidget(Widget* parent = nullptr);
    virtual ~ClockWidget() override;

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void timerEvent(TimerEvent&) override;

    dword m_lastSeenTimestamp { 0 };
};

