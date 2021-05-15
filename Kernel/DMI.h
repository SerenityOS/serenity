/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/KBuffer.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel::SMBIOS {

struct [[gnu::packed]] LegacyEntryPoint32bit {
    char legacy_sig[5];
    u8 checksum2;
    u16 smboios_table_length;
    u32 smbios_table_ptr;
    u16 smbios_tables_count;
    u8 smbios_bcd_revision;
};

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

namespace Kernel {

class DMIExpose {

public:
    static void initialize();

    DMIExpose();

    static DMIExpose& the();

    bool is_available() const { return m_available; }
    OwnPtr<KBuffer> entry_point() const;
    OwnPtr<KBuffer> structure_table() const;
    size_t entry_point_length() const;
    size_t structure_table_length() const;

private:
    void set_64_bit_entry_initialization_values();
    void set_32_bit_entry_initialization_values();
    void initialize_exposer();

    Optional<PhysicalAddress> find_entry64bit_point();
    Optional<PhysicalAddress> find_entry32bit_point();

    PhysicalAddress m_entry_point;
    PhysicalAddress m_structure_table;
    bool m_using_64bit_entry_point { false };
    bool m_available { false };
    size_t m_structure_table_length { 0 };
    size_t m_entry_point_length { 0 };
};

}
