/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/StringView.h>
#include <AK/Try.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::Memory {

template<typename T>
struct TypedMapping {
    T const* ptr() const { return reinterpret_cast<T const*>(region->vaddr().offset(offset).as_ptr()); }
    T* ptr() { return reinterpret_cast<T*>(region->vaddr().offset(offset).as_ptr()); }
    VirtualAddress base_address() const { return region->vaddr().offset(offset); }
    T const* operator->() const { return ptr(); }
    T* operator->() { return ptr(); }
    T const& operator*() const { return *ptr(); }
    T& operator*() { return *ptr(); }

    OwnPtr<Region> region;
    PhysicalAddress paddr;
    size_t offset { 0 };
    size_t length { 0 };
};

template<typename T>
struct TypedMapping<T[]> {
    T const* ptr() const { return reinterpret_cast<T const*>(region->vaddr().offset(offset).as_ptr()); }
    T* ptr() { return reinterpret_cast<T*>(region->vaddr().offset(offset).as_ptr()); }
    VirtualAddress base_address() const { return region->vaddr().offset(offset); }
    T const* operator->() const { return ptr(); }
    T* operator->() { return ptr(); }
    T const& operator*() const { return *ptr(); }
    T& operator*() { return *ptr(); }

    size_t size() const { return length / sizeof(T); }
    T const& operator[](size_t index) const
    {
        VERIFY(index < size());
        return ptr()[index];
    }
    T& operator[](size_t index)
    {
        VERIFY(index < size());
        return ptr()[index];
    }

    OwnPtr<Region> region;
    PhysicalAddress paddr;
    size_t offset { 0 };
    size_t length { 0 };
};

template<typename T>
static ErrorOr<NonnullOwnPtr<TypedMapping<T>>> adopt_new_nonnull_own_typed_mapping(PhysicalAddress paddr, size_t length, Region::Access access = Region::Access::Read)
{
    auto mapping_length = TRY(page_round_up(paddr.offset_in_page() + length));
    auto region = TRY(MM.allocate_mmio_kernel_region(paddr.page_base(), mapping_length, {}, access));
    auto table = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Memory::TypedMapping<T>()));
    table->region = move(region);
    table->offset = paddr.offset_in_page();
    table->paddr = paddr;
    table->length = length;
    return table;
}

template<typename T>
static ErrorOr<TypedMapping<T>> map_typed(PhysicalAddress paddr, size_t length, Region::Access access = Region::Access::Read)
{
    TypedMapping<T> table;
    auto mapping_length = TRY(page_round_up(paddr.offset_in_page() + length));
    table.region = TRY(MM.allocate_mmio_kernel_region(paddr.page_base(), mapping_length, {}, access));
    table.offset = paddr.offset_in_page();
    table.paddr = paddr;
    table.length = length;
    return table;
}

template<typename T>
static ErrorOr<TypedMapping<T>> map_typed(PhysicalAddress paddr)
{
    return map_typed<T>(paddr, sizeof(T));
}

template<typename T>
static ErrorOr<TypedMapping<T>> map_typed_writable(PhysicalAddress paddr)
{
    return map_typed<T>(paddr, sizeof(T), Region::Access::Read | Region::Access::Write);
}

template<typename T>
static ErrorOr<NonnullOwnPtr<TypedMapping<T[]>>> adopt_new_nonnull_own_typed_mapping_array(PhysicalAddress paddr, size_t items, Region::Access access = Region::Access::Read)
{
    size_t length_in_bytes = items * sizeof(T);
    auto mapping_length = TRY(page_round_up(paddr.offset_in_page() + length_in_bytes));
    auto region = TRY(MM.allocate_mmio_kernel_region(paddr.page_base(), mapping_length, {}, access));
    auto table = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Memory::TypedMapping<T[]>()));
    table->region = move(region);
    table->offset = paddr.offset_in_page();
    table->paddr = paddr;
    table->length = length_in_bytes;
    return table;
}

template<typename T>
static ErrorOr<TypedMapping<T[]>> map_typed_array(PhysicalAddress paddr, size_t items, Region::Access access = Region::Access::Read)
{
    size_t length_in_bytes = items * sizeof(T);
    return map_typed<T[]>(paddr, length_in_bytes, access);
}

template<typename T>
static ErrorOr<TypedMapping<T[]>> allocate_dma_region_as_typed_array(size_t items, StringView name, Region::Access access, MemoryType memory_type = MemoryType::NonCacheable)
{
    size_t length_in_bytes = items * sizeof(T);
    size_t mapping_length = TRY(page_round_up(length_in_bytes));
    auto region = TRY(MM.allocate_dma_buffer_pages(mapping_length, name, access, memory_type));

    Memory::TypedMapping<T[]> table;
    table.paddr = region->physical_page(0)->paddr();
    table.region = move(region);
    table.offset = 0;
    table.length = length_in_bytes;

    return table;
}

}
