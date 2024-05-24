/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/DebugOutput.h>
#include <Kernel/Arch/x86_64/BochsDebugOutput.h>
#include <Kernel/Arch/x86_64/IO.h>

#if !defined(PREKERNEL)
#    include <Kernel/Arch/Processor.h>
#endif

namespace Kernel {

static constexpr u16 serial_com1_io_port = 0x3F8;

void bochs_debug_output(char ch)
{
    IO::out8(IO::BOCHS_DEBUG_PORT, ch);
}

void debug_output(char ch)
{
    static bool serial_ready = false;
    static bool was_cr = false;

    if (!serial_ready) {
        IO::out8(serial_com1_io_port + 1, 0x00);
        IO::out8(serial_com1_io_port + 3, 0x80);
        IO::out8(serial_com1_io_port + 0, 0x02);
        IO::out8(serial_com1_io_port + 1, 0x00);
        IO::out8(serial_com1_io_port + 3, 0x03);
        IO::out8(serial_com1_io_port + 2, 0xC7);
        IO::out8(serial_com1_io_port + 4, 0x0B);

        serial_ready = true;
    }

    while ((IO::in8(serial_com1_io_port + 5) & 0x20) == 0) {
#if !defined(PREKERNEL)
        Processor::wait_check();
#else
        ;
#endif
    }

    if (ch == '\n' && !was_cr)
        IO::out8(serial_com1_io_port, '\r');

    IO::out8(serial_com1_io_port, ch);

    was_cr = ch == '\r';
}

}
