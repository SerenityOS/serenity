/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/riscv64/CSR.h>
#include <Kernel/Arch/riscv64/Delay.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

namespace Kernel {

static u32 s_timebase_frequency = 0;

void microseconds_delay(u32 microseconds)
{
    VERIFY(s_timebase_frequency != 0);

    u64 const start = RISCV64::CSR::read(RISCV64::CSR::Address::TIME);
    u64 const delta = (static_cast<u64>(microseconds) * s_timebase_frequency) / 1'000'000ull;

    while ((RISCV64::CSR::read(RISCV64::CSR::Address::TIME) - start) < delta)
        Processor::pause();
}

void init_delay_loop()
{
    s_timebase_frequency = DeviceTree::get().resolve_property("/cpus/timebase-frequency"sv).value().as<u32>();
}

}
