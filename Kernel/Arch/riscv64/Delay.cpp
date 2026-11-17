/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/riscv64/CSR.h>
#include <Kernel/Arch/riscv64/Delay.h>
#include <Kernel/Arch/riscv64/Timer.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

namespace Kernel {

void microseconds_delay(u32 microseconds)
{
    VERIFY(RISCV64::Timer::is_initialized());

    auto frequency = RISCV64::Timer::the().ticks_per_second();
    VERIFY(frequency != 0);

    u64 const start = RISCV64::CSR::read<RISCV64::CSR::Address::TIME>();
    u64 const delta = (static_cast<u64>(microseconds) * frequency) / 1'000'000ull;

    while ((RISCV64::CSR::read<RISCV64::CSR::Address::TIME>() - start) < delta)
        Processor::pause();
}

}
