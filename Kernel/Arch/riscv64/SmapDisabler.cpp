/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/Arch/riscv64/CSR.h>

namespace Kernel {

SmapDisabler::SmapDisabler()
    : m_flags(RISCV64::CSR::read_and_set_bits<RISCV64::CSR::Address::SSTATUS>(to_underlying(RISCV64::CSR::SSTATUS::Bit::SUM)))
{
}

SmapDisabler::~SmapDisabler()
{
    if ((m_flags & to_underlying(RISCV64::CSR::SSTATUS::Bit::SUM)) == 0)
        RISCV64::CSR::clear_bits<RISCV64::CSR::Address::SSTATUS>(RISCV64::CSR::SSTATUS::Bit::SUM);
}

}
