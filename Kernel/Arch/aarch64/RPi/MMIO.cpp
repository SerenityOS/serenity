/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/CPUID.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>

namespace Kernel::RPi {

MMIO::MMIO()
    : m_base_address(0xFE00'0000)
{
    auto main_id_register = Aarch64::MIDR_EL1::read();
    if (static_cast<ArmLimited>(main_id_register.PartNum) <= ArmLimited::Cortex_A53) // Raspberry Pi 3
        m_base_address = PhysicalAddress(0x3F00'0000);
}

MMIO& MMIO::the()
{
    static MMIO instance;
    return instance;
}

}
