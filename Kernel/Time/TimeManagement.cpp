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

#include <Kernel/ACPI/ACPIParser.h>
#include <Kernel/KParams.h>
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

static TimeManagement* s_time_management;

bool TimeManagement::initialized()
{
    return s_time_management != nullptr;
}

bool TimeManagement::is_system_timer(const HardwareTimer& timer) const
{
    return &timer == m_system_timer.ptr();
}

void TimeManagement::set_epoch_time(time_t value)
{
    InterruptDisabler disabler;
    m_epoch_time = value;
}

time_t TimeManagement::epoch_time() const
{
    return m_epoch_time;
}

void TimeManagement::initialize(bool probe_non_legacy_hardware_timers)
{
    ASSERT(!TimeManagement::initialized());
    s_time_management = new TimeManagement(probe_non_legacy_hardware_timers);
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

void TimeManagement::stale_function(const RegisterState&)
{
}

TimeManagement::TimeManagement(bool probe_non_legacy_hardware_timers)
{
    if (ACPI::Parser::the().is_operable()) {
        if (!ACPI::Parser::the().x86_specific_flags().cmos_rtc_not_present) {
            RTC::initialize();
            m_epoch_time += boot_time();
        } else {
            klog() << "ACPI: RTC CMOS Not present";
        }
    } else {
        // We just assume that we can access RTC CMOS, if ACPI isn't usable.
        RTC::initialize();
        m_epoch_time += boot_time();
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

Vector<size_t> TimeManagement::scan_and_initialize_periodic_timers()
{
    bool enable_periodic_mode = is_hpet_periodic_mode_allowed();
    dbg() << "Scanning for Periodic timers";
    Vector<size_t> periodic_timers_indexes;
    periodic_timers_indexes.ensure_capacity(m_hardware_timers.size());
    for (size_t index = 0; index < m_hardware_timers.size(); index++) {
        if (!m_hardware_timers[index].is_null()) {
            if (m_hardware_timers[index]->is_periodic_capable()) {
                periodic_timers_indexes.append(index);
                if (enable_periodic_mode)
                    m_hardware_timers[index]->set_periodic();
            }
        }
    }
    return periodic_timers_indexes;
}

Vector<size_t> TimeManagement::scan_for_non_periodic_timers()
{
    dbg() << "Scanning for Non-Periodic timers";
    Vector<size_t> non_periodic_timers_indexes;
    non_periodic_timers_indexes.ensure_capacity(m_hardware_timers.size());
    for (size_t index = 0; index < m_hardware_timers.size(); index++) {
        if (!m_hardware_timers[index].is_null())
            if (!m_hardware_timers[index]->is_periodic_capable())
                non_periodic_timers_indexes.append(index);
    }
    return non_periodic_timers_indexes;
}

bool TimeManagement::is_hpet_periodic_mode_allowed()
{
    if (!KParams::the().has("hpet")) {
        return true;
    }
    auto hpet_mode = KParams::the().get("hpet");
    if (hpet_mode == "periodic")
        return true;
    if (hpet_mode == "nonperiodic")
        return false;
    ASSERT_NOT_REACHED();
}

bool TimeManagement::probe_and_set_non_legacy_hardware_timers()
{
    if (!ACPI::Parser::the().is_operable())
        return false;
    if (!HPET::test_and_initialize())
        return false;
    if (!HPET::the().comparators().size()) {
        dbg() << "HPET initialization aborted.";
        return false;
    }
    dbg() << "HPET: Setting appropriate functions to timers.";

    m_hardware_timers.resize(HPET::the().comparators().size());
    for (size_t index = 0; index < m_hardware_timers.size(); index++) {
        m_hardware_timers[index] = HPET::the().comparators()[index];
#ifdef TIME_DEBUG
        dbg() << m_hardware_timers[index].ptr() << " <- " << HPET::the().comparators()[index].ptr();
#endif
    }

    auto periodic_timer_indexes = scan_and_initialize_periodic_timers();
    auto non_periodic_timer_indexes = scan_for_non_periodic_timers();

    if (is_hpet_periodic_mode_allowed())
        ASSERT(!periodic_timer_indexes.is_empty());

    ASSERT(periodic_timer_indexes.size() + non_periodic_timer_indexes.size() >= 2);

    if (periodic_timer_indexes.size() >= 2) {
        m_time_keeper_timer = m_hardware_timers[periodic_timer_indexes[1]];
        m_system_timer = m_hardware_timers[periodic_timer_indexes[0]];
    } else {
        if (periodic_timer_indexes.size() == 1) {
            m_time_keeper_timer = m_hardware_timers[periodic_timer_indexes[0]];
            m_system_timer = m_hardware_timers[non_periodic_timer_indexes[0]];
        } else {
            m_time_keeper_timer = m_hardware_timers[non_periodic_timer_indexes[1]];
            m_system_timer = m_hardware_timers[non_periodic_timer_indexes[0]];
        }
    }

    m_system_timer->change_function([](const RegisterState& regs) { update_scheduler_ticks(regs); });
    dbg() << "Reset timers";
    m_system_timer->try_to_set_frequency(m_system_timer->calculate_nearest_possible_frequency(1024));
    m_time_keeper_timer->change_function([](const RegisterState& regs) { update_time(regs); });
    m_time_keeper_timer->try_to_set_frequency(OPTIMAL_TICKS_PER_SECOND_RATE);

    return true;
}

bool TimeManagement::probe_and_set_legacy_hardware_timers()
{
    if (ACPI::Parser::the().is_operable()) {
        if (ACPI::Parser::the().x86_specific_flags().cmos_rtc_not_present) {
            dbg() << "ACPI: CMOS RTC Not Present";
            return false;
        } else {
            dbg() << "ACPI: CMOS RTC Present";
        }
    }

    m_hardware_timers[0] = PIT::initialize([](const RegisterState& regs) { update_time(regs); });
    m_hardware_timers[1] = RealTimeClock::create([](const RegisterState& regs) { update_scheduler_ticks(regs); });
    m_time_keeper_timer = m_hardware_timers[0];
    m_system_timer = m_hardware_timers[1];
    return true;
}

TimeManagement& TimeManagement::the()
{
    ASSERT(TimeManagement::initialized());
    return *s_time_management;
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
        ++m_epoch_time;
        m_ticks_this_second = 0;
    }
}

void TimeManagement::update_scheduler_ticks(const RegisterState& regs)
{
    TimeManagement::the().update_ticks(regs);
}

void TimeManagement::update_ticks(const RegisterState& regs)
{
    Scheduler::timer_tick(regs);
}
}
