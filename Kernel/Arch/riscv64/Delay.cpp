/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/riscv64/CSR.h>

namespace Kernel {

void microseconds_delay(u32 microseconds)
{
    // FIXME: actually delay in micoseconds

    u64 const start = RISCV64::CSR::read(RISCV64::CSR::Address::TIME);

    while ((RISCV64::CSR::read(RISCV64::CSR::Address::TIME) - start) < static_cast<u64>(microseconds) * 100) {
        // Processor::pause();
    }
}

}
