/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Time/TimeSource.h>

namespace Kernel {

u64 TimeSource::read_counter()
{
    u64 raw = read_counter_raw();
    if (m_counter_size == TimeSource::CounterSize::Bits64)
        return raw;

    auto wraps = m_counter_last_read >> 32;
    if (raw < (m_counter_last_read & 0xFFFFFFFF))
        wraps++;
    return (wraps << 32) | raw;
}

u64 TimeSource::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = read_counter();

    u64 delta_ticks = m_counter_drift;
    if (current_value >= m_counter_last_read)
        delta_ticks += current_value - m_counter_last_read;
    else
        delta_ticks += (NumericLimits<u64>::max() - m_counter_last_read + 1) + current_value;

    u64 ticks_since_last_second = static_cast<u64>(ticks_this_second) + delta_ticks;
    seconds_since_boot += ticks_since_last_second / frequency();
    ticks_this_second = ticks_since_last_second % frequency();

    if (!query_only) {
        m_counter_drift = 0;
        m_counter_last_read = current_value;
    }

    return (delta_ticks * 1'000'000'000ull) / frequency();
}

void TimeSource::add_drift(u64 drift)
{
    m_counter_drift += drift;
}

}
