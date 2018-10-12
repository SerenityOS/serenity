#pragma once

#include "Widget.h"

class ClockWidget final : public Widget {
public:
    explicit ClockWidget(Widget* parent = nullptr);
    virtual ~ClockWidget() override;

private:
    virtual void onPaint(PaintEvent&) override;
    virtual void onTimer(TimerEvent&) override;

    dword m_lastSeenTimestamp { 0 };
};

