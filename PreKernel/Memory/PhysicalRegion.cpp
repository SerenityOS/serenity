/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <PreKernel/Memory/PhysicalRegion.h>
#include <PreKernel/Memory/kmalloc.h>

namespace Prekernel {

NonnullOwnPtr<PhysicalRegion> PhysicalRegion::create(PhysicalAddress base_address, size_t length, Multiboot::MemoryEntryType entry_type)
{
    // Note: For multiboot protocol 1 entries, do a simple conversion.
    auto e820_entry_type = (PhysicalRegion::Type)entry_type;
    return make<PhysicalRegion>(*new PhysicalRegion(base_address, length, e820_entry_type));
}

PhysicalRegion::PhysicalRegion(PhysicalAddress base_address, size_t length, PhysicalRegion::Type entry_type)
    : m_range({ base_address, length })
    , m_type(entry_type)
{
    VERIFY(!m_range.is_null());
}

}
