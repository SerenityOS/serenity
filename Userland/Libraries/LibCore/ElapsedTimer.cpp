/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Time.h>
#include <LibCore/ElapsedTimer.h>

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
    m_origin_time = m_precise ? Duration::now_monotonic() : Duration::now_monotonic_coarse();
}

void ElapsedTimer::reset()
{
    m_valid = false;
    m_origin_time = {};
}

i64 ElapsedTimer::elapsed_milliseconds() const
{
    return elapsed_time().to_milliseconds();
}

Duration ElapsedTimer::elapsed_time() const
{
    VERIFY(is_valid());
    auto now = m_precise ? Duration::now_monotonic() : Duration::now_monotonic_coarse();
    return now - m_origin_time;
}

}
