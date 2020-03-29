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

#include <AK/StringView.h>
#include <Kernel/ACPI/DMIDecoder.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/StdLib.h>

namespace Kernel {

static DMIDecoder* s_dmi_decoder;

//#define SMBIOS_DEBUG

#define SMBIOS_BASE_SEARCH_ADDR 0xf0000
#define SMBIOS_END_SEARCH_ADDR 0xfffff
#define SMBIOS_SEARCH_AREA_SIZE (SMBIOS_END_SEARCH_ADDR - SMBIOS_BASE_SEARCH_ADDR)

DMIDecoder& DMIDecoder::the()
{
    if (s_dmi_decoder == nullptr) {
        s_dmi_decoder = new DMIDecoder(true);
    }
    return *s_dmi_decoder;
}

void DMIDecoder::initialize()
{
    if (s_dmi_decoder == nullptr) {
        s_dmi_decoder = new DMIDecoder(true);
    }
}

void DMIDecoder::initialize_untrusted()
{
    if (s_dmi_decoder == nullptr) {
        s_dmi_decoder = new DMIDecoder(false);
    }
}

void DMIDecoder::set_64_bit_entry_initialization_values(PhysicalAddress entry)
{
    klog() << "DMIDecoder: SMBIOS 64bit Entry point @ " << m_entry64bit_point;
    m_use_64bit_entry = true;

    auto region = MM.allocate_kernel_region(entry.page_base(), PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder 64 bit Initialization", Region::Access::Read, false, false);
    auto& entry_ptr = *(SMBIOS::EntryPoint64bit*)region->vaddr().offset(entry.offset_in_page()).as_ptr();
    m_structure_table = PhysicalAddress(entry_ptr.table_ptr);
    m_structures_count = entry_ptr.table_maximum_size;
    m_table_length = entry_ptr.table_maximum_size;
}

void DMIDecoder::set_32_bit_entry_initialization_values(PhysicalAddress entry)
{
    klog() << "DMIDecoder: SMBIOS 32bit Entry point @ " << m_entry32bit_point;
    m_use_64bit_entry = false;

    auto region = MM.allocate_kernel_region(entry.page_base(), PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder 32 bit Initialization", Region::Access::Read, false, false);
    auto& entry_ptr = *(SMBIOS::EntryPoint32bit*)region->vaddr().offset(entry.offset_in_page()).as_ptr();

    m_structure_table = PhysicalAddress(entry_ptr.legacy_structure.smbios_table_ptr);
    m_structures_count = entry_ptr.legacy_structure.smbios_tables_count;
    m_table_length = entry_ptr.legacy_structure.smbios_table_length;
}

void DMIDecoder::initialize_parser()
{

    if (m_entry32bit_point.is_null() && m_entry64bit_point.is_null()) {
        m_operable = false;
        klog() << "DMI Decoder is disabled. Cannot find SMBIOS tables.";
        return;
    }

    m_operable = true;
    klog() << "DMI Decoder is enabled";
    if (!m_entry64bit_point.is_null()) {
        set_64_bit_entry_initialization_values(m_entry64bit_point);
    } else if (!m_entry32bit_point.is_null()) {
        set_32_bit_entry_initialization_values(m_entry32bit_point);
    }
    klog() << "DMIDecoder: Data table @ " << m_structure_table;
}

void DMIDecoder::generate_data_raw_blob(KBufferBuilder& builder) const
{
    auto region = MM.allocate_kernel_region(m_structure_table.page_base(), PAGE_ROUND_UP(m_table_length), "DMI Decoder Enumerating SMBIOS", Region::Access::Read, false, false);
    auto* v_table_ptr = (volatile u8*)region->vaddr().offset(m_structure_table.offset_in_page()).as_ptr();
    for (size_t index = 0; index < m_table_length; index++)
        builder.append(v_table_ptr[index]);
}

void DMIDecoder::generate_entry_raw_blob(KBufferBuilder& builder) const
{
    auto region = MM.allocate_kernel_region(m_entry32bit_point.page_base(), PAGE_ROUND_UP(sizeof(SMBIOS::EntryPoint32bit)), "DMI Decoder Enumerating SMBIOS", Region::Access::Read, false, false);
    auto* v_table_ptr = (volatile u8*)region->vaddr().offset(m_entry32bit_point.offset_in_page()).as_ptr();
    for (size_t index = 0; index < sizeof(SMBIOS::EntryPoint32bit); index++)
        builder.append(v_table_ptr[index]);
}

DMIDecoder::DMIDecoder(bool trusted)
    : m_entry32bit_point(find_entry32bit_point())
    , m_entry64bit_point(find_entry64bit_point())
    , m_structure_table(PhysicalAddress())
    , m_untrusted(!trusted)
{
    if (!trusted) {
        klog() << "DMI Decoder initialized as untrusted due to user request.";
    }
    initialize_parser();
}

PhysicalAddress DMIDecoder::find_entry64bit_point()
{
    PhysicalAddress paddr = PhysicalAddress(SMBIOS_BASE_SEARCH_ADDR);
    auto region = MM.allocate_kernel_region(paddr, PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder Entry Point 64 bit Finding", Region::Access::Read, false, false);

    char* tested_physical_ptr = (char*)paddr.get();
    for (char* entry_str = (char*)(region->vaddr().get()); entry_str < (char*)(region->vaddr().get() + (SMBIOS_SEARCH_AREA_SIZE)); entry_str += 16) {
#ifdef SMBIOS_DEBUG
        dbg() << "DMI Decoder: Looking for 64 bit Entry point @ V " << (void*)entry_str << " P " << (void*)tested_physical_ptr;
#endif
        if (!strncmp("_SM3_", entry_str, strlen("_SM3_")))
            return PhysicalAddress((FlatPtr)tested_physical_ptr);

        tested_physical_ptr += 16;
    }
    return {};
}

PhysicalAddress DMIDecoder::find_entry32bit_point()
{
    PhysicalAddress paddr = PhysicalAddress(SMBIOS_BASE_SEARCH_ADDR);
    auto region = MM.allocate_kernel_region(paddr, PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder Entry Point 32 bit Finding", Region::Access::Read, false, false);

    char* tested_physical_ptr = (char*)paddr.get();
    for (char* entry_str = (char*)(region->vaddr().get()); entry_str < (char*)(region->vaddr().get() + (SMBIOS_SEARCH_AREA_SIZE)); entry_str += 16) {
#ifdef SMBIOS_DEBUG
        dbg() << "DMI Decoder: Looking for 32 bit Entry point @ V " << (void*)entry_str << " P " << (void*)tested_physical_ptr;
#endif
        if (!strncmp("_SM_", entry_str, strlen("_SM_")))
            return PhysicalAddress((FlatPtr)tested_physical_ptr);

        tested_physical_ptr += 16;
    }
    return {};
}

}
