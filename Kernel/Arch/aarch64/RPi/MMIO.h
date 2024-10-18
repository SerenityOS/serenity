/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel::RPi {

// Knows about memory-mapped IO addresses on the Broadcom family of SOCs used in Raspberry Pis.
// RPi3 is the first Raspberry Pi that supports aarch64.
// https://github.com/raspberrypi/documentation/files/1888662/BCM2837-ARM-Peripherals.-.Revised.-.V2-1.pdf (RPi3)
// https://datasheets.raspberrypi.org/bcm2711/bcm2711-peripherals.pdf (RPi4 Model B)
class MMIO {
public:
    static MMIO& the();

    template<typename T>
    ErrorOr<Memory::TypedMapping<T volatile>> peripheral(FlatPtr offset)
    {
        return Memory::map_typed_writable<T volatile>(PhysicalAddress { m_base_address.offset(offset) });
    }

private:
    MMIO();

    PhysicalAddress m_base_address;
};

}
