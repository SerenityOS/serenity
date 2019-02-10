#include <stdio.h>
#include <time.h>
#include <SharedGraphics/Painter.h>
#include "ClockWidget.h"

ClockWidget::ClockWidget(GWidget* parent)
    : GWidget(parent)
{
    set_relative_rect({ 0, 0, 100, 40 });
    start_timer(300);
}

ClockWidget::~ClockWidget()
{
}

void ClockWidget::paint_event(GPaintEvent&)
{
    auto now = time(nullptr);
    auto& tm = *localtime(&now);

    char timeBuf[128];
    sprintf(timeBuf, "%02u:%02u:%02u", tm.tm_hour, tm.tm_min, tm.tm_sec);

    Painter painter(*this);
    painter.fill_rect(rect(), Color::LightGray);
    painter.draw_text(rect(), timeBuf, Painter::TextAlignment::Center, Color::Black);
}

void ClockWidget::timer_event(GTimerEvent&)
{
    auto now = time(nullptr);
    if (now == m_last_time)
        return;
    m_last_time = now;
    update();
}
