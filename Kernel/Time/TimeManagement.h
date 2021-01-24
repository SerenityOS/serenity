/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

#define OPTIMAL_TICKS_PER_SECOND_RATE 250

class HardwareTimerBase;

enum class TimePrecision {
    Coarse = 0,
    Precise
};

class TimeManagement {
    AK_MAKE_ETERNAL;

public:
    TimeManagement();
    static bool initialized();
    static void initialize(u32 cpu);
    static TimeManagement& the();

    static bool is_valid_clock_id(clockid_t);
    KResultOr<timespec> current_time(clockid_t) const;
    timespec monotonic_time(TimePrecision = TimePrecision::Coarse) const;
    timespec monotonic_time_raw() const
    {
        // TODO: implement
        return monotonic_time(TimePrecision::Precise);
    }
    timespec epoch_time(TimePrecision = TimePrecision::Precise) const;
    void set_epoch_time(timespec);
    time_t ticks_per_second() const;
    time_t boot_time() const;

    bool is_system_timer(const HardwareTimerBase&) const;

    static void update_time(const RegisterState&);
    static void update_time_hpet(const RegisterState&);
    void increment_time_since_boot_hpet();
    void increment_time_since_boot();

    static bool is_hpet_periodic_mode_allowed();

    u64 uptime_ms() const;
    static timeval now_as_timeval();

    timespec remaining_epoch_time_adjustment() const { return m_remaining_epoch_time_adjustment; }
    void set_remaining_epoch_time_adjustment(const timespec& adjustment) { m_remaining_epoch_time_adjustment = adjustment; }

    bool can_query_precise_time() const { return m_can_query_precise_time; }

private:
    bool probe_and_set_legacy_hardware_timers();
    bool probe_and_set_non_legacy_hardware_timers();
    Vector<HardwareTimerBase*> scan_and_initialize_periodic_timers();
    Vector<HardwareTimerBase*> scan_for_non_periodic_timers();
    NonnullRefPtrVector<HardwareTimerBase> m_hardware_timers;
    void set_system_timer(HardwareTimerBase&);
    static void system_timer_tick(const RegisterState&);

    // Variables between m_update1 and m_update2 are synchronized
    Atomic<u32> m_update1 { 0 };
    u32 m_ticks_this_second { 0 };
    u64 m_seconds_since_boot { 0 };
    timespec m_epoch_time { 0, 0 };
    timespec m_remaining_epoch_time_adjustment { 0, 0 };
    Atomic<u32> m_update2 { 0 };

    u32 m_time_ticks_per_second { 0 }; // may be different from interrupts/second (e.g. hpet)
    bool m_can_query_precise_time { false };

    RefPtr<HardwareTimerBase> m_system_timer;
    RefPtr<HardwareTimerBase> m_time_keeper_timer;
};

}
