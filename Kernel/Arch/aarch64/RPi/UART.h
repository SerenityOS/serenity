/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

struct UARTRegisters;

// Abstracts the PL011 UART on a Raspberry Pi.
// (The BCM2711 on a Raspberry Pi 4 has five PL011 UARTs; this is always the first of those.)
class UART {
public:
    static UART& the();

    void send(u32 c);
    u32 receive();

    void print_str(char const* s, size_t length)
    {
        for (size_t i = 0; i < length; ++i)
            send(*s++);
    }

private:
    UART();

    void set_baud_rate(int baud_rate, int uart_frequency_in_hz);
    void wait_until_we_can_send();
    void wait_until_we_can_receive();

    UARTRegisters volatile* m_registers;
};

}
