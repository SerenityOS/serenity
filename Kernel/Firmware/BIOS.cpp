/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Firmware/BIOS.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define SMBIOS_BASE_SEARCH_ADDR 0xf0000
#define SMBIOS_END_SEARCH_ADDR 0xfffff
#define SMBIOS_SEARCH_AREA_SIZE (SMBIOS_END_SEARCH_ADDR - SMBIOS_BASE_SEARCH_ADDR)

UNMAP_AFTER_INIT NonnullRefPtr<DMIEntryPointExposedBlob> DMIEntryPointExposedBlob::must_create(PhysicalAddress dmi_entry_point, size_t blob_size)
{
    return adopt_ref(*new (nothrow) DMIEntryPointExposedBlob(dmi_entry_point, blob_size));
}

UNMAP_AFTER_INIT BIOSSysFSComponent::BIOSSysFSComponent()
{
}

ErrorOr<size_t> BIOSSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

UNMAP_AFTER_INIT DMIEntryPointExposedBlob::DMIEntryPointExposedBlob(PhysicalAddress dmi_entry_point, size_t blob_size)
    : BIOSSysFSComponent()
    , m_dmi_entry_point(dmi_entry_point)
    , m_dmi_entry_point_length(blob_size)
{
}

ErrorOr<NonnullOwnPtr<KBuffer>> DMIEntryPointExposedBlob::try_to_generate_buffer() const
{
    auto dmi_blob = TRY(Memory::map_typed<u8>((m_dmi_entry_point), m_dmi_entry_point_length));
    return KBuffer::try_create_with_bytes(Span<u8> { dmi_blob.ptr(), m_dmi_entry_point_length });
}

UNMAP_AFTER_INIT NonnullRefPtr<SMBIOSExposedTable> SMBIOSExposedTable::must_create(PhysicalAddress smbios_structure_table, size_t smbios_structure_table_length)
{
    return adopt_ref(*new (nothrow) SMBIOSExposedTable(smbios_structure_table, smbios_structure_table_length));
}

UNMAP_AFTER_INIT SMBIOSExposedTable::SMBIOSExposedTable(PhysicalAddress smbios_structure_table, size_t smbios_structure_table_length)
    : BIOSSysFSComponent()
    , m_smbios_structure_table(smbios_structure_table)
    , m_smbios_structure_table_length(smbios_structure_table_length)
{
}

ErrorOr<NonnullOwnPtr<KBuffer>> SMBIOSExposedTable::try_to_generate_buffer() const
{
    auto dmi_blob = TRY(Memory::map_typed<u8>((m_smbios_structure_table), m_smbios_structure_table_length));
    return KBuffer::try_create_with_bytes(Span<u8> { dmi_blob.ptr(), m_smbios_structure_table_length });
}

UNMAP_AFTER_INIT void BIOSSysFSDirectory::set_dmi_64_bit_entry_initialization_values()
{
    dbgln("BIOSSysFSDirectory: SMBIOS 64bit Entry point @ {}", m_dmi_entry_point);
    auto smbios_entry = Memory::map_typed<SMBIOS::EntryPoint64bit>(m_dmi_entry_point, SMBIOS_SEARCH_AREA_SIZE).release_value_but_fixme_should_propagate_errors();
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->table_ptr);
    m_dmi_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->table_maximum_size;
}

UNMAP_AFTER_INIT void BIOSSysFSDirectory::set_dmi_32_bit_entry_initialization_values()
{
    dbgln("BIOSSysFSDirectory: SMBIOS 32bit Entry point @ {}", m_dmi_entry_point);
    auto smbios_entry = Memory::map_typed<SMBIOS::EntryPoint32bit>(m_dmi_entry_point, SMBIOS_SEARCH_AREA_SIZE).release_value_but_fixme_should_propagate_errors();
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->legacy_structure.smbios_table_ptr);
    m_dmi_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->legacy_structure.smbios_table_length;
}

UNMAP_AFTER_INIT NonnullRefPtr<BIOSSysFSDirectory> BIOSSysFSDirectory::must_create(FirmwareSysFSDirectory& firmware_directory)
{
    auto bios_directory = MUST(adopt_nonnull_ref_or_enomem(new (nothrow) BIOSSysFSDirectory(firmware_directory)));
    bios_directory->create_components();
    return bios_directory;
}

void BIOSSysFSDirectory::create_components()
{
    if (m_dmi_entry_point.is_null() || m_smbios_structure_table.is_null())
        return;
    if (m_dmi_entry_point_length == 0) {
        dbgln("BIOSSysFSDirectory: invalid dmi entry length");
        return;
    }
    if (m_smbios_structure_table_length == 0) {
        dbgln("BIOSSysFSDirectory: invalid smbios structure table length");
        return;
    }
    m_components.append(DMIEntryPointExposedBlob::must_create(m_dmi_entry_point, m_dmi_entry_point_length));
    m_components.append(SMBIOSExposedTable::must_create(m_smbios_structure_table, m_smbios_structure_table_length));
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

UNMAP_AFTER_INIT BIOSSysFSDirectory::BIOSSysFSDirectory(FirmwareSysFSDirectory& firmware_directory)
    : SysFSDirectory(firmware_directory)
{
    auto entry_32bit = find_dmi_entry32bit_point();
    if (entry_32bit.has_value()) {
        m_dmi_entry_point = entry_32bit.value();
    }

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
    auto bios_or_error = map_bios();
    if (bios_or_error.is_error())
        return {};
    return bios_or_error.value().find_chunk_starting_with("_SM3_", 16);
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> BIOSSysFSDirectory::find_dmi_entry32bit_point()
{
    auto bios_or_error = map_bios();
    if (bios_or_error.is_error())
        return {};
    return bios_or_error.value().find_chunk_starting_with("_SM_", 16);
}

ErrorOr<Memory::MappedROM> map_bios()
{
    Memory::MappedROM mapping;
    mapping.size = 128 * KiB;
    mapping.paddr = PhysicalAddress(0xe0000);
    auto region_size = TRY(Memory::page_round_up(mapping.size));
    mapping.region = TRY(MM.allocate_kernel_region(mapping.paddr, region_size, {}, Memory::Region::Access::Read));
    return mapping;
}

ErrorOr<Memory::MappedROM> map_ebda()
{
    auto ebda_segment_ptr = TRY(Memory::map_typed<u16>(PhysicalAddress(0x40e)));
    PhysicalAddress ebda_paddr(PhysicalAddress(*ebda_segment_ptr).get() << 4);
    // The EBDA size is stored in the first byte of the EBDA in 1K units
    size_t ebda_size = *TRY(Memory::map_typed<u8>(ebda_paddr));
    ebda_size *= 1024;

    Memory::MappedROM mapping;
    auto region_size = TRY(Memory::page_round_up(ebda_size));
    mapping.region = TRY(MM.allocate_kernel_region(ebda_paddr.page_base(), region_size, {}, Memory::Region::Access::Read));
    mapping.offset = ebda_paddr.offset_in_page();
    mapping.size = ebda_size;
    mapping.paddr = ebda_paddr;
    return mapping;
}

}
