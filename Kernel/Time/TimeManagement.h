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
#include <Kernel/UnixTypes.h>

namespace Kernel {

#define OPTIMAL_TICKS_PER_SECOND_RATE 1000

class HardwareTimerBase;

class TimeManagement {
    AK_MAKE_ETERNAL;

public:
    TimeManagement();
    static bool initialized();
    static void initialize(u32 cpu);
    static TimeManagement& the();

    timespec epoch_time() const;
    void set_epoch_time(timespec);
    time_t seconds_since_boot() const;
    time_t ticks_per_second() const;
    time_t ticks_this_second() const;
    time_t boot_time() const;

    bool is_system_timer(const HardwareTimerBase&) const;

    static void update_time(const RegisterState&);
    void increment_time_since_boot(const RegisterState&);

    static bool is_hpet_periodic_mode_allowed();

    static timeval now_as_timeval();

    timespec remaining_epoch_time_adjustment() const { return m_remaining_epoch_time_adjustment; }
    void set_remaining_epoch_time_adjustment(const timespec& adjustment) { m_remaining_epoch_time_adjustment = adjustment; }

private:
    bool probe_and_set_legacy_hardware_timers();
    bool probe_and_set_non_legacy_hardware_timers();
    Vector<HardwareTimerBase*> scan_and_initialize_periodic_timers();
    Vector<HardwareTimerBase*> scan_for_non_periodic_timers();
    NonnullRefPtrVector<HardwareTimerBase> m_hardware_timers;
    void set_system_timer(HardwareTimerBase&);

    u32 m_ticks_this_second { 0 };
    u32 m_seconds_since_boot { 0 };
    timespec m_epoch_time { 0, 0 };
    timespec m_remaining_epoch_time_adjustment { 0, 0 };
    RefPtr<HardwareTimerBase> m_system_timer;
    RefPtr<HardwareTimerBase> m_time_keeper_timer;
};

}
