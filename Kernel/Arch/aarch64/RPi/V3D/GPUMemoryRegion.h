/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveRedBlackTree.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/Arch/aarch64/RPi/V3D/GPUVirtualAddress.h>
#include <Kernel/Memory/RegionTree.h>

namespace Kernel::RPi::V3D {

class GPUMemoryRegion {
    template<typename Region, size_t PageSize>
    friend class Memory::RegionTree;

public:
    static ErrorOr<NonnullOwnPtr<GPUMemoryRegion>> create_unplaced()
    {
        return adopt_nonnull_own_or_enomem(new (nothrow) GPUMemoryRegion({ GPUVirtualAddress { 0u }, 0 }));
    }

    GPUVirtualAddress vaddr() const { return m_range.base(); }
    GPUVirtualRange range() const { return m_range; }

private:
    GPUMemoryRegion(GPUVirtualRange range)
        : m_range(range)
    {
    }

    GPUVirtualRange m_range;
    IntrusiveRedBlackTreeNode<FlatPtr, GPUMemoryRegion, RawPtr<GPUMemoryRegion>> m_tree_node;
};

}
