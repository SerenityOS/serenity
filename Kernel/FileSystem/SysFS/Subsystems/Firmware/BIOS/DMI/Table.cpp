/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/BIOS/DMI/Table.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

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

}
