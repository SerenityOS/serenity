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

#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/PC/BIOS.h>
#include <Kernel/DMI.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/MappedROM.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

#define SMBIOS_BASE_SEARCH_ADDR 0xf0000
#define SMBIOS_END_SEARCH_ADDR 0xfffff
#define SMBIOS_SEARCH_AREA_SIZE (SMBIOS_END_SEARCH_ADDR - SMBIOS_BASE_SEARCH_ADDR)

AK::Singleton<DMIExpose> s_the;

void DMIExpose::set_64_bit_entry_initialization_values()
{
    klog() << "DMIExpose: SMBIOS 64bit Entry point @ " << m_entry_point;
    auto smbios_entry = map_typed<SMBIOS::EntryPoint64bit>(PhysicalAddress(m_entry_point), SMBIOS_SEARCH_AREA_SIZE);
    m_structure_table = PhysicalAddress(smbios_entry.ptr()->table_ptr);
    m_entry_point_length = smbios_entry.ptr()->length;
    m_structure_table_length = smbios_entry.ptr()->table_maximum_size;
}

void DMIExpose::set_32_bit_entry_initialization_values()
{
    klog() << "DMIExpose: SMBIOS 32bit Entry point @ " << m_entry_point;
    auto smbios_entry = map_typed<SMBIOS::EntryPoint32bit>(PhysicalAddress(m_entry_point), SMBIOS_SEARCH_AREA_SIZE);
    m_structure_table = PhysicalAddress(smbios_entry.ptr()->legacy_structure.smbios_table_ptr);
    m_entry_point_length = smbios_entry.ptr()->length;
    m_structure_table_length = smbios_entry.ptr()->legacy_structure.smboios_table_length;
}

void DMIExpose::initialize()
{
    s_the.ensure_instance();
}

DMIExpose& DMIExpose::the()
{
    return *s_the;
}

size_t DMIExpose::entry_point_length() const
{
    return m_entry_point_length;
}
size_t DMIExpose::structure_table_length() const
{
    return m_structure_table_length;
}

void DMIExpose::initialize_exposer()
{
    ASSERT(!(m_entry_point.is_null()));
    if (m_using_64bit_entry_point) {
        set_64_bit_entry_initialization_values();
    } else {
        set_32_bit_entry_initialization_values();
    }
    klog() << "DMIExpose: Data table @ " << m_structure_table;
}

OwnPtr<KBuffer> DMIExpose::entry_point() const
{
    auto dmi_blob = map_typed<u8>((m_entry_point), m_entry_point_length);
    return KBuffer::try_create_with_bytes(Span<u8> { dmi_blob.ptr(), m_entry_point_length });
}
OwnPtr<KBuffer> DMIExpose::structure_table() const
{
    auto dmi_blob = map_typed<u8>(m_structure_table, m_structure_table_length);
    return KBuffer::try_create_with_bytes(Span<u8> { dmi_blob.ptr(), m_structure_table_length });
}

DMIExpose::DMIExpose()
{
    auto entry_32bit = find_entry32bit_point();
    m_entry_point = entry_32bit.value();

    auto entry_64bit = find_entry64bit_point();
    if (entry_64bit.has_value()) {
        m_entry_point = entry_64bit.value();
        m_using_64bit_entry_point = true;
    }
    if (m_entry_point.is_null())
        return;
    m_available = true;
    initialize_exposer();
}

Optional<PhysicalAddress> DMIExpose::find_entry64bit_point()
{
    return map_bios().find_chunk_starting_with("_SM3_", 16);
}

Optional<PhysicalAddress> DMIExpose::find_entry32bit_point()
{
    return map_bios().find_chunk_starting_with("_SM_", 16);
}

}
