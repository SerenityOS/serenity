/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Time.h>
#include <LibCore/ElapsedTimer.h>
#include <sys/time.h>
#include <time.h>

namespace Core {

ElapsedTimer ElapsedTimer::start_new()
{
    ElapsedTimer timer;
    timer.start();
    return timer;
}

void ElapsedTimer::start()
{
    m_valid = true;
    timespec now_spec;
    clock_gettime(m_precise ? CLOCK_MONOTONIC : CLOCK_MONOTONIC_COARSE, &now_spec);
    m_origin_time.tv_sec = now_spec.tv_sec;
    m_origin_time.tv_usec = now_spec.tv_nsec / 1000;
}

void ElapsedTimer::reset()
{
    m_valid = false;
    m_origin_time = { 0, 0 };
}

int ElapsedTimer::elapsed() const
{
    VERIFY(is_valid());
    struct timeval now;
    timespec now_spec;
    clock_gettime(m_precise ? CLOCK_MONOTONIC : CLOCK_MONOTONIC_COARSE, &now_spec);
    now.tv_sec = now_spec.tv_sec;
    now.tv_usec = now_spec.tv_nsec / 1000;
    struct timeval diff;
    timeval_sub(now, m_origin_time, diff);
    return diff.tv_sec * 1000 + diff.tv_usec / 1000;
}

Time ElapsedTimer::elapsed_time() const
{
    return Time::from_milliseconds(elapsed());
}

}
