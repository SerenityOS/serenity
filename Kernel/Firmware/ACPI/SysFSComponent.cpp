/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SysFSComponent.h"
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT NonnullLockRefPtr<ACPISysFSComponent> ACPISysFSComponent::create(StringView name, PhysicalAddress paddr, size_t table_size)
{
    // FIXME: Handle allocation failure gracefully
    auto table_name = KString::must_create(name);
    return adopt_lock_ref(*new (nothrow) ACPISysFSComponent(move(table_name), paddr, table_size));
}

ErrorOr<size_t> ACPISysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> ACPISysFSComponent::try_to_generate_buffer() const
{
    auto acpi_blob = TRY(Memory::map_typed<u8>((m_paddr), m_length));
    return KBuffer::try_create_with_bytes("ACPISysFSComponent: Blob"sv, Span<u8> { acpi_blob.ptr(), m_length });
}

UNMAP_AFTER_INIT ACPISysFSComponent::ACPISysFSComponent(NonnullOwnPtr<KString> table_name, PhysicalAddress paddr, size_t table_size)
    : m_paddr(paddr)
    , m_length(table_size)
    , m_table_name(move(table_name))
{
}

}
