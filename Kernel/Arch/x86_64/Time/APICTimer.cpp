/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Arch/x86_64/Time/APICTimer.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

#define APIC_TIMER_MEASURE_CPU_CLOCK

UNMAP_AFTER_INIT APICTimer* APICTimer::initialize(u8 interrupt_number, HardwareTimerBase& calibration_source)
{
    auto timer = adopt_lock_ref(*new APICTimer(interrupt_number, nullptr));
    timer->register_interrupt_handler();
    if (!timer->calibrate(calibration_source)) {
        return nullptr;
    }
    return &timer.leak_ref();
}

UNMAP_AFTER_INIT APICTimer::APICTimer(u8 interrupt_number, Function<void()> callback)
    : HardwareTimer<GenericInterruptHandler>(interrupt_number, move(callback))
{
    disable_remap();
}

UNMAP_AFTER_INIT bool APICTimer::calibrate(HardwareTimerBase& calibration_source)
{
    VERIFY_INTERRUPTS_DISABLED();

    dmesgln("APICTimer: Using {} as calibration source", calibration_source.model());

    struct {
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
        bool supports_tsc { Processor::current().has_feature(CPUFeature::TSC) };
#endif
        APIC& apic { APIC::the() };
        size_t ticks_in_100ms { 0 };
        Atomic<size_t, AK::memory_order_relaxed> calibration_ticks { 0 };
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
        u64 volatile start_tsc { 0 }, end_tsc { 0 };
#endif
        u64 volatile start_reference { 0 }, end_reference { 0 };
        u32 volatile start_apic_count { 0 }, end_apic_count { 0 };
        bool query_reference { false };
    } state;

    state.ticks_in_100ms = calibration_source.ticks_per_second() / 10;
    state.query_reference = calibration_source.can_query_raw();

    // temporarily replace the timer callbacks
    auto original_source_callback = calibration_source.set_callback([&state, &calibration_source]() {
        u32 current_timer_count = state.apic.get_timer_current_count();
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
        u64 current_tsc = state.supports_tsc ? read_tsc() : 0;
#endif
        u64 current_reference = state.query_reference ? calibration_source.current_raw() : 0;

        auto prev_tick = state.calibration_ticks.fetch_add(1);
        if (prev_tick == 0) {
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
            state.start_tsc = current_tsc;
#endif
            state.start_apic_count = current_timer_count;
            state.start_reference = current_reference;
        } else if (prev_tick + 1 == state.ticks_in_100ms + 1) {
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
            state.end_tsc = current_tsc;
#endif
            state.end_apic_count = current_timer_count;
            state.end_reference = current_reference;
        }
    });

    // Setup a counter that should be much longer than our calibration time.
    // We don't want the APIC timer to actually fire. We do however want the
    // calbibration_source timer to fire so that we can read the current
    // tick count from the APIC timer
    auto original_callback = set_callback([&]() {
        // TODO: How should we handle this?
        PANIC("APICTimer: Timer fired during calibration!");
    });
    state.apic.setup_local_timer(0xffffffff, APIC::TimerMode::Periodic, true);

    sti();
    // Loop for about 100 ms
    while (state.calibration_ticks.load() <= state.ticks_in_100ms)
        Processor::wait_check();
    cli();

    // Restore timer callbacks
    calibration_source.set_callback(move(original_source_callback));
    set_callback(move(original_callback));

    disable_local_timer();

    if (state.query_reference) {
        u64 one_tick_ns = calibration_source.raw_to_ns((state.end_reference - state.start_reference) / state.ticks_in_100ms);
        m_frequency = (u32)(1000000000ull / one_tick_ns);
        dmesgln("APICTimer: Ticks per second: {} ({}.{}ms)", m_frequency, one_tick_ns / 1000000, one_tick_ns % 1000000);
    } else {
        // For now, assume the frequency is exactly the same
        m_frequency = calibration_source.ticks_per_second();
        dmesgln("APICTimer: Ticks per second: {} (assume same frequency as reference clock)", m_frequency);
    }

    auto delta_apic_count = state.start_apic_count - state.end_apic_count; // The APIC current count register decrements!
    m_timer_period = (delta_apic_count * state.apic.get_timer_divisor()) / state.ticks_in_100ms;

    u64 apic_freq = delta_apic_count * state.apic.get_timer_divisor() * 10;
    dmesgln("APICTimer: Bus clock speed: {}.{} MHz", apic_freq / 1000000, apic_freq % 1000000);
    if (apic_freq < 1000000) {
        dmesgln("APICTimer: Frequency too slow!");
        return false;
    }

#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
    if (state.supports_tsc) {
        auto delta_tsc = (state.end_tsc - state.start_tsc) * 10;
        dmesgln("APICTimer: CPU clock speed: {}.{} MHz", delta_tsc / 1000000, delta_tsc % 1000000);
    }
#endif

    enable_local_timer();
    return true;
}

void APICTimer::enable_local_timer()
{
    APIC::the().setup_local_timer(m_timer_period, m_timer_mode, true);
}

void APICTimer::disable_local_timer()
{
    APIC::the().setup_local_timer(0, APIC::TimerMode::OneShot, false);
}

void APICTimer::set_periodic()
{
    // FIXME: Implement it...
    VERIFY_NOT_REACHED();
}
void APICTimer::set_non_periodic()
{
    // FIXME: Implement it...
    VERIFY_NOT_REACHED();
}

void APICTimer::reset_to_default_ticks_per_second()
{
}

bool APICTimer::try_to_set_frequency([[maybe_unused]] size_t frequency)
{
    return true;
}

bool APICTimer::is_capable_of_frequency([[maybe_unused]] size_t frequency) const
{
    return false;
}

size_t APICTimer::calculate_nearest_possible_frequency([[maybe_unused]] size_t frequency) const
{
    return 0;
}

}
