/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
