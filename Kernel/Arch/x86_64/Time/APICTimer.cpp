/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Arch/x86_64/Time/APICTimer.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

#define APIC_TIMER_MEASURE_CPU_CLOCK

UNMAP_AFTER_INIT APICTimer* APICTimer::initialize(InterruptNumber interrupt_number, HardwareTimerBase& calibration_source)
{
    auto timer = adopt_lock_ref(*new APICTimer(interrupt_number, nullptr));
    timer->register_interrupt_handler();

    APIC::the().setup_local_timer(NumericLimits<u32>::max(), TimerMode::OneShot, false);
    APIC::the().set_timer_count(NumericLimits<u32>::max());

    auto counter_frequency = timer->measure_frequency(calibration_source, []() {
        auto count = APIC::the().get_timer_current_count();
        VERIFY(count != 0);
        return count; }, CounterDirection::Down);

    dbgln("LocalAPIC: Bus speed: {} MHz", (counter_frequency * APIC::the().get_timer_divisor() + 500'000) / 1'000'000);

    auto period = (counter_frequency + OPTIMAL_TICKS_PER_SECOND_RATE / 2) / OPTIMAL_TICKS_PER_SECOND_RATE;
    if (period == 0 || period > NumericLimits<u32>::max()) {
        dmesgln("LocalAPIC: Counter frequency {} Hz cannot support {} Hz", counter_frequency, OPTIMAL_TICKS_PER_SECOND_RATE);
        timer->disable_local_timer();
        return nullptr;
    }

    timer->m_timer_period = static_cast<u32>(period);
    timer->m_frequency = counter_frequency / timer->m_timer_period;
    dmesgln("LocalAPIC: Timer period {} counts at approximately {} Hz", timer->m_timer_period, timer->m_frequency);
    timer->enable_local_timer();

    return &timer.leak_ref();
}

UNMAP_AFTER_INIT APICTimer::APICTimer(InterruptNumber interrupt_number, Function<void()> callback)
    : HardwareTimer<GenericInterruptHandler>(interrupt_number, move(callback))
{
    disable_remap();
}

void APICTimer::enable_local_timer()
{
    APIC::the().setup_local_timer(m_timer_period, m_timer_mode, true);
}

void APICTimer::disable_local_timer()
{
    APIC::the().setup_local_timer(0, TimerMode::OneShot, false);
}

size_t APICTimer::frequency() const
{
    return m_frequency;
}

}
