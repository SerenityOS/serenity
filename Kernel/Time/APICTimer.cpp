/*
 * Copyright (c) 2020, the SerenityOS developers
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

#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <Kernel/Time/APICTimer.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

#define APIC_TIMER_MEASURE_CPU_CLOCK

UNMAP_AFTER_INIT APICTimer* APICTimer::initialize(u8 interrupt_number, HardwareTimerBase& calibration_source)
{
    auto timer = adopt(*new APICTimer(interrupt_number, nullptr));
    timer->register_interrupt_handler();
    if (!timer->calibrate(calibration_source)) {
        return nullptr;
    }
    return &timer.leak_ref();
}

UNMAP_AFTER_INIT APICTimer::APICTimer(u8 interrupt_number, Function<void(const RegisterState&)> callback)
    : HardwareTimer<GenericInterruptHandler>(interrupt_number, move(callback))
{
    disable_remap();
}

UNMAP_AFTER_INIT bool APICTimer::calibrate(HardwareTimerBase& calibration_source)
{
    VERIFY_INTERRUPTS_DISABLED();

    klog() << "APICTimer: Using " << calibration_source.model() << " as calibration source";

    auto& apic = APIC::the();
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
    bool supports_tsc = Processor::current().has_feature(CPUFeature::TSC);
#endif

    // temporarily replace the timer callbacks
    const size_t ticks_in_100ms = calibration_source.ticks_per_second() / 10;
    Atomic<size_t> calibration_ticks = 0;
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
    volatile u64 start_tsc = 0, end_tsc = 0;
#endif
    volatile u32 start_apic_count = 0, end_apic_count = 0;
    auto original_source_callback = calibration_source.set_callback([&](const RegisterState&) {
        u32 current_timer_count = apic.get_timer_current_count();
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
        u64 current_tsc = supports_tsc ? read_tsc() : 0;
#endif

        auto prev_tick = calibration_ticks.fetch_add(1, AK::memory_order_acq_rel);
        if (prev_tick == 0) {
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
            start_tsc = current_tsc;
#endif
            start_apic_count = current_timer_count;
        } else if (prev_tick + 1 == ticks_in_100ms) {
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
            end_tsc = current_tsc;
#endif
            end_apic_count = current_timer_count;
        }
    });

    // Setup a counter that should be much longer than our calibration time.
    // We don't want the APIC timer to actually fire. We do however want the
    // calbibration_source timer to fire so that we can read the current
    // tick count from the APIC timer
    auto original_callback = set_callback([&](const RegisterState&) {
        klog() << "APICTimer: Timer fired during calibration!";
        VERIFY_NOT_REACHED(); // TODO: How should we handle this?
    });
    apic.setup_local_timer(0xffffffff, APIC::TimerMode::Periodic, true);

    sti();
    // Loop for about 100 ms
    while (calibration_ticks.load(AK::memory_order_relaxed) < ticks_in_100ms)
        ;
    cli();

    // Restore timer callbacks
    calibration_source.set_callback(move(original_source_callback));
    set_callback(move(original_callback));

    disable_local_timer();

    auto delta_apic_count = start_apic_count - end_apic_count; // The APIC current count register decrements!
    m_timer_period = (delta_apic_count * apic.get_timer_divisor()) / ticks_in_100ms;

    auto apic_freq = (delta_apic_count * apic.get_timer_divisor()) / apic.get_timer_divisor();
    if (apic_freq < 1000000) {
        klog() << "APICTimer: Frequency too slow!";
        return false;
    }
    klog() << "APICTimer: Bus clock speed: " << (apic_freq / 1000000) << "." << (apic_freq % 1000000) << " MHz";
#ifdef APIC_TIMER_MEASURE_CPU_CLOCK
    if (supports_tsc) {
        auto delta_tsc = end_tsc - start_tsc;
        klog() << "APICTimer: CPU clock speed: " << (delta_tsc / 1000000) << "." << (delta_tsc % 1000000) << " MHz";
    }
#endif

    // TODO: measure rather than assuming it matches?
    m_frequency = calibration_source.ticks_per_second();

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
