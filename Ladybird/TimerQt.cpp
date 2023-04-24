/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimerQt.h"
#include <AK/NonnullRefPtr.h>
#include <QTimer>

namespace Ladybird {

NonnullRefPtr<TimerQt> TimerQt::create()
{
    return adopt_ref(*new TimerQt);
}

TimerQt::TimerQt()
{
    m_timer = new QTimer;
    QObject::connect(m_timer, &QTimer::timeout, [this] {
        if (on_timeout)
            on_timeout();
    });
}

TimerQt::~TimerQt()
{
    delete m_timer;
}

void TimerQt::start()
{
    m_timer->start();
}

void TimerQt::start(int interval_ms)
{
    m_timer->start(interval_ms);
}

void TimerQt::restart()
{
    restart(interval());
}

void TimerQt::restart(int interval_ms)
{
    if (is_active())
        stop();
    start(interval_ms);
}

void TimerQt::stop()
{
    m_timer->stop();
}

void TimerQt::set_active(bool active)
{
    if (active)
        m_timer->start();
    else
        m_timer->stop();
}

bool TimerQt::is_active() const
{
    return m_timer->isActive();
}

int TimerQt::interval() const
{
    return m_timer->interval();
}

void TimerQt::set_interval(int interval_ms)
{
    m_timer->setInterval(interval_ms);
}

bool TimerQt::is_single_shot() const
{
    return m_timer->isSingleShot();
}

void TimerQt::set_single_shot(bool single_shot)
{
    m_timer->setSingleShot(single_shot);
}

}
