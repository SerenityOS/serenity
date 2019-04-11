#include <LibCore/CTimer.h>

CTimer::CTimer(CObject* parent)
    : CObject(parent)
{
}

CTimer::~CTimer()
{
}

void CTimer::start()
{
    start(m_interval);
}

void CTimer::start(int interval)
{
    if (m_active)
        return;
    start_timer(interval);
    m_active = true;
}

void CTimer::stop()
{
    if (!m_active)
        return;
    stop_timer();
    m_active = false;
}

void CTimer::timer_event(CTimerEvent&)
{
    if (m_single_shot)
        stop();
    if (on_timeout)
        on_timeout();
}
