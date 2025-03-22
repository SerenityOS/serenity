/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/riscv64/SBI.h>
#include <Kernel/Arch/riscv64/Timer.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

namespace Kernel::RISCV64 {

static Timer* s_the;

Timer::Timer()
{
    m_frequency = DeviceTree::get().resolve_property("/cpus/timebase-frequency"sv).value().as<u32>();

    m_interrupt_interval = m_frequency / OPTIMAL_TICKS_PER_SECOND_RATE;

    set_compare(current_ticks() + m_interrupt_interval);
    RISCV64::CSR::set_bits(RISCV64::CSR::Address::SIE, 1 << (to_underlying(CSR::SCAUSE::SupervisorTimerInterrupt) & ~CSR::SCAUSE_INTERRUPT_MASK));
}

NonnullLockRefPtr<Timer> Timer::initialize()
{
    VERIFY(!s_the);
    auto timer = adopt_lock_ref(*new Timer);
    s_the = timer.ptr();
    return timer;
}

Timer& Timer::the()
{
    VERIFY(s_the);
    return *s_the;
}

u64 Timer::current_ticks()
{
    return RISCV64::CSR::read(RISCV64::CSR::Address::TIME);
}

void Timer::handle_interrupt()
{
    if (m_callback)
        m_callback();
    set_compare(current_ticks() + m_interrupt_interval);
}

void Timer::disable()
{
    RISCV64::CSR::clear_bits(RISCV64::CSR::Address::SIE, 1 << (to_underlying(CSR::SCAUSE::SupervisorTimerInterrupt) & ~CSR::SCAUSE_INTERRUPT_MASK));
}

u64 Timer::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = current_ticks();
    u64 delta_ticks = m_main_counter_drift;
    if (current_value >= m_main_counter_last_read) {
        delta_ticks += current_value - m_main_counter_last_read;
    } else {
        // the counter wrapped around
        delta_ticks += (NumericLimits<u64>::max() - m_main_counter_last_read + 1) + current_value;
    }

    u64 ticks_since_last_second = (u64)ticks_this_second + delta_ticks;
    auto frequency = ticks_per_second();
    seconds_since_boot += ticks_since_last_second / frequency;
    ticks_this_second = ticks_since_last_second % frequency;

    if (!query_only) {
        m_main_counter_drift = 0;
        m_main_counter_last_read = current_value;
    }

    // Return the time passed (in ns) since last time update_time was called
    return (delta_ticks * 1000000000ull) / frequency;
}

void Timer::set_compare(u64 compare)
{
    if (SBI::Timer::set_timer(compare).is_error())
        MUST(SBI::Legacy::set_timer(compare));
}

}
