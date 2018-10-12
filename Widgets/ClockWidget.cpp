#include "ClockWidget.h"
#include "Painter.h"
#include <time.h>

ClockWidget::ClockWidget(Widget* parent)
    : Widget(parent)
{
    setWindowRelativeRect({ 0, 0, 100, 40 });
    startTimer(250);
}

ClockWidget::~ClockWidget()
{
}

void ClockWidget::onPaint(PaintEvent&)
{
    auto now = time(nullptr);
    auto& tm = *localtime(&now);
    
    char timeBuf[128];
    sprintf(timeBuf, "%02u:%02u:%02u ", tm.tm_hour, tm.tm_min, tm.tm_sec);
    
    Painter painter(*this);
    painter.fillRect(rect(), Color::MidGray);
    painter.drawText(rect(), timeBuf, Painter::TextAlignment::Center, Color::Black);
}

void ClockWidget::onTimer(TimerEvent&)
{
    auto now = time(nullptr);
    if (now == m_lastSeenTimestamp)
        return;
    m_lastSeenTimestamp = now;
    update();
}

