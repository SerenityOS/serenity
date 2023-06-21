/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/x86_64/IO.h>

namespace Kernel {

void microseconds_delay(u32 microseconds)
{
    IO::delay(microseconds);
}

}
