/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NeverDestroyed.h>
#include <Kernel/Arch/riscv64/SBI.h>
#include <Kernel/Arch/riscv64/Timer.h>

namespace Kernel::RISCV64 {

Timer::Timer()
    : HardwareTimer(to_underlying(CSR::SCAUSE::SupervisorTimerInterrupt) & ~CSR::SCAUSE_INTERRUPT_MASK)
{
    // FIXME: Actually query the frequency of the timer from the device tree.
    // Based on the "/cpus/timebase-frequency" device tree node for the QEMU virt machine
    m_frequency = 10'000'000; // in Hz

    set_interrupt_interval_usec(m_frequency / OPTIMAL_TICKS_PER_SECOND_RATE);

    set_compare(microseconds_since_boot() + m_interrupt_interval);
    RISCV64::CSR::set_bits(RISCV64::CSR::Address::SIE, 1 << interrupt_number());
}

NonnullLockRefPtr<Timer> Timer::initialize()
{
    auto timer = adopt_lock_ref(*new Timer);
    timer->register_interrupt_handler();
    return timer;
}

u64 Timer::microseconds_since_boot()
{
    return RISCV64::CSR::read(RISCV64::CSR::Address::TIME);
}

bool Timer::handle_interrupt(RegisterState const& regs)
{
    auto result = HardwareTimer::handle_interrupt(regs);

    set_compare(microseconds_since_boot() + m_interrupt_interval);

    return result;
}

u64 Timer::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = microseconds_since_boot();
    u64 delta_ticks = m_main_counter_drift;
    if (current_value >= m_main_counter_last_read) {
        delta_ticks += current_value - m_main_counter_last_read;
    } else {
        // the counter wrapped around
        delta_ticks += (NumericLimits<u64>::max() - m_main_counter_last_read + 1) + current_value;
    }

    u64 ticks_since_last_second = (u64)ticks_this_second + delta_ticks;
    auto ticks_per_second = frequency();
    seconds_since_boot += ticks_since_last_second / ticks_per_second;
    ticks_this_second = ticks_since_last_second % ticks_per_second;

    if (!query_only) {
        m_main_counter_drift = 0;
        m_main_counter_last_read = current_value;
    }

    // Return the time passed (in ns) since last time update_time was called
    return (delta_ticks * 1000000000ull) / ticks_per_second;
}

void Timer::set_interrupt_interval_usec(u32 interrupt_interval)
{
    m_interrupt_interval = interrupt_interval;
}

void Timer::set_compare(u64 compare)
{
    if (SBI::Timer::set_timer(compare).is_error())
        MUST(SBI::Legacy::set_timer(compare));
}

}

namespace Kernel {

bool HardwareTimer<GenericInterruptHandler>::eoi()
{
    return true;
}

}
