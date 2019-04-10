#include <LibGUI/GTimer.h>

GTimer::GTimer(GObject* parent)
    : GObject(parent)
{
}

GTimer::~GTimer()
{
}

void GTimer::start()
{
    start(m_interval);
}

void GTimer::start(int interval)
{
    if (m_active)
        return;
    start_timer(interval);
    m_active = true;
}

void GTimer::stop()
{
    if (!m_active)
        return;
    stop_timer();
    m_active = false;
}

void GTimer::timer_event(CTimerEvent&)
{
    if (m_single_shot)
        stop();
    if (on_timeout)
        on_timeout();
}
