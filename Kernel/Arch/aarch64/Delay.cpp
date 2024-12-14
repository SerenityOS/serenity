/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/Processor.h>

namespace Kernel {

void microseconds_delay(u32 microseconds)
{
    auto frequency = Aarch64::CNTFRQ_EL0::read().ClockFrequency;

    // TODO: Fall back to the devicetree clock-frequency property.
    VERIFY(frequency != 0);

    // Use the EL1 virtual timer, as that timer should should be accessible to us both on device and in a VM.
    u64 const start = Aarch64::CNTVCT_EL0::read().VirtualCount;
    u64 const delta = (static_cast<u64>(microseconds) * frequency) / 1'000'000ull;

    while ((Aarch64::CNTVCT_EL0::read().VirtualCount - start) < delta)
        Processor::pause();
}

}
