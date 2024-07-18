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

alignas(PAGE_SIZE) __attribute__((section(".bss.fdt"))) u8 s_fdt_storage[fdt_storage_size];

static Singleton<OwnPtr<Memory::Region>> s_fdt_region;
static VirtualAddress s_fdt_addr { nullptr };

ErrorOr<void> unflatten_fdt()
{
    if (g_boot_info.boot_method == BootMethod::PreInit) {
        s_fdt_addr = VirtualAddress { &s_fdt_storage };
    } else {
        auto fdt_region_size = TRY(Memory::page_round_up(g_boot_info.flattened_devicetree_size + g_boot_info.flattened_devicetree_paddr.offset_in_page()));
        *s_fdt_region = TRY(MM.allocate_mmio_kernel_region(g_boot_info.flattened_devicetree_paddr.page_base(), fdt_region_size, {}, Memory::Region::Access::Read));

        s_fdt_addr = (*s_fdt_region)->vaddr().offset(g_boot_info.flattened_devicetree_paddr.offset_in_page());
    }

    *s_device_tree = TRY(DeviceTree::DeviceTree::parse({ s_fdt_addr.as_ptr(), g_boot_info.flattened_devicetree_size }));

    return {};
}

void dump_fdt()
{
    auto& header = *bit_cast<DeviceTree::FlattenedDeviceTreeHeader*>(s_fdt_addr.as_ptr());
    auto fdt = ReadonlyBytes(s_fdt_addr.as_ptr(), g_boot_info.flattened_devicetree_size);
    MUST(DeviceTree::dump(header, fdt));
}

ErrorOr<StringView> get_command_line_from_fdt()
{
    VERIFY(g_boot_info.boot_method == BootMethod::PreInit);
    auto& header = *bit_cast<DeviceTree::FlattenedDeviceTreeHeader*>(&s_fdt_storage[0]);
    auto fdt = ReadonlyBytes(s_fdt_storage, g_boot_info.flattened_devicetree_size);
    return TRY(DeviceTree::slow_get_property("/chosen/bootargs"sv, header, fdt)).as_string();
}

}

DeviceTree::DeviceTree const& DeviceTree::get()
{
    VERIFY(*s_device_tree);
    return **s_device_tree;
}
