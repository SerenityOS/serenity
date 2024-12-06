/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

struct UARTRegisters;

// Abstracts the PL011 UART on a Raspberry Pi.
// (The BCM2711 on a Raspberry Pi 4 has five PL011 UARTs; this is always the first of those.)
class UART {
public:
    static ErrorOr<NonnullOwnPtr<UART>> initialize(PhysicalAddress);

    void send(u32 c);
    u32 receive();

    void print_str(char const*, size_t);

    void set_baud_rate(int baud_rate, int uart_frequency_in_hz);

private:
    UART(Memory::TypedMapping<UARTRegisters volatile>);

    void wait_until_we_can_send();
    void wait_until_we_can_receive();

    Memory::TypedMapping<UARTRegisters volatile> m_registers;
};

}
