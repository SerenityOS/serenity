/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>

namespace Core {

Timer::Timer(Object* parent)
    : Object(parent)
{
}

Timer::Timer(int interval, Function<void()>&& timeout_handler, Object* parent)
    : Object(parent)
    , on_timeout(move(timeout_handler))
{
    start(interval);
}

Timer::~Timer()
{
}

void Timer::start()
{
    start(m_interval);
}

void Timer::start(int interval)
{
    if (m_active)
        return;
    m_interval = interval;
    start_timer(interval);
    m_active = true;
}

void Timer::restart()
{
    restart(m_interval);
}

void Timer::restart(int interval)
{
    if (m_active)
        stop();
    start(interval);
}

void Timer::stop()
{
    if (!m_active)
        return;
    stop_timer();
    m_active = false;
}

void Timer::timer_event(TimerEvent&)
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

}
