/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Arch/PC/BIOS.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Sections.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

#define SMBIOS_BASE_SEARCH_ADDR 0xf0000
#define SMBIOS_END_SEARCH_ADDR 0xfffff
#define SMBIOS_SEARCH_AREA_SIZE (SMBIOS_END_SEARCH_ADDR - SMBIOS_BASE_SEARCH_ADDR)

UNMAP_AFTER_INIT NonnullRefPtr<DMIEntryPointExposedBlob> DMIEntryPointExposedBlob::create(PhysicalAddress dmi_entry_point, size_t blob_size)
{
    return adopt_ref(*new (nothrow) DMIEntryPointExposedBlob(dmi_entry_point, blob_size));
}

UNMAP_AFTER_INIT BIOSSysFSComponent::BIOSSysFSComponent(String name)
    : SysFSComponent(name)
{
}

KResultOr<size_t> BIOSSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription*) const
{
    auto blob = try_to_generate_buffer();
    if (!blob)
        return KResult(EFAULT);

    if ((size_t)offset >= blob->size())
        return KSuccess;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(blob->data() + offset, nread))
        return KResult(EFAULT);
    return nread;
}

UNMAP_AFTER_INIT DMIEntryPointExposedBlob::DMIEntryPointExposedBlob(PhysicalAddress dmi_entry_point, size_t blob_size)
    : BIOSSysFSComponent("smbios_entry_point")
    , m_dmi_entry_point(dmi_entry_point)
    , m_dmi_entry_point_length(blob_size)
{
}

OwnPtr<KBuffer> DMIEntryPointExposedBlob::try_to_generate_buffer() const
{
    auto dmi_blob = map_typed<u8>((m_dmi_entry_point), m_dmi_entry_point_length);
    return KBuffer::try_create_with_bytes(Span<u8> { dmi_blob.ptr(), m_dmi_entry_point_length });
}

UNMAP_AFTER_INIT NonnullRefPtr<SMBIOSExposedTable> SMBIOSExposedTable::create(PhysicalAddress smbios_structure_table, size_t smbios_structure_table_length)
{
    return adopt_ref(*new (nothrow) SMBIOSExposedTable(smbios_structure_table, smbios_structure_table_length));
}

UNMAP_AFTER_INIT SMBIOSExposedTable::SMBIOSExposedTable(PhysicalAddress smbios_structure_table, size_t smbios_structure_table_length)
    : BIOSSysFSComponent("DMI")
    , m_smbios_structure_table(smbios_structure_table)
    , m_smbios_structure_table_length(smbios_structure_table_length)
{
}

OwnPtr<KBuffer> SMBIOSExposedTable::try_to_generate_buffer() const
{
    auto dmi_blob = map_typed<u8>((m_smbios_structure_table), m_smbios_structure_table_length);
    return KBuffer::try_create_with_bytes(Span<u8> { dmi_blob.ptr(), m_smbios_structure_table_length });
}

UNMAP_AFTER_INIT void BIOSSysFSDirectory::set_dmi_64_bit_entry_initialization_values()
{
    dbgln("BIOSSysFSDirectory: SMBIOS 64bit Entry point @ {}", m_dmi_entry_point);
    auto smbios_entry = map_typed<SMBIOS::EntryPoint64bit>(m_dmi_entry_point, SMBIOS_SEARCH_AREA_SIZE);
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->table_ptr);
    m_dmi_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->table_maximum_size;
}

UNMAP_AFTER_INIT void BIOSSysFSDirectory::set_dmi_32_bit_entry_initialization_values()
{
    dbgln("BIOSSysFSDirectory: SMBIOS 32bit Entry point @ {}", m_dmi_entry_point);
    auto smbios_entry = map_typed<SMBIOS::EntryPoint32bit>(m_dmi_entry_point, SMBIOS_SEARCH_AREA_SIZE);
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->legacy_structure.smbios_table_ptr);
    m_dmi_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->legacy_structure.smboios_table_length;
}

UNMAP_AFTER_INIT void BIOSSysFSDirectory::initialize()
{
    auto bios_folder = adopt_ref(*new (nothrow) BIOSSysFSDirectory());
    SysFSComponentRegistry::the().register_new_component(bios_folder);
    bios_folder->create_components();
}

void BIOSSysFSDirectory::create_components()
{
    auto dmi_entry_point = DMIEntryPointExposedBlob::create(m_dmi_entry_point, m_dmi_entry_point_length);
    m_components.append(dmi_entry_point);
    auto smbios_table = SMBIOSExposedTable::create(m_smbios_structure_table, m_smbios_structure_table_length);
    m_components.append(smbios_table);
}

size_t BIOSSysFSDirectory::dmi_entry_point_length() const
{
    return m_dmi_entry_point_length;
}
size_t BIOSSysFSDirectory::smbios_structure_table_length() const
{
    return m_smbios_structure_table_length;
}

UNMAP_AFTER_INIT void BIOSSysFSDirectory::initialize_dmi_exposer()
{
    VERIFY(!(m_dmi_entry_point.is_null()));
    if (m_using_64bit_dmi_entry_point) {
        set_dmi_64_bit_entry_initialization_values();
    } else {
        set_dmi_32_bit_entry_initialization_values();
    }
    dbgln("BIOSSysFSDirectory: Data table @ {}", m_smbios_structure_table);
}

OwnPtr<KBuffer> BIOSSysFSDirectory::smbios_structure_table() const
{
    auto dmi_blob = map_typed<u8>(m_smbios_structure_table, m_smbios_structure_table_length);
    return KBuffer::try_create_with_bytes(Span<u8> { dmi_blob.ptr(), m_smbios_structure_table_length });
}

UNMAP_AFTER_INIT BIOSSysFSDirectory::BIOSSysFSDirectory()
    : SysFSDirectory("bios", SysFSComponentRegistry::the().root_folder())
{
    auto entry_32bit = find_dmi_entry32bit_point();
    m_dmi_entry_point = entry_32bit.value();

    auto entry_64bit = find_dmi_entry64bit_point();
    if (entry_64bit.has_value()) {
        m_dmi_entry_point = entry_64bit.value();
        m_using_64bit_dmi_entry_point = true;
    }
    if (m_dmi_entry_point.is_null())
        return;
    initialize_dmi_exposer();
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> BIOSSysFSDirectory::find_dmi_entry64bit_point()
{
    return map_bios().find_chunk_starting_with("_SM3_", 16);
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> BIOSSysFSDirectory::find_dmi_entry32bit_point()
{
    return map_bios().find_chunk_starting_with("_SM_", 16);
}

MappedROM map_bios()
{
    MappedROM mapping;
    mapping.size = 128 * KiB;
    mapping.paddr = PhysicalAddress(0xe0000);
    mapping.region = MM.allocate_kernel_region(mapping.paddr, page_round_up(mapping.size), {}, Region::Access::Read);
    return mapping;
}

MappedROM map_ebda()
{
    auto ebda_segment_ptr = map_typed<u16>(PhysicalAddress(0x40e));
    auto ebda_length_ptr_b0 = map_typed<u8>(PhysicalAddress(0x413));
    auto ebda_length_ptr_b1 = map_typed<u8>(PhysicalAddress(0x414));

    PhysicalAddress ebda_paddr(*ebda_segment_ptr << 4);
    size_t ebda_size = (*ebda_length_ptr_b1 << 8) | *ebda_length_ptr_b0;

    MappedROM mapping;
    mapping.region = MM.allocate_kernel_region(ebda_paddr.page_base(), page_round_up(ebda_size), {}, Region::Access::Read);
    mapping.offset = ebda_paddr.offset_in_page();
    mapping.size = ebda_size;
    mapping.paddr = ebda_paddr;
    return mapping;
}

}
