#include "ClockWidget.h"
#include <SharedGraphics/Painter.h>
#include <time.h>

ClockWidget::ClockWidget(GWidget* parent)
    : GWidget(parent)
{
    setWindowRelativeRect({ 0, 0, 100, 40 });
    startTimer(250);
}

ClockWidget::~ClockWidget()
{
}

void ClockWidget::paintEvent(PaintEvent&)
{
    auto now = time(nullptr);
    auto& tm = *localtime(&now);
    
    char timeBuf[128];
    sprintf(timeBuf, "%02u:%02u:%02u ", tm.tm_hour, tm.tm_min, tm.tm_sec);
    
    Painter painter(*this);
    painter.fill_rect(rect(), Color::MidGray);
    painter.draw_text(rect(), timeBuf, Painter::TextAlignment::Center, Color::Black);
}

void ClockWidget::timerEvent(TimerEvent&)
{
    auto now = time(nullptr);
    if (now == m_lastSeenTimestamp)
        return;
    m_lastSeenTimestamp = now;
    update();
}

