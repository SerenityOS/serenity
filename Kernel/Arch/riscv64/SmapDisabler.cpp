/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/Arch/riscv64/CSR.h>

namespace Kernel {

SmapDisabler::SmapDisabler()
    : m_flags(RISCV64::CSR::read_and_set_bits(RISCV64::CSR::Address::SSTATUS, 1 << to_underlying(RISCV64::CSR::SSTATUS::Offset::SUM)))
{
}

SmapDisabler::~SmapDisabler()
{
    if ((m_flags & (1 << to_underlying(RISCV64::CSR::SSTATUS::Offset::SUM))) == 0)
        RISCV64::CSR::clear_bits(RISCV64::CSR::Address::SSTATUS, 1 << to_underlying(RISCV64::CSR::SSTATUS::Offset::SUM));
}

}
