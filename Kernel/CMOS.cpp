/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/CMOS.h>

namespace CMOS {

u8 read(u8 index)
{
    IO::out8(0x70, index);
    return IO::in8(0x71);
}

void write(u8 index, u8 data)
{
    IO::out8(0x70, index);
    IO::out8(0x71, data);
}

}
