/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/MainIdRegister.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>

namespace Kernel::RPi {

MMIO::MMIO()
    : m_base_address(0xFE00'0000)
{
    MainIdRegister id;
    if (id.part_num() <= MainIdRegister::RaspberryPi3)
        m_base_address = PhysicalAddress(0x3F00'0000);
}

MMIO& MMIO::the()
{
    static MMIO instance;
    return instance;
}

}
