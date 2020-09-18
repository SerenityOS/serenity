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

#include <AK/Singleton.h>
#include <AK/Time.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Time/HPET.h>
#include <Kernel/Time/HPETComparator.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/PIT.h>
#include <Kernel/Time/RTC.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/VM/MemoryManager.h>

//#define TIME_DEBUG

namespace Kernel {

static AK::Singleton<TimeManagement> s_the;

TimeManagement& TimeManagement::the()
{
    return *s_the;
}

bool TimeManagement::is_system_timer(const HardwareTimer& timer) const
{
    return &timer == m_system_timer.ptr();
}

void TimeManagement::set_epoch_time(timespec ts)
{
    timespec ticks = { 0, (long)ticks_this_second() * (long)1'000'000 };
    timespec_sub(ts, ticks, ts);
    InterruptDisabler disabler;
    m_epoch_time = ts;
}

timespec TimeManagement::epoch_time() const
{
    timespec ts = m_epoch_time;
    timespec ticks = { 0, (long)ticks_this_second() * (long)1'000'000 };
    timespec_add(ts, ticks, ts);
    return ts;
}

void TimeManagement::initialize()
{
    ASSERT(!s_the.is_initialized());
    s_the.ensure_instance();
}
time_t TimeManagement::seconds_since_boot() const
{
    return m_seconds_since_boot;
}
time_t TimeManagement::ticks_per_second() const
{
    return m_system_timer->ticks_per_second();
}

time_t TimeManagement::ticks_this_second() const
{
    return m_ticks_this_second;
}

time_t TimeManagement::boot_time() const
{
    return RTC::boot_time();
}

TimeManagement::TimeManagement()
{
    bool probe_non_legacy_hardware_timers = !(kernel_command_line().lookup("time").value_or("modern") == "legacy");
    if (ACPI::is_enabled()) {
        if (!ACPI::Parser::the()->x86_specific_flags().cmos_rtc_not_present) {
            RTC::initialize();
            m_epoch_time.tv_sec += boot_time();
        } else {
            klog() << "ACPI: RTC CMOS Not present";
        }
    } else {
        // We just assume that we can access RTC CMOS, if ACPI isn't usable.
        RTC::initialize();
        m_epoch_time.tv_sec += boot_time();
    }
    if (probe_non_legacy_hardware_timers) {
        if (!probe_and_set_non_legacy_hardware_timers())
            if (!probe_and_set_legacy_hardware_timers())
                ASSERT_NOT_REACHED();
        return;
    }
    if (probe_and_set_legacy_hardware_timers())
        return;
    ASSERT_NOT_REACHED();
}

timeval TimeManagement::now_as_timeval()
{
    timespec ts = s_the.ptr()->epoch_time();
    timeval tv;
    timespec_to_timeval(ts, tv);
    return tv;
}

Vector<HardwareTimer*> TimeManagement::scan_and_initialize_periodic_timers()
{
    bool should_enable = is_hpet_periodic_mode_allowed();
    dbg() << "Time: Scanning for periodic timers";
    Vector<HardwareTimer*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (hardware_timer.is_periodic_capable()) {
            timers.append(&hardware_timer);
            if (should_enable)
                hardware_timer.set_periodic();
        }
    }
    return timers;
}

Vector<HardwareTimer*> TimeManagement::scan_for_non_periodic_timers()
{
    dbg() << "Time: Scanning for non-periodic timers";
    Vector<HardwareTimer*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (!hardware_timer.is_periodic_capable())
            timers.append(&hardware_timer);
    }
    return timers;
}

bool TimeManagement::is_hpet_periodic_mode_allowed()
{
    auto hpet_mode = kernel_command_line().lookup("hpet").value_or("periodic");
    if (hpet_mode == "periodic")
        return true;
    if (hpet_mode == "nonperiodic")
        return false;
    ASSERT_NOT_REACHED();
}

bool TimeManagement::probe_and_set_non_legacy_hardware_timers()
{
    if (!ACPI::is_enabled())
        return false;
    if (!HPET::test_and_initialize())
        return false;
    if (!HPET::the().comparators().size()) {
        dbg() << "HPET initialization aborted.";
        return false;
    }
    dbg() << "HPET: Setting appropriate functions to timers.";

    for (auto& hpet_comparator : HPET::the().comparators())
        m_hardware_timers.append(hpet_comparator);

    auto periodic_timers = scan_and_initialize_periodic_timers();
    auto non_periodic_timers = scan_for_non_periodic_timers();

    if (is_hpet_periodic_mode_allowed())
        ASSERT(!periodic_timers.is_empty());

    ASSERT(periodic_timers.size() + non_periodic_timers.size() >= 2);

    if (periodic_timers.size() >= 2) {
        m_time_keeper_timer = periodic_timers[1];
        m_system_timer = periodic_timers[0];
    } else {
        if (periodic_timers.size() == 1) {
            m_time_keeper_timer = periodic_timers[0];
            m_system_timer = non_periodic_timers[0];
        } else {
            m_time_keeper_timer = non_periodic_timers[1];
            m_system_timer = non_periodic_timers[0];
        }
    }

    m_system_timer->set_callback(Scheduler::timer_tick);
    dbg() << "Reset timers";
    m_system_timer->try_to_set_frequency(m_system_timer->calculate_nearest_possible_frequency(1024));
    m_time_keeper_timer->set_callback(TimeManagement::update_time);
    m_time_keeper_timer->try_to_set_frequency(OPTIMAL_TICKS_PER_SECOND_RATE);

    return true;
}

bool TimeManagement::probe_and_set_legacy_hardware_timers()
{
    if (ACPI::is_enabled()) {
        if (ACPI::Parser::the()->x86_specific_flags().cmos_rtc_not_present) {
            dbg() << "ACPI: CMOS RTC Not Present";
            return false;
        } else {
            dbg() << "ACPI: CMOS RTC Present";
        }
    }

    m_hardware_timers.append(PIT::initialize(TimeManagement::update_time));
    m_hardware_timers.append(RealTimeClock::create(Scheduler::timer_tick));
    m_time_keeper_timer = m_hardware_timers[0];
    m_system_timer = m_hardware_timers[1];
    return true;
}

void TimeManagement::update_time(const RegisterState& regs)
{
    TimeManagement::the().increment_time_since_boot(regs);
}

void TimeManagement::increment_time_since_boot(const RegisterState&)
{
    ASSERT(!m_time_keeper_timer.is_null());
    if (++m_ticks_this_second >= m_time_keeper_timer->ticks_per_second()) {
        // FIXME: Synchronize with other clock somehow to prevent drifting apart.
        ++m_seconds_since_boot;
        ++m_epoch_time.tv_sec;
        m_ticks_this_second = 0;
    }
}

}
