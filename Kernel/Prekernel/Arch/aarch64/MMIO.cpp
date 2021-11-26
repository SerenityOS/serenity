/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/MMIO.h>
#include <Kernel/Prekernel/Arch/aarch64/MainIdRegister.h>

namespace Prekernel {

MMIO::MMIO()
    : m_base_address(0xFE00'0000)
{
    MainIdRegister id;
    if (id.part_num() <= MainIdRegister::RaspberryPi3)
        m_base_address = 0x3F00'0000;
}

MMIO& MMIO::the()
{
    static MMIO instance;
    return instance;
}

}
