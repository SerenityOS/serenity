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
