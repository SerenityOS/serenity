/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/Time/ARMv8Timer.h>

namespace Kernel {

void microseconds_delay(u32 microseconds)
{
    VERIFY(ARMv8Timer::is_initialized());

    auto frequency = ARMv8Timer::the().ticks_per_second();
    VERIFY(frequency != 0);

    // Use the EL1 virtual timer, as that timer should should be accessible to us both on device and in a VM.
    u64 const start = Aarch64::CNTVCT_EL0::read().VirtualCount;
    u64 const delta = (static_cast<u64>(microseconds) * frequency) / 1'000'000ull;

    while ((Aarch64::CNTVCT_EL0::read().VirtualCount - start) < delta)
        Processor::pause();
}

}
