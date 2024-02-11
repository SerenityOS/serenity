/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/riscv64/CPU.h>
#include <Kernel/Memory/MemoryManager.h>
#include <LibDeviceTree/DeviceTree.h>
#include <Userland/Libraries/LibDeviceTree/FlattenedDeviceTree.h>
#include <Userland/Libraries/LibDeviceTree/Validation.h>

static Singleton<OwnPtr<DeviceTree::DeviceTree>> s_device_tree;

namespace Kernel {

BootInfo s_boot_info;

alignas(PAGE_SIZE) __attribute__((section(".bss.fdt"))) u8 s_fdt_storage[fdt_storage_size];

ErrorOr<void> unflatten_fdt()
{
    *s_device_tree = TRY(DeviceTree::DeviceTree::parse({ s_fdt_storage, fdt_storage_size }));
    return {};
}

void dump_fdt()
{
    auto& header = *bit_cast<DeviceTree::FlattenedDeviceTreeHeader*>(&s_fdt_storage[0]);
    auto fdt = ReadonlyBytes(s_fdt_storage, header.totalsize);
    MUST(DeviceTree::dump(header, fdt));
}

ErrorOr<StringView> get_command_line_from_fdt()
{
    auto& header = *bit_cast<DeviceTree::FlattenedDeviceTreeHeader*>(&s_fdt_storage[0]);
    auto fdt = ReadonlyBytes(s_fdt_storage, header.totalsize);
    return TRY(DeviceTree::slow_get_property("/chosen/bootargs"sv, header, fdt)).as_string();
}

}

DeviceTree::DeviceTree const& DeviceTree::get()
{
    VERIFY(*s_device_tree);
    return **s_device_tree;
}
