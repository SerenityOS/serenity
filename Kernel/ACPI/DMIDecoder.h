/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/FixedArray.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/VM/Region.h>
#include <LibBareMetal/Memory/PhysicalAddress.h>
#include <LibBareMetal/Memory/VirtualAddress.h>
#include <LibHardware/SMBIOS/Definitions.h>

namespace Kernel {

class DMIDecoder {
public:
    static DMIDecoder& the();
    static void initialize();
    static void initialize_untrusted();
    Vector<SMBIOS::PhysicalMemoryArray*>& get_physical_memory_areas();
    bool is_reliable();
    u64 get_bios_characteristics();

private:
    void enumerate_smbios_tables();
    PhysicalAddress get_next_physical_table(PhysicalAddress p_table);
    PhysicalAddress get_smbios_physical_table_by_handle(u16 handle);
    PhysicalAddress get_smbios_physical_table_by_type(u8 table_type);
    char* get_smbios_string(PhysicalAddress, u8 string_number);
    size_t get_table_size(PhysicalAddress);

    explicit DMIDecoder(bool trusted);
    void initialize_parser();

    void set_64_bit_entry_initialization_values(PhysicalAddress);
    void set_32_bit_entry_initialization_values(PhysicalAddress);

    PhysicalAddress find_entry32bit_point();
    PhysicalAddress find_entry64bit_point();

    PhysicalAddress m_entry32bit_point;
    PhysicalAddress m_entry64bit_point;
    PhysicalAddress m_structure_table;
    u32 m_structures_count;
    u32 m_table_length;
    bool m_use_64bit_entry;
    bool m_operable;
    bool m_untrusted;

    SinglyLinkedList<PhysicalAddress> m_smbios_tables;
};

}
