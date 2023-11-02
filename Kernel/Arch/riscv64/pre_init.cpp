/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

extern "C" [[noreturn]] void pre_init(FlatPtr mhartid, PhysicalPtr fdt_phys_addr);
extern "C" [[noreturn]] void pre_init(FlatPtr mhartid, PhysicalPtr fdt_phys_addr)
{
    (void)mhartid;
    (void)fdt_phys_addr;

    // FIXME: Implement this

    Processor::halt();
}

}
