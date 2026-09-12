/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Arch/aarch64/RPi/V3D/GPUVirtualAddress.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi::V3D {

class V3D;

static constexpr size_t V3D_VIRTUAL_MEMORY_SIZE = 4uz * GiB;

static constexpr size_t V3D_PAGE_SHIFT = 12;
static constexpr size_t V3D_PAGE_SIZE = 1uz << V3D_PAGE_SHIFT;

class PageTable {
public:
    static ErrorOr<PageTable> create();

    void insert_entries_for_buffer(Badge<V3D>, GPUVirtualAddress, Memory::VMObject const&);
    void remove_entries_for_buffer(Badge<V3D>, GPUVirtualAddress, Memory::VMObject const&);

    PhysicalAddress physical_address() const { return m_entries.paddr; }

private:
    PageTable(Memory::TypedMapping<u32 volatile[]>);

    Memory::TypedMapping<u32 volatile[]> m_entries;
};

}
