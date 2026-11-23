/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/TimeDeviceBase.h>

namespace Kernel {

u64 TimeDeviceBase::measure_frequency(HardwareTimerBase& source, AK::Function<u64()> read_counter, CounterDirection direction) const
{
    constexpr u64 CALIBRATION_DURATION_NS = 100'000'000;

    dbgln("{}: Calibrating using {}", model(), source.model());

    auto start = read_counter();
    auto start_reference = source.current_raw();

    // NOTE: The raw counter does not run at source.frequency() (that is the tick rate), so use raw_to_ns().
    while (source.raw_to_ns(source.current_raw() - start_reference) < CALIBRATION_DURATION_NS)
        Processor::wait_check();

    auto end = read_counter();
    auto end_reference = source.current_raw();

    VERIFY(direction == CounterDirection::Down ? start > end : end > start);

    auto elapsed_ns = source.raw_to_ns(end_reference - start_reference);
    VERIFY(elapsed_ns > 0);

    auto counter_delta = direction == CounterDirection::Down ? start - end : end - start;
    auto frequency = counter_delta * 1'000'000'000ull / elapsed_ns;

    dmesgln("{}: Measured base frequency: {}MHz", model(), (frequency + 500'000) / 1'000'000);
    return frequency;
}

}
