/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/APICTimer.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

#define APIC_TIMER_MEASURE_CPU_CLOCK

UNMAP_AFTER_INIT APICTimer* APICTimer::initialize(u8 interrupt_number, HardwareTimerBase& calibration_source)
{
    auto timer = adopt_ref(*new APICTimer(interrupt_number, nullptr));
    timer->register_interrupt_handler();
    if (!timer->calibrate(calibration_source)) {
        return nullptr;
    }
    return &timer.leak_ref();
}

UNMAP_AFTER_INIT APICTimer::APICTimer(u8 interrupt_number, Function<void(RegisterState const&)> callback)
    : HardwareTimer<GenericInterruptHandler>(interrupt_number, move(callback))
{
    disable_remap();
}

UNMAP_AFTER_INIT bool APICTimer::calibrate(HardwareTimerBase& calibration_source)
{
    VERIFY_INTERRUPTS_DISABLED();

    dmesgln("APICTimer: Using {} as calibration source", calibration_source.model());

    auto& apic = APIC::the();
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
    bool supports_tsc = Processor::current().has_feature(CPUFeature::TSC);
#endif

    // temporarily replace the timer callbacks
    const size_t ticks_in_100ms = calibration_source.ticks_per_second() / 10;
    Atomic<size_t, AK::memory_order_relaxed> calibration_ticks = 0;
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
    volatile u64 start_tsc = 0, end_tsc = 0;
#endif
    volatile u64 start_reference = 0, end_reference = 0;
    volatile u32 start_apic_count = 0, end_apic_count = 0;
    bool query_reference = calibration_source.can_query_raw();
    auto original_source_callback = calibration_source.set_callback([&](RegisterState const&) {
        u32 current_timer_count = apic.get_timer_current_count();
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
        u64 current_tsc = supports_tsc ? read_tsc() : 0;
#endif
        u64 current_reference = query_reference ? calibration_source.current_raw() : 0;

        auto prev_tick = calibration_ticks.fetch_add(1);
        if (prev_tick == 0) {
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
            start_tsc = current_tsc;
#endif
            start_apic_count = current_timer_count;
            start_reference = current_reference;
        } else if (prev_tick + 1 == ticks_in_100ms + 1) {
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
            end_tsc = current_tsc;
#endif
            end_apic_count = current_timer_count;
            end_reference = current_reference;
        }
    });

    // Setup a counter that should be much longer than our calibration time.
    // We don't want the APIC timer to actually fire. We do however want the
    // calbibration_source timer to fire so that we can read the current
    // tick count from the APIC timer
    auto original_callback = set_callback([&](RegisterState const&) {
        // TODO: How should we handle this?
        PANIC("APICTimer: Timer fired during calibration!");
    });
    apic.setup_local_timer(0xffffffff, APIC::TimerMode::Periodic, true);

    sti();
    // Loop for about 100 ms
    while (calibration_ticks.load() <= ticks_in_100ms)
        ;
    cli();

    // Restore timer callbacks
    calibration_source.set_callback(move(original_source_callback));
    set_callback(move(original_callback));

    disable_local_timer();

    if (query_reference) {
        u64 one_tick_ns = calibration_source.raw_to_ns((end_reference - start_reference) / ticks_in_100ms);
        m_frequency = (u32)(1000000000ull / one_tick_ns);
        dmesgln("APICTimer: Ticks per second: {} ({}.{}ms)", m_frequency, one_tick_ns / 1000000, one_tick_ns % 1000000);
    } else {
        // For now, assume the frequency is exactly the same
        m_frequency = calibration_source.ticks_per_second();
        dmesgln("APICTimer: Ticks per second: {} (assume same frequency as reference clock)", m_frequency);
    }

    auto delta_apic_count = start_apic_count - end_apic_count; // The APIC current count register decrements!
    m_timer_period = (delta_apic_count * apic.get_timer_divisor()) / ticks_in_100ms;

    u64 apic_freq = delta_apic_count * apic.get_timer_divisor() * 10;
    dmesgln("APICTimer: Bus clock speed: {}.{} MHz", apic_freq / 1000000, apic_freq % 1000000);
    if (apic_freq < 1000000) {
        dmesgln("APICTimer: Frequency too slow!");
        return false;
    }

#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
    if (supports_tsc) {
        auto delta_tsc = (end_tsc - start_tsc) * 10;
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

size_t APICTimer::ticks_per_second() const
{
    return m_frequency;
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
