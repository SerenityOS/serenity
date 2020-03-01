/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    klog() << "DMIDecoder: SMBIOS 64bit Entry point @ P " << String::format("%p", m_entry64bit_point.get());
    m_use_64bit_entry = true;

    auto region = MM.allocate_kernel_region(entry.page_base(), PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder 64 bit Initialization", Region::Access::Read, false, false);
    auto& entry_ptr = *(SMBIOS::EntryPoint64bit*)region->vaddr().offset(entry.offset_in_page().get()).as_ptr();
    m_structure_table = PhysicalAddress(entry_ptr.table_ptr);
    m_structures_count = entry_ptr.table_maximum_size;
    m_table_length = entry_ptr.table_maximum_size;
}

void DMIDecoder::set_32_bit_entry_initialization_values(PhysicalAddress entry)
{
    klog() << "DMIDecoder: SMBIOS 32bit Entry point @ P " << String::format("%p", m_entry32bit_point.get());
    m_use_64bit_entry = false;

    auto region = MM.allocate_kernel_region(entry.page_base(), PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder 32 bit Initialization", Region::Access::Read, false, false);
    auto& entry_ptr = *(SMBIOS::EntryPoint32bit*)region->vaddr().offset(entry.offset_in_page().get()).as_ptr();

    m_structure_table = PhysicalAddress(entry_ptr.legacy_structure.smbios_table_ptr);
    m_structures_count = entry_ptr.legacy_structure.smbios_tables_count;
    m_table_length = entry_ptr.legacy_structure.smboios_table_length;
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
    klog() << "DMIDecoder: Data table @ P " << String::format("%p", m_structure_table.get());
    enumerate_smbios_tables();
}

void DMIDecoder::enumerate_smbios_tables()
{

    u32 table_length = m_table_length;
    auto p_table = m_structure_table;

    auto region = MM.allocate_kernel_region(p_table.page_base(), PAGE_ROUND_UP(table_length), "DMI Decoder Enumerating SMBIOS", Region::Access::Read, false, false);
    volatile SMBIOS::TableHeader* v_table_ptr = (SMBIOS::TableHeader*)region->vaddr().offset(p_table.offset_in_page().get()).as_ptr();

#ifdef SMBIOS_DEBUG
    dbg() << "DMIDecoder: Total Table length " << m_table_length;
#endif

    u32 structures_count = 0;
    while (table_length > 0) {
#ifdef SMBIOS_DEBUG
        dbg() << "DMIDecoder: Examining table @ P " << (void*)p_table.as_ptr() << " V " << const_cast<SMBIOS::TableHeader*>(v_table_ptr);
#endif
        structures_count++;
        if (v_table_ptr->type == (u8)SMBIOS::TableType::EndOfTable) {
            klog() << "DMIDecoder: Detected table with type 127, End of SMBIOS data.";
            break;
        }
        klog() << "DMIDecoder: Detected table with type " << v_table_ptr->type;
        m_smbios_tables.append(p_table);
        table_length -= v_table_ptr->length;

        size_t table_size = get_table_size(p_table);
        p_table = p_table.offset(table_size);
        v_table_ptr = (SMBIOS::TableHeader*)((uintptr_t)v_table_ptr + table_size);
#ifdef SMBIOS_DEBUG
        dbg() << "DMIDecoder: Next table @ P 0x" << p_table.get();
#endif
    }
    m_structures_count = structures_count;
}

size_t DMIDecoder::get_table_size(PhysicalAddress table)
{
    auto region = MM.allocate_kernel_region(table.page_base(), PAGE_ROUND_UP(m_table_length), "DMI Decoder Determining table size", Region::Access::Read, false, false);
    auto& table_v_ptr = (SMBIOS::TableHeader&)*region->vaddr().offset(table.offset_in_page().get()).as_ptr();
#ifdef SMBIOS_DEBUG
    dbg() << "DMIDecoder: table legnth - " << table_v_ptr.length;
#endif
    const char* strtab = (char*)&table_v_ptr + table_v_ptr.length;
    size_t index = 1;
    while (strtab[index - 1] != '\0' || strtab[index] != '\0') {
        if (index > m_table_length) {
            ASSERT_NOT_REACHED(); // FIXME: Instead of halting, find a better solution (Hint: use m_operable to disallow further use of DMIDecoder)
        }
        index++;
    }
#ifdef SMBIOS_DEBUG
    dbg() << "DMIDecoder: table size - " << (table_v_ptr.length + index + 1);
#endif
    return table_v_ptr.length + index + 1;
}

PhysicalAddress DMIDecoder::get_next_physical_table(PhysicalAddress p_table)
{
    return p_table.offset(get_table_size(p_table));
}

PhysicalAddress DMIDecoder::get_smbios_physical_table_by_handle(u16 handle)
{

    for (auto table : m_smbios_tables) {
        if (table.is_null())
            continue;
        auto region = MM.allocate_kernel_region(table.page_base(), PAGE_SIZE * 2, "DMI Decoder Finding Table", Region::Access::Read, false, false);
        SMBIOS::TableHeader* table_v_ptr = (SMBIOS::TableHeader*)region->vaddr().offset(table.offset_in_page().get()).as_ptr();

        if (table_v_ptr->handle == handle) {
            return table;
        }
    }
    return {};
}
PhysicalAddress DMIDecoder::get_smbios_physical_table_by_type(u8 table_type)
{

    for (auto table : m_smbios_tables) {
        if (table.is_null())
            continue;
        auto region = MM.allocate_kernel_region(table.page_base(), PAGE_ROUND_UP(PAGE_SIZE * 2), "DMI Decoder Finding Table", Region::Access::Read, false, false);
        SMBIOS::TableHeader* table_v_ptr = (SMBIOS::TableHeader*)region->vaddr().offset(table.offset_in_page().get()).as_ptr();
        if (table_v_ptr->type == table_type) {
            return table;
        }
    }
    return {};
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
            return PhysicalAddress((uintptr_t)tested_physical_ptr);

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
            return PhysicalAddress((uintptr_t)tested_physical_ptr);

        tested_physical_ptr += 16;
    }
    return {};
}

Vector<SMBIOS::PhysicalMemoryArray*>& DMIDecoder::get_physical_memory_areas()
{
    // FIXME: Implement it...
    klog() << "DMIDecoder::get_physical_memory_areas() is not implemented.";
    ASSERT_NOT_REACHED();
}
bool DMIDecoder::is_reliable()
{
    return !m_untrusted;
}
u64 DMIDecoder::get_bios_characteristics()
{
    // FIXME: Make sure we have some mapping here so we don't rely on existing identity mapping...
    ASSERT_NOT_REACHED();
    ASSERT(m_operable == true);
    auto* bios_info = (SMBIOS::BIOSInfo*)get_smbios_physical_table_by_type(0).as_ptr();
    ASSERT(bios_info != nullptr);

    klog() << "DMIDecoder: BIOS info @ P " << String::format("%p", bios_info);
    return bios_info->bios_characteristics;
}

char* DMIDecoder::get_smbios_string(PhysicalAddress, u8)
{
    // FIXME: Implement it...
    // FIXME: Make sure we have some mapping here so we don't rely on existing identity mapping...
    klog() << "DMIDecoder::get_smbios_string() is not implemented.";
    ASSERT_NOT_REACHED();
    return nullptr;
}

}
