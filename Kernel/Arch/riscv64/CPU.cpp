/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/CPU.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Userland/Libraries/LibDeviceTree/FlattenedDeviceTree.h>
#include <Userland/Libraries/LibDeviceTree/Validation.h>

namespace Kernel {

BootInfo s_boot_info;

alignas(PAGE_SIZE) __attribute__((section(".bss.fdt"))) u8 s_fdt_storage[fdt_storage_size];

void dump_fdt()
{
    auto& header = *bit_cast<DeviceTree::FlattenedDeviceTreeHeader*>(&s_fdt_storage[0]);
    auto fdt = ReadonlyBytes(s_fdt_storage, header.totalsize);
    MUST(DeviceTree::dump(header, fdt));
}

}
