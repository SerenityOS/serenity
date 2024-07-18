/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/EFIPrekernel/DeviceTree.h>
#include <Kernel/EFIPrekernel/Globals.h>

#include <LibDeviceTree/FlattenedDeviceTree.h>

namespace Kernel {

void fill_flattened_devicetree_boot_info(BootInfo* boot_info)
{
    // Search the flattened devicetree configuration table.
    for (FlatPtr i = 0; i < g_efi_system_table->number_of_table_entries; i++) {
        if (g_efi_system_table->configuration_table[i].vendor_guid == EFI::DTB_TABLE_GUID) {
            boot_info->flattened_devicetree_paddr = PhysicalAddress { bit_cast<PhysicalPtr>(g_efi_system_table->configuration_table[i].vendor_table) };
            break;
        }
    }

    if (!boot_info->flattened_devicetree_paddr.is_null()) {
        DeviceTree::FlattenedDeviceTreeHeader* fdt_header = bit_cast<DeviceTree::FlattenedDeviceTreeHeader*>(boot_info->flattened_devicetree_paddr);
        boot_info->flattened_devicetree_size = fdt_header->totalsize;
    }
}

}
