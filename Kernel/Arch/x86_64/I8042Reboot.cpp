/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/x86_64/I8042Reboot.h>
#include <Kernel/Arch/x86_64/IO.h>

namespace Kernel {

void i8042_reboot()
{
    dbgln("attempting reboot via KB Controller...");
    IO::out8(0x64, 0xFE);
}

}
