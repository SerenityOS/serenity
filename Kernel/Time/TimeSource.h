/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Time/HardwareTimerType.h>
#include <Kernel/Time/TimeDeviceBase.h>

namespace Kernel {

class TimeSource : public virtual TimeDeviceBase {
public:
    enum class CounterSize {
        Bits32,
        Bits64,
    };

    TimeSource(CounterSize size)
        : m_counter_size(size)
    {
    }

    virtual u64 read_counter_raw() const = 0;
    u64 read_counter();

    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);

protected:
    CounterSize counter_size() const { return m_counter_size; }

    u64 counter_last_read() const { return m_counter_last_read; }
    void clear_counter_last_read() { m_counter_last_read = 0; }

    void add_drift(u64 drift);

private:
    CounterSize m_counter_size {};

    u64 m_counter_last_read { 0 };
    u64 m_counter_drift { 0 };
};

}
