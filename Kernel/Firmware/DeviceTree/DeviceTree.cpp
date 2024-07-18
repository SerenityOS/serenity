/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceTree.h"
#include <AK/NeverDestroyed.h>
#include <AK/Singleton.h>
#include <Kernel/Memory/MemoryManager.h>
#include <LibDeviceTree/DeviceTree.h>
#include <Userland/Libraries/LibDeviceTree/FlattenedDeviceTree.h>
#include <Userland/Libraries/LibDeviceTree/Validation.h>

static Singleton<OwnPtr<DeviceTree::DeviceTree>> s_device_tree;

namespace Kernel::DeviceTree {

alignas(PAGE_SIZE) __attribute__((section(".bss.fdt"))) u8 s_fdt_storage[fdt_storage_size];

static NeverDestroyed<OwnPtr<Memory::Region>> s_flattened_devicetree_region;
static ReadonlyBytes s_flattened_devicetree;

ErrorOr<void> unflatten_fdt()
{
    *s_device_tree = TRY(::DeviceTree::DeviceTree::parse(flattened_devicetree()));

    return {};
}

bool verify_fdt()
{
    static bool verified { false };
    static bool verification_succeeded { false };

    if (verified)
        return verification_succeeded;

    verified = true;
    auto& header = *bit_cast<::DeviceTree::FlattenedDeviceTreeHeader*>(&s_fdt_storage[0]);
    auto fdt = ReadonlyBytes(s_fdt_storage, g_boot_info.flattened_devicetree_size);

    verification_succeeded = ::DeviceTree::validate_flattened_device_tree(header, fdt, ::DeviceTree::Verbose::No);

    return verification_succeeded;
}

void dump_fdt()
{
    auto& header = *bit_cast<::DeviceTree::FlattenedDeviceTreeHeader*>(flattened_devicetree().data());
    MUST(::DeviceTree::dump(header, flattened_devicetree()));
}

ErrorOr<StringView> get_command_line_from_fdt()
{
    auto& header = *bit_cast<::DeviceTree::FlattenedDeviceTreeHeader*>(&s_fdt_storage[0]);
    auto fdt = ReadonlyBytes(s_fdt_storage, g_boot_info.flattened_devicetree_size);
    return TRY(::DeviceTree::slow_get_property("/chosen/bootargs"sv, header, fdt)).as_string();
}

::DeviceTree::DeviceTree const& get()
{
    VERIFY(*s_device_tree);
    return **s_device_tree;
}

void map_flattened_devicetree()
{
    if (g_boot_info.boot_method != BootMethod::EFI) {
        s_flattened_devicetree = ReadonlyBytes { &s_fdt_storage, g_boot_info.flattened_devicetree_size };
        return;
    }

    auto fdt_region_size = MUST(Memory::page_round_up(g_boot_info.flattened_devicetree_size + g_boot_info.flattened_devicetree_paddr.offset_in_page()));
    *s_flattened_devicetree_region = MUST(MM.allocate_mmio_kernel_region(g_boot_info.flattened_devicetree_paddr.page_base(), fdt_region_size, {}, Memory::Region::Access::Read, Memory::MemoryType::Normal));

    s_flattened_devicetree = ReadonlyBytes { (*s_flattened_devicetree_region)->vaddr().offset(g_boot_info.flattened_devicetree_paddr.offset_in_page()).as_ptr(), g_boot_info.flattened_devicetree_size };

    // We didn't verify the FDT at the start of init() if booted via EFI, so do it now.
    auto& header = *bit_cast<::DeviceTree::FlattenedDeviceTreeHeader*>(s_flattened_devicetree.data());
    VERIFY(::DeviceTree::validate_flattened_device_tree(header, s_flattened_devicetree, ::DeviceTree::Verbose::Yes));
}

ReadonlyBytes flattened_devicetree()
{
    return s_flattened_devicetree;
}

}
