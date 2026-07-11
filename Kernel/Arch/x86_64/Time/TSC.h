/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel {

class TSC {
public:
    static bool is_supported_and_invariant();
    static TSC& the();

    bool calibrate(HardwareTimerBase& calibration_source);
    u64 frequency() const { return m_frequency; }
    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);

private:
    u64 m_frequency { 0 };
    u64 m_main_counter_last_read { 0 };
};

}
