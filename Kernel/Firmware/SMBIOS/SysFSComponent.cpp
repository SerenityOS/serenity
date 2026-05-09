/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Firmware/SMBIOS/SysFSComponent.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullRefPtr<SysFSSMBIOSComponent> SysFSSMBIOSComponent::must_create(Type type, PhysicalAddress blob_paddr, size_t blob_size)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSSMBIOSComponent(type, blob_paddr, blob_size)).release_nonnull();
}

UNMAP_AFTER_INIT SysFSSMBIOSComponent::SysFSSMBIOSComponent(Type type, PhysicalAddress blob_paddr, size_t blob_size)
    : m_blob_paddr(blob_paddr)
    , m_blob_length(blob_size)
    , m_type(type)
{
}

ErrorOr<size_t> SysFSSMBIOSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

StringView SysFSSMBIOSComponent::name() const
{
    switch (m_type) {
    case Type::SMBIOSEntryPoint:
        return "entry_point"sv;
    case Type::SMBIOSTable:
        return "structure_table"sv;
    default:
        break;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullOwnPtr<KBuffer>> SysFSSMBIOSComponent::try_to_generate_buffer() const
{
    auto blob = TRY(Memory::map_typed<u8>((m_blob_paddr), m_blob_length));
    return KBuffer::try_create_with_bytes("SysFSSMBIOSComponent: Blob"sv, Span<u8> { blob.ptr(), m_blob_length });
}
}
