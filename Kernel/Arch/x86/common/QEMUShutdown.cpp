/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Arch/x86/common/QEMUShutdown.h>

namespace Kernel {

void qemu_shutdown()
{
    // Note: This will invoke QEMU Shutdown, but for other platforms (or emulators),
    // this has no effect on the system.
    // We also try the Bochs/Old QEMU shutdown method, if the first didn't work.
    IO::out16(0x604, 0x2000);
    IO::out16(0xb004, 0x2000);
}

}
