/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftMMU.h"
#include "Emulator.h"
#include "MmapRegion.h"
#include "Report.h"
#include <AK/ByteBuffer.h>
#include <AK/Memory.h>
#include <AK/QuickSort.h>

namespace UserspaceEmulator {

SoftMMU::SoftMMU(Emulator& emulator)
    : m_emulator(emulator)
{
}

void SoftMMU::add_region(NonnullOwnPtr<Region> region)
{
    VERIFY(!find_region({ 0x23, region->base() }));

    size_t first_page_in_region = region->base() / PAGE_SIZE;
    size_t last_page_in_region = (region->base() + region->size() - 1) / PAGE_SIZE;
    for (size_t page = first_page_in_region; page <= last_page_in_region; ++page) {
        m_page_to_region_map[page] = region.ptr();
    }

    m_regions.append(move(region));
    quick_sort((Vector<OwnPtr<Region>>&)m_regions, [](auto& a, auto& b) { return a->base() < b->base(); });
}

void SoftMMU::remove_region(Region& region)
{
    size_t first_page_in_region = region.base() / PAGE_SIZE;
    for (size_t i = 0; i < ceil_div(region.size(), PAGE_SIZE); ++i) {
        m_page_to_region_map[first_page_in_region + i] = nullptr;
    }

    m_regions.remove_first_matching([&](auto& entry) { return entry.ptr() == &region; });
}

void SoftMMU::ensure_split_at(X86::LogicalAddress address)
{
    // FIXME: If this fails, call Emulator::dump_backtrace
    VERIFY(address.selector() != 0x2b);

    u32 offset = address.offset();
    VERIFY((offset & (PAGE_SIZE - 1)) == 0);
    size_t page_index = address.offset() / PAGE_SIZE;

    if (!page_index)
        return;
    if (m_page_to_region_map[page_index - 1] != m_page_to_region_map[page_index])
        return;
    if (!m_page_to_region_map[page_index])
        return;

    // If we get here, we know that the page exists and belongs to a region, that there is
    // a previous page, and that it belongs to the same region.
    auto* old_region = verify_cast<MmapRegion>(m_page_to_region_map[page_index]);

    // dbgln("splitting at {:p}", address.offset());
    // dbgln("    old region: {:p}-{:p}", old_region->base(), old_region->end() - 1);

    NonnullOwnPtr<MmapRegion> new_region = old_region->split_at(VirtualAddress(offset));
    // dbgln("    new region: {:p}-{:p}", new_region->base(), new_region->end() - 1);
    // dbgln(" up old region: {:p}-{:p}", old_region->base(), old_region->end() - 1);

    size_t first_page_in_region = new_region->base() / PAGE_SIZE;
    size_t last_page_in_region = (new_region->base() + new_region->size() - 1) / PAGE_SIZE;

    // dbgln("  @ remapping pages {} thru {}", first_page_in_region, last_page_in_region);

    for (size_t page = first_page_in_region; page <= last_page_in_region; ++page) {
        VERIFY(m_page_to_region_map[page] == old_region);
        m_page_to_region_map[page] = new_region.ptr();
    }

    m_regions.append(move(new_region));
    quick_sort((Vector<OwnPtr<Region>>&)m_regions, [](auto& a, auto& b) { return a->base() < b->base(); });
}

void SoftMMU::set_tls_region(NonnullOwnPtr<Region> region)
{
    VERIFY(!m_tls_region);
    m_tls_region = move(region);
}

ValueWithShadow<u8> SoftMMU::read8(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::read8: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_readable()) {
        reportln("SoftMMU::read8: Non-readable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    return region->read8(address.offset() - region->base());
}

ValueWithShadow<u16> SoftMMU::read16(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::read16: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_readable()) {
        reportln("SoftMMU::read16: Non-readable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    return region->read16(address.offset() - region->base());
}

ValueWithShadow<u32> SoftMMU::read32(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::read32: No region for @ {:04x}:{:p}", address.selector(), address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_readable()) {
        reportln("SoftMMU::read32: Non-readable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    return region->read32(address.offset() - region->base());
}

ValueWithShadow<u64> SoftMMU::read64(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::read64: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_readable()) {
        reportln("SoftMMU::read64: Non-readable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    return region->read64(address.offset() - region->base());
}

ValueWithShadow<u128> SoftMMU::read128(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::read128: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_readable()) {
        reportln("SoftMMU::read128: Non-readable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    return region->read128(address.offset() - region->base());
}

ValueWithShadow<u256> SoftMMU::read256(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::read256: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_readable()) {
        reportln("SoftMMU::read256: Non-readable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    return region->read256(address.offset() - region->base());
}

void SoftMMU::write8(X86::LogicalAddress address, ValueWithShadow<u8> value)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::write8: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_writable()) {
        reportln("SoftMMU::write8: Non-writable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }
    region->write8(address.offset() - region->base(), value);
}

void SoftMMU::write16(X86::LogicalAddress address, ValueWithShadow<u16> value)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::write16: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_writable()) {
        reportln("SoftMMU::write16: Non-writable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    region->write16(address.offset() - region->base(), value);
}

void SoftMMU::write32(X86::LogicalAddress address, ValueWithShadow<u32> value)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::write32: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_writable()) {
        reportln("SoftMMU::write32: Non-writable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    region->write32(address.offset() - region->base(), value);
}

void SoftMMU::write64(X86::LogicalAddress address, ValueWithShadow<u64> value)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::write64: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_writable()) {
        reportln("SoftMMU::write64: Non-writable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    region->write64(address.offset() - region->base(), value);
}

void SoftMMU::write128(X86::LogicalAddress address, ValueWithShadow<u128> value)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::write128: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_writable()) {
        reportln("SoftMMU::write128: Non-writable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    region->write128(address.offset() - region->base(), value);
}

void SoftMMU::write256(X86::LogicalAddress address, ValueWithShadow<u256> value)
{
    auto* region = find_region(address);
    if (!region) {
        reportln("SoftMMU::write256: No region for @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    if (!region->is_writable()) {
        reportln("SoftMMU::write256: Non-writable region @ {:p}", address.offset());
        m_emulator.dump_backtrace();
        TODO();
    }

    region->write256(address.offset() - region->base(), value);
}

void SoftMMU::copy_to_vm(FlatPtr destination, const void* source, size_t size)
{
    // FIXME: We should have a way to preserve the shadow data here as well.
    for (size_t i = 0; i < size; ++i)
        write8({ 0x23, destination + i }, shadow_wrap_as_initialized(((const u8*)source)[i]));
}

void SoftMMU::copy_from_vm(void* destination, const FlatPtr source, size_t size)
{
    // FIXME: We should have a way to preserve the shadow data here as well.
    for (size_t i = 0; i < size; ++i)
        ((u8*)destination)[i] = read8({ 0x23, source + i }).value();
}

ByteBuffer SoftMMU::copy_buffer_from_vm(const FlatPtr source, size_t size)
{
    auto buffer = ByteBuffer::create_uninitialized(size).release_value(); // FIXME: Handle possible OOM situation.
    copy_from_vm(buffer.data(), source, size);
    return buffer;
}

bool SoftMMU::fast_fill_memory8(X86::LogicalAddress address, size_t size, ValueWithShadow<u8> value)
{
    if (!size)
        return true;
    auto* region = find_region(address);
    if (!region)
        return false;
    if (!region->contains(address.offset() + size - 1))
        return false;

    if (is<MmapRegion>(*region) && static_cast<const MmapRegion&>(*region).is_malloc_block()) {
        if (auto* tracer = m_emulator.malloc_tracer()) {
            // FIXME: Add a way to audit an entire range of memory instead of looping here!
            for (size_t i = 0; i < size; ++i) {
                tracer->audit_write(*region, address.offset() + (i * sizeof(u8)), sizeof(u8));
            }
        }
    }

    size_t offset_in_region = address.offset() - region->base();
    memset(region->data() + offset_in_region, value.value(), size);
    memset(region->shadow_data() + offset_in_region, value.shadow(), size);
    return true;
}

bool SoftMMU::fast_fill_memory32(X86::LogicalAddress address, size_t count, ValueWithShadow<u32> value)
{
    if (!count)
        return true;
    auto* region = find_region(address);
    if (!region)
        return false;
    if (!region->contains(address.offset() + (count * sizeof(u32)) - 1))
        return false;

    if (is<MmapRegion>(*region) && static_cast<const MmapRegion&>(*region).is_malloc_block()) {
        if (auto* tracer = m_emulator.malloc_tracer()) {
            // FIXME: Add a way to audit an entire range of memory instead of looping here!
            for (size_t i = 0; i < count; ++i) {
                tracer->audit_write(*region, address.offset() + (i * sizeof(u32)), sizeof(u32));
            }
        }
    }

    size_t offset_in_region = address.offset() - region->base();
    fast_u32_fill((u32*)(region->data() + offset_in_region), value.value(), count);
    fast_u32_fill((u32*)(region->shadow_data() + offset_in_region), value.shadow(), count);
    return true;
}

}
