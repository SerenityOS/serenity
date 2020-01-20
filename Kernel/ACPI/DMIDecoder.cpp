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
#include <Kernel/StdLib.h>
#include <Kernel/VM/MemoryManager.h>

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

void DMIDecoder::initialize_parser()
{
    if (m_entry32bit_point != nullptr || m_entry64bit_point != nullptr) {
        m_operable = true;
        kprintf("DMI Decoder is enabled\n");
        if (m_entry64bit_point != nullptr) {
            kprintf("DMIDecoder: SMBIOS 64bit Entry point @ P 0x%x\n", m_entry64bit_point);
            m_use_64bit_entry = true;
            m_structure_table = (SMBIOS::TableHeader*)m_entry64bit_point->table_ptr;
            m_structures_count = m_entry64bit_point->table_maximum_size;
            m_table_length = m_entry64bit_point->table_maximum_size;
        } else if (m_entry32bit_point != nullptr) {
            kprintf("DMIDecoder: SMBIOS 32bit Entry point @ P 0x%x\n", m_entry32bit_point);
            m_use_64bit_entry = false;
            m_structure_table = (SMBIOS::TableHeader*)m_entry32bit_point->legacy_structure.smbios_table_ptr;
            m_structures_count = m_entry32bit_point->legacy_structure.smbios_tables_count;
            m_table_length = m_entry32bit_point->legacy_structure.smboios_table_length;
        }
        kprintf("DMIDecoder: Data table @ P 0x%x\n", m_structure_table);
        enumerate_smbios_tables();
    } else {
        m_operable = false;
        kprintf("DMI Decoder is disabled. Cannot find SMBIOS tables.\n");
    }
}

void DMIDecoder::enumerate_smbios_tables()
{

    u32 table_length = m_table_length;
    SMBIOS::TableHeader* p_table_ptr = m_structure_table;

    PhysicalAddress paddr = PhysicalAddress(page_base_of((uintptr_t)p_table_ptr));
    auto region = MM.allocate_kernel_region(paddr, PAGE_ROUND_UP(table_length), "DMI Decoder Enumerating SMBIOS", Region::Access::Read, false, false);

    volatile SMBIOS::TableHeader* v_table_ptr = (SMBIOS::TableHeader*)region->vaddr().offset(offset_in_page((uintptr_t)p_table_ptr)).as_ptr();
#ifdef SMBIOS_DEBUG
    dbgprintf("DMIDecoder: Total Table length %d\n", m_table_length);
#endif

    u32 structures_count = 0;
    while (table_length > 0) {
#ifdef SMBIOS_DEBUG
        dbgprintf("DMIDecoder: Examining table @ P 0x%x V 0x%x\n", p_table_ptr, v_table_ptr);
#endif
        structures_count++;
        if (v_table_ptr->type == (u8)SMBIOS::TableType::EndOfTable) {
            kprintf("DMIDecoder: Detected table with type 127, End of SMBIOS data.\n");
            break;
        }
        kprintf("DMIDecoder: Detected table with type %d\n", v_table_ptr->type);
        m_smbios_tables.append(p_table_ptr);
        table_length -= v_table_ptr->length;

        size_t table_size = get_table_size(*p_table_ptr);
        p_table_ptr = (SMBIOS::TableHeader*)((uintptr_t)p_table_ptr + table_size);
        v_table_ptr = (SMBIOS::TableHeader*)((uintptr_t)v_table_ptr + table_size);
#ifdef SMBIOS_DEBUG
        dbgprintf("DMIDecoder: Next table @ P 0x%x\n", p_table_ptr);
#endif
        if (p_table_ptr == nullptr)
            break;
    }
    m_structures_count = structures_count;
}

size_t DMIDecoder::get_table_size(SMBIOS::TableHeader& table)
{
    // FIXME: Make sure we have some mapping here so we don't rely on existing identity mapping...
#ifdef SMBIOS_DEBUG
    dbgprintf("DMIDecoder: table legnth - 0x%x\n", table.length);
#endif
    const char* strtab = (char*)&table + table.length;
    size_t index = 1;
    while (strtab[index - 1] != '\0' || strtab[index] != '\0') {
        if (index > m_table_length) {
            ASSERT_NOT_REACHED(); // FIXME: Instead of halting, find a better solution (Hint: use m_operable to disallow further use of DMIDecoder)
        }
        index++;
    }
#ifdef SMBIOS_DEBUG
    dbgprintf("DMIDecoder: table size - 0x%x\n", table.length + index + 1);
#endif
    return table.length + index + 1;
}

SMBIOS::TableHeader* DMIDecoder::get_next_physical_table(SMBIOS::TableHeader& p_table)
{
    return (SMBIOS::TableHeader*)((uintptr_t)&p_table + get_table_size(p_table));
}

SMBIOS::TableHeader* DMIDecoder::get_smbios_physical_table_by_handle(u16 handle)
{

    for (auto* table : m_smbios_tables) {
        if (!table)
            continue;
        auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((uintptr_t)table)), PAGE_SIZE * 2, "DMI Decoder Finding Table", Region::Access::Read, false, false);
        SMBIOS::TableHeader* table_v_ptr = (SMBIOS::TableHeader*)region->vaddr().offset(offset_in_page((uintptr_t)table)).as_ptr();

        if (table_v_ptr->handle == handle) {
            return table;
        }
    }
    return nullptr;
}
SMBIOS::TableHeader* DMIDecoder::get_smbios_physical_table_by_type(u8 table_type)
{

    for (auto* table : m_smbios_tables) {
        if (!table)
            continue;
        auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((uintptr_t)table)), PAGE_ROUND_UP(PAGE_SIZE * 2), "DMI Decoder Finding Table", Region::Access::Read, false, false);
        SMBIOS::TableHeader* table_v_ptr = (SMBIOS::TableHeader*)region->vaddr().offset(offset_in_page((uintptr_t)table)).as_ptr();
        if (table_v_ptr->type == table_type) {
            return table;
        }
    }
    return nullptr;
}

DMIDecoder::DMIDecoder(bool trusted)
    : m_entry32bit_point(find_entry32bit_point())
    , m_entry64bit_point(find_entry64bit_point())
    , m_structure_table(nullptr)
    , m_untrusted(!trusted)
{
    if (!trusted) {
        kprintf("DMI Decoder initialized as untrusted due to user request.\n");
    }
    initialize_parser();
}

SMBIOS::EntryPoint64bit* DMIDecoder::find_entry64bit_point()
{
    PhysicalAddress paddr = PhysicalAddress(SMBIOS_BASE_SEARCH_ADDR);
    auto region = MM.allocate_kernel_region(paddr, PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder Entry Point 64 bit Finding", Region::Access::Read, false, false);

    char* tested_physical_ptr = (char*)paddr.get();
    for (char* entry_str = (char*)(region->vaddr().get()); entry_str < (char*)(region->vaddr().get() + (SMBIOS_SEARCH_AREA_SIZE)); entry_str += 16) {
#ifdef SMBIOS_DEBUG
        dbgprintf("DMI Decoder: Looking for 64 bit Entry point @ V 0x%x P 0x%x\n", entry_str, tested_physical_ptr);
#endif
        if (!strncmp("_SM3_", entry_str, strlen("_SM3_")))
            return (SMBIOS::EntryPoint64bit*)tested_physical_ptr;

        tested_physical_ptr += 16;
    }
    return nullptr;
}

SMBIOS::EntryPoint32bit* DMIDecoder::find_entry32bit_point()
{
    PhysicalAddress paddr = PhysicalAddress(SMBIOS_BASE_SEARCH_ADDR);
    auto region = MM.allocate_kernel_region(paddr, PAGE_ROUND_UP(SMBIOS_SEARCH_AREA_SIZE), "DMI Decoder Entry Point 32 bit Finding", Region::Access::Read, false, false);

    char* tested_physical_ptr = (char*)paddr.get();
    for (char* entry_str = (char*)(region->vaddr().get()); entry_str < (char*)(region->vaddr().get() + (SMBIOS_SEARCH_AREA_SIZE)); entry_str += 16) {
#ifdef SMBIOS_DEBUG
        dbgprintf("DMI Decoder: Looking for 32 bit Entry point @ V 0x%x P 0x%x\n", entry_str, tested_physical_ptr);
#endif
        if (!strncmp("_SM_", entry_str, strlen("_SM_")))
            return (SMBIOS::EntryPoint32bit*)tested_physical_ptr;

        tested_physical_ptr += 16;
    }
    return nullptr;
}

Vector<SMBIOS::PhysicalMemoryArray*>& DMIDecoder::get_physical_memory_areas()
{
    // FIXME: Implement it...
    kprintf("DMIDecoder::get_physical_memory_areas() is not implemented.\n");
    ASSERT_NOT_REACHED();
}
bool DMIDecoder::is_reliable()
{
    return !m_untrusted;
}
u64 DMIDecoder::get_bios_characteristics()
{
    // FIXME: Make sure we have some mapping here so we don't rely on existing identity mapping...
    ASSERT(m_operable == true);
    SMBIOS::BIOSInfo* bios_info = (SMBIOS::BIOSInfo*)get_smbios_physical_table_by_type(0);
    ASSERT(bios_info != nullptr);
    kprintf("DMIDecoder: BIOS info @ P 0x%x\n", bios_info);
    return bios_info->bios_characteristics;
}

char* DMIDecoder::get_smbios_string(SMBIOS::TableHeader&, u8)
{
    // FIXME: Implement it...
    // FIXME: Make sure we have some mapping here so we don't rely on existing identity mapping...
    kprintf("DMIDecoder::get_smbios_string() is not implemented.\n");
    ASSERT_NOT_REACHED();
    return nullptr;
}
