/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/EFIPrekernel/ConfigurationTable.h>
#include <Kernel/EFIPrekernel/Globals.h>

#include <LibDeviceTree/FlattenedDeviceTree.h>

namespace Kernel {

static void* search_efi_configuration_table(EFI::GUID guid)
{
    for (FlatPtr i = 0; i < g_efi_system_table->number_of_table_entries; i++) {
        if (g_efi_system_table->configuration_table[i].vendor_guid == guid)
            return g_efi_system_table->configuration_table[i].vendor_table;
    }

    return nullptr;
}

void populate_devicetree_and_acpi_boot_info(BootInfo* boot_info)
{
    boot_info->flattened_devicetree_paddr = PhysicalAddress { bit_cast<PhysicalPtr>(search_efi_configuration_table(EFI::DTB_TABLE_GUID)) };
    if (!boot_info->flattened_devicetree_paddr.is_null()) {
        DeviceTree::FlattenedDeviceTreeHeader* fdt_header = reinterpret_cast<DeviceTree::FlattenedDeviceTreeHeader*>(boot_info->flattened_devicetree_paddr.as_ptr());
        boot_info->flattened_devicetree_size = fdt_header->totalsize;
    }

    // Prefer ACPI 2.0.
    boot_info->acpi_rsdp_paddr = PhysicalAddress { bit_cast<PhysicalPtr>(search_efi_configuration_table(EFI::ACPI_2_0_TABLE_GUID)) };
    if (boot_info->acpi_rsdp_paddr.is_null())
        boot_info->acpi_rsdp_paddr = PhysicalAddress { bit_cast<PhysicalPtr>(search_efi_configuration_table(EFI::ACPI_TABLE_GUID)) };
}

}
