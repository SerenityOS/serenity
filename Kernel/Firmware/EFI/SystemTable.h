/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/Protocols/ConsoleSupport.h>
#include <Kernel/Firmware/EFI/Services/BootServices.h>

// https://uefi.org/specs/UEFI/2.10/04_EFI_System_Table.html

namespace Kernel::EFI {

// EFI_CONFIGURATION_TABLE: https://uefi.org/specs/UEFI/2.10/04_EFI_System_Table.html#efi-configuration-table-properties-table
struct ConfigurationTable {
    GUID vendor_guid;
    void* vendor_table;
};
static_assert(AssertSize<ConfigurationTable, 24>());

// EFI_DTB_TABLE_GUID: https://uefi.org/specs/UEFI/2.10/04_EFI_System_Table.html#devicetree-tables
static constexpr GUID DTB_TABLE_GUID = { 0xb1b621d5, 0xf19c, 0x41a5, { 0x83, 0x0b, 0xd9, 0x15, 0x2c, 0x69, 0xaa, 0xe0 } };

// https://uefi.org/specs/UEFI/2.10/04_EFI_System_Table.html#industry-standard-configuration-tables
static constexpr GUID ACPI_2_0_TABLE_GUID = { 0x8868e871, 0xe4f1, 0x11d3, { 0xbc, 0x22, 0x00, 0x80, 0xc7, 0x3c, 0x88, 0x81 } };
static constexpr GUID ACPI_TABLE_GUID = { 0xeb9d2d30, 0x2d88, 0x11d3, { 0x9a, 0x16, 0x00, 0x90, 0x27, 0x3f, 0xc1, 0x4d } };
static constexpr GUID SMBIOS3_TABLE_GUID = { 0xf2fd1544, 0x9794, 0x4a2c, { 0x99, 0x2e, 0xe5, 0xbb, 0xcf, 0x20, 0xe3, 0x94 } };
static constexpr GUID SMBIOS_TABLE_GUID = { 0xeb9d2d31, 0x2d88, 0x11d3, { 0x9a, 0x16, 0x00, 0x90, 0x27, 0x3f, 0xc1, 0x4d } };

// EFI_SYSTEM_TABLE: https://uefi.org/specs/UEFI/2.10/04_EFI_System_Table.html#efi-system-table-1
struct SystemTable {
    static constexpr u64 signature = 0x5453595320494249;

    using RuntimeServices = void;

    TableHeader hdr;
    char16_t* firmware_vendor;
    u32 firmware_revision;
    Handle console_in_handle;
    SimpleTextInputProtocol* con_in;
    Handle console_out_handle;
    SimpleTextOutputProtocol* con_out;
    Handle standard_error_handle;
    SimpleTextOutputProtocol* std_err;
    RuntimeServices* runtime_services;
    BootServices* boot_services;
    FlatPtr number_of_table_entries;
    ConfigurationTable* configuration_table;
};
static_assert(AssertSize<SystemTable, 120>());

}
