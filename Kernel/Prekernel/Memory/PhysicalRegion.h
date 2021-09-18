/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Prekernel/Images/Multiboot.h>
#include <Kernel/Prekernel/Memory/PhysicalRange.h>

namespace Prekernel {

class PhysicalRegion {
    AK_MAKE_ETERNAL
public:
    // Note: This adopts e820 entry types as the "standard".
    // Multiboot protocol 1 (as well as the linux boot protocol for providing memory map) also adopts this convention.
    enum class Type {
        Usable = 1,
        Reserved = 2,
        ACPIReclaimable = 3,
        ACPINVS = 4,
        BadMemory = 5,
    };

public:
    static NonnullOwnPtr<PhysicalRegion> create(PhysicalAddress, size_t, Multiboot::MemoryEntryType);
    const PhysicalRange& range() const { return m_range; }
    Type type() const { return m_type; }

private:
    PhysicalRegion(PhysicalAddress, size_t, Type);

    const PhysicalRange m_range;
    const Type m_type;
};

}
