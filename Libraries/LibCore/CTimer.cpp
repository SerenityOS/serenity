#include <LibCore/CTimer.h>

CTimer::CTimer(CObject* parent)
    : CObject(parent)
{
}

CTimer::CTimer(int interval, Function<void()>&& timeout_handler, CObject* parent)
    : CObject(parent)
    , on_timeout(move(timeout_handler))
{
    start(interval);
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
    m_interval = interval;
    start_timer(interval);
    m_active = true;
}

void CTimer::restart(int interval)
{
    if (m_active)
        stop();
    start(interval);
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
    else {
        if (m_interval_dirty) {
            stop();
            start(m_interval);
        }
    }

    if (on_timeout)
        on_timeout();
}
