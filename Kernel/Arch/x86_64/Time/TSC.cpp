/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TSC.h"
#include <AK/Singleton.h>
#include <Kernel/Arch/Processor.h>

namespace Kernel {

TSC& TSC::the()
{
    static Singleton<TSC> s_the {};
    return *s_the;
}

bool TSC::is_supported_and_invariant()
{
    bool has_sufficient_tsc = Processor::current().has_feature(CPUFeature::TSC)
        && Processor::current().has_feature(CPUFeature::CONSTANT_TSC)
        && Processor::current().has_feature(CPUFeature::NONSTOP_TSC);

    if (!has_sufficient_tsc)
        dmesgln("TSC: TSC is unsupported or not invariant");

    return has_sufficient_tsc;
}

bool TSC::calibrate(HardwareTimerBase& calibration_source)
{
    VERIFY_INTERRUPTS_DISABLED();

    // FIXME: Intel CPUs provide the frequency as a CPUID leaf, we could use that instead of doing the calibration.

    struct {
        size_t ticks_in_100ms { 0 };
        Atomic<size_t, AK::memory_order_relaxed> calibration_ticks { 0 };
        u64 volatile start_tsc { 0 }, end_tsc { 0 };
        u64 volatile start_reference { 0 }, end_reference { 0 };
        u32 volatile start_apic_count { 0 }, end_apic_count { 0 };
        bool query_reference { false };
    } state;

    state.ticks_in_100ms = calibration_source.ticks_per_second() / 10;

    if (!calibration_source.can_query_raw())
        return false;

    auto original_source_callback = calibration_source.set_callback([&state, &calibration_source]() {
        u64 current_tsc = read_tsc();
        auto prev_tick = state.calibration_ticks.fetch_add(1);
        if (prev_tick == 0) {
            state.start_tsc = current_tsc;
            state.start_reference = calibration_source.current_raw();
        } else if (prev_tick == state.ticks_in_100ms) {
            state.end_tsc = current_tsc;
            state.end_reference = calibration_source.current_raw();
        }
    });

    Processor::enable_interrupts();
    while (state.calibration_ticks.load() <= state.ticks_in_100ms)
        Processor::wait_check();
    Processor::disable_interrupts();

    calibration_source.set_callback(move(original_source_callback));

    u64 measurement_duration = calibration_source.raw_to_ns(state.end_reference - state.start_reference);
    m_frequency = (u32)(1000'000'000ull * (state.end_tsc - state.start_tsc) / measurement_duration);

    dmesgln("TSC: Measured base frequency: {}MHz", (m_frequency + 500'000) / 1000'000);

    return true;
}

u64 TSC::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = read_tsc();
    u64 delta_ticks { 0 };
    if (current_value >= m_main_counter_last_read) {
        delta_ticks += current_value - m_main_counter_last_read;
    } else {
        // the counter wrapped around
        delta_ticks += (NumericLimits<u64>::max() - m_main_counter_last_read + 1) + current_value;
    }

    u64 ticks_since_last_second = (u64)ticks_this_second + delta_ticks;
    seconds_since_boot += ticks_since_last_second / m_frequency;
    ticks_this_second = ticks_since_last_second % m_frequency;

    if (!query_only) {
        m_main_counter_last_read = current_value;
    }

    // Return the time passed (in ns) since last time update_time was called
    return (delta_ticks * 1000000000ull) / m_frequency;
}

}
