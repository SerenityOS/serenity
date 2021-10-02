/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Prekernel {

struct UARTRegisters;

// Abstracts the PL011 UART on a Raspberry Pi.
// (The BCM2711 on a Raspberry Pi 4 has five PL011 UARTs; this is always the first of those.)
class UART {
public:
    static UART& the();

    void send(u32 c);
    u32 receive();

    void print_str(const char* s)
    {
        while (*s)
            send(*s++);
    }
    void print_num(u64 n)
    {
        char buf[21];
        int i = 0;
        do {
            buf[i++] = (n % 10) + '0';
            n /= 10;
        } while (n);
        for (i--; i >= 0; i--)
            send(buf[i]);
    }

private:
    UART();

    void set_baud_rate(int baud_rate, int uart_frequency_in_hz);
    void wait_until_we_can_send();
    void wait_until_we_can_receive();

    UARTRegisters volatile* m_registers;
};

}
