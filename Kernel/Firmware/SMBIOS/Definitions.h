/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::SMBIOS {

// SMBIOS Specification Version 3.9.0

// Table 1 – SMBIOS 2.1 (32-bit) Entry Point structure
struct [[gnu::packed]] LegacyEntryPoint32bit {
    char legacy_sig[5];
    u8 checksum2;
    u16 smbios_table_length;
    u32 smbios_table_ptr;
    u16 smbios_tables_count;
    u8 smbios_bcd_revision;
};

// Table 1 – SMBIOS 2.1 (32-bit) Entry Point structure
struct [[gnu::packed]] EntryPoint32bit {
    char sig[4];
    u8 checksum;
    u8 length;
    u8 major_version;
    u8 minor_version;
    u16 maximum_structure_size;
    u8 implementation_revision;
    char formatted_area[5];
    LegacyEntryPoint32bit legacy_structure;
};

// Table 2 – SMBIOS 3.0 (64-bit) Entry Point structure
struct [[gnu::packed]] EntryPoint64bit {
    char sig[5];
    u8 checksum;
    u8 length;
    u8 major_version;
    u8 minor_version;
    u8 document_revision;
    u8 revision;
    u8 reserved;
    u32 table_maximum_size;
    u64 table_ptr;
};
}
