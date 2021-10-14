/*
 * Copyright (c) 2021, Undefine <cqundefine@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/MMIO.h>
#include <Kernel/Prekernel/Arch/aarch64/Power.h>

constexpr u32 PM_RSTC = 0x0010001c;
constexpr u32 PM_RSTS = 0x00100020;
constexpr u32 PM_WDOG = 0x00100024;
constexpr u32 PM_WDOG_MAGIC = 0x5a000000;
constexpr u32 PM_RSTC_FULLRST = 0x00000020;

namespace Prekernel {

Power& Power::the()
{
    static Power instance;
    return instance;
}

[[noreturn]] void Power::reset()
{
    u32 r = MMIO::the().read(PM_RSTS);
    r &= ~0xfffffaaa;
    MMIO::the().write(PM_RSTS, PM_WDOG_MAGIC | r);
    MMIO::the().write(PM_WDOG, PM_WDOG_MAGIC | 10);
    MMIO::the().write(PM_RSTC, PM_WDOG_MAGIC | PM_RSTC_FULLRST);

    VERIFY_NOT_REACHED();
}

}
