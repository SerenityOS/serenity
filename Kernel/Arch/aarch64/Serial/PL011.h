/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

// Technical Reference Manual for the PrimeCell UART (PL011): https://documentation-service.arm.com/static/5e8e36c2fd977155116a90b5

struct PL011Registers;

class PL011 {
public:
    static ErrorOr<NonnullOwnPtr<PL011>> initialize(PhysicalAddress);

    void send(u32 c);
    u32 receive();

    void print_str(char const*, size_t);

    void set_baud_rate(int baud_rate, int uart_frequency_in_hz);

private:
    PL011(Memory::TypedMapping<PL011Registers volatile>);

    void wait_until_we_can_send();
    void wait_until_we_can_receive();

    Memory::TypedMapping<PL011Registers volatile> m_registers;
};

}
