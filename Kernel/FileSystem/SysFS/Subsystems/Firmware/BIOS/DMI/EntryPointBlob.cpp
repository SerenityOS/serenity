/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/BIOS/DMI/EntryPointBlob.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<DMIEntryPointExposedBlob> DMIEntryPointExposedBlob::must_create(PhysicalAddress dmi_entry_point, size_t blob_size)
{
    return adopt_ref(*new (nothrow) DMIEntryPointExposedBlob(dmi_entry_point, blob_size));
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

}
