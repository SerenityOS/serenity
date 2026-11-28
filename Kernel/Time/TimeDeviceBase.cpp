/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/TimeDeviceBase.h>

namespace Kernel {

u64 TimeDeviceBase::measure_frequency(TimeSource& source, AK::Function<u64()> read_counter, CounterDirection direction) const
{
    constexpr u64 CALIBRATION_DURATION_NS = 100'000'000;

    dbgln("{}: Calibrating using {}", model(), source.model());

    auto calibration_frequency = source.frequency();
    auto target_source_ticks = CALIBRATION_DURATION_NS * calibration_frequency / 1'000'000'000ull;

    auto start = read_counter();
    auto start_reference = source.read_counter();

    while (source.read_counter() < start_reference + target_source_ticks)
        Processor::wait_check();

    auto end = read_counter();
    auto end_reference = source.read_counter();

    VERIFY(direction == CounterDirection::Down ? start > end : end > start);
    VERIFY(end_reference > start_reference);

    auto counter_delta = direction == CounterDirection::Down ? start - end : end - start;
    auto frequency = counter_delta * calibration_frequency / (end_reference - start_reference);

    dmesgln("{}: Measured base frequency: {}MHz", model(), (frequency + 500'000) / 1'000'000);
    return frequency;
}

}
