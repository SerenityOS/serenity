/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Prekernel {

// Knows about memory-mapped IO addresses on the Broadcom family of SOCs used in Raspberry Pis.
// RPi3 is the first Raspberry Pi that supports aarch64.
// https://github.com/raspberrypi/documentation/files/1888662/BCM2837-ARM-Peripherals.-.Revised.-.V2-1.pdf (RPi3)
// https://datasheets.raspberrypi.org/bcm2711/bcm2711-peripherals.pdf (RPi4 Model B)
class MMIO {
public:
    static MMIO& the();

    u32 read(FlatPtr offset) { return *peripheral_address(offset); }
    void write(FlatPtr offset, u32 value) { *peripheral_address(offset) = value; }

    u32 volatile* peripheral_address(FlatPtr offset) { return (u32 volatile*)(m_base_address + offset); }
    template<class T>
    T volatile* peripheral(FlatPtr offset) { return (T volatile*)peripheral_address(offset); }

    FlatPtr peripheral_base_address() const { return m_base_address; }
    FlatPtr peripheral_end_address() const { return m_base_address + 0x00FFFFFF; }

private:
    MMIO();

    unsigned int m_base_address;
};

}
