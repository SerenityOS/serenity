/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// I²C-bus specification and user manual: https://www.nxp.com/docs/en/user-guide/UM10204.pdf

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>

namespace Kernel::I2C {

AK_TYPEDEF_DISTINCT_ORDERED_ID(u16, Address);

enum class DataDirection : u8 {
    Read,
    Write,
};

constexpr u8 seven_bit_address_byte(Address address, DataDirection data_direction)
{
    VERIFY(address <= 0x7f);

    // 3.1.10 The target address and R/W bit
    // "After the START condition (S), a target address is sent. This address is seven bits long followed by an
    //  eighth bit which is a data direction bit (R/W̅) — a ‘zero’ indicates a transmission (WRITE),
    //  a ‘one’ indicates a request for data (READ) (refer to Figure 10)."
    u8 rw_bit = data_direction == DataDirection::Write ? 0 : 1;
    return (address.value() << 1) | rw_bit;
}

}
