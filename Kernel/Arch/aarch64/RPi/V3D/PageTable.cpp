/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/V3D/PageTable.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi::V3D {

static constexpr size_t PAGE_TABLE_ENTRY_COUNT = V3D_VIRTUAL_MEMORY_SIZE / V3D_PAGE_SIZE;

static constexpr u32 PAGE_TABLE_ENTRY_WRITABLE = 1u << 29;
static constexpr u32 PAGE_TABLE_ENTRY_VALID = 1u << 28;

ErrorOr<PageTable> PageTable::create()
{
    auto entries = TRY(Memory::allocate_dma_region_as_typed_array<u32 volatile>(PAGE_TABLE_ENTRY_COUNT, "V3D Page Table"sv, Memory::Region::Access::ReadWrite));
    return PageTable { move(entries) };
}

void PageTable::insert_entries_for_buffer(Badge<V3D>, GPUVirtualAddress gpu_vaddr, Memory::VMObject const& vmobject)
{
    VERIFY(static_cast<u64>(gpu_vaddr.get()) + vmobject.size() < V3D_VIRTUAL_MEMORY_SIZE);
    VERIFY((vmobject.size() % V3D_PAGE_SIZE) == 0);

    auto start_page_index = gpu_vaddr.get() / V3D_PAGE_SIZE;
    auto page_count = vmobject.size() / V3D_PAGE_SIZE;
    auto end_page_index = start_page_index + page_count;

    // NOTE: This code assumes that the CPU page size is equal to the V3D page size since we use GPU page indices
    //       to index into the VMObject physical page array.
    static_assert(PAGE_SIZE == V3D_PAGE_SIZE);

    for (size_t page_index = start_page_index; page_index < end_page_index; page_index++) {
        auto page_index_in_vmobject = page_index - start_page_index;

        auto paddr = vmobject.physical_pages()[page_index_in_vmobject]->paddr();

        // FIXME: Maybe allow userspace to create read-only mappings for the GPU?

        VERIFY(page_index < PAGE_TABLE_ENTRY_COUNT);
        VERIFY(m_entries[page_index] == 0); // A buffer should never be mapped twice!
        m_entries[page_index] = (paddr.get() / V3D_PAGE_SIZE) | PAGE_TABLE_ENTRY_VALID | PAGE_TABLE_ENTRY_WRITABLE;
    }
}

void PageTable::remove_entries_for_buffer(Badge<V3D>, GPUVirtualAddress gpu_vaddr, Memory::VMObject const& vmobject)
{
    VERIFY(static_cast<u64>(gpu_vaddr.get()) + vmobject.size() < V3D_VIRTUAL_MEMORY_SIZE);
    VERIFY((vmobject.size() % V3D_PAGE_SIZE) == 0);

    auto start_page_index = gpu_vaddr.get() / V3D_PAGE_SIZE;
    auto page_count = vmobject.size() / V3D_PAGE_SIZE;
    auto end_page_index = start_page_index + page_count;

    for (size_t page_index = start_page_index; page_index < end_page_index; page_index++) {
        VERIFY(page_index < PAGE_TABLE_ENTRY_COUNT);
        m_entries[page_index] = 0;
    }
}

PageTable::PageTable(Memory::TypedMapping<u32 volatile[]> entries)
    : m_entries(move(entries))
{
}

}
