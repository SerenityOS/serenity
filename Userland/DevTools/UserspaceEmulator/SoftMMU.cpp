/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "SoftMMU.h"
#include "Emulator.h"
#include "MmapRegion.h"
#include "Report.h"
#include "SharedBufferRegion.h"
#include <AK/ByteBuffer.h>
#include <AK/Memory.h>

namespace UserspaceEmulator {

SoftMMU::SoftMMU(Emulator& emulator)
    : m_emulator(emulator)
{
}

void SoftMMU::add_region(NonnullOwnPtr<Region> region)
{
    ASSERT(!find_region({ 0x23, region->base() }));

    // FIXME: More sanity checks pls
    if (is<SharedBufferRegion>(*region))
        m_shbuf_regions.set(static_cast<SharedBufferRegion*>(region.ptr())->shbuf_id(), region.ptr());

    size_t first_page_in_region = region->base() / PAGE_SIZE;
    size_t last_page_in_region = (region->base() + region->size() - 1) / PAGE_SIZE;
    for (size_t page = first_page_in_region; page <= last_page_in_region; ++page) {
        m_page_to_region_map[page] = region.ptr();
    }

    m_regions.append(move(region));
}

void SoftMMU::remove_region(Region& region)
{
    size_t first_page_in_region = region.base() / PAGE_SIZE;
    for (size_t i = 0; i < ceil_div(region.size(), PAGE_SIZE); ++i) {
        m_page_to_region_map[first_page_in_region + i] = nullptr;
    }

    if (is<SharedBufferRegion>(region))
        m_shbuf_regions.remove(static_cast<SharedBufferRegion&>(region).shbuf_id());
    m_regions.remove_first_matching([&](auto& entry) { return entry.ptr() == &region; });
}

void SoftMMU::set_tls_region(NonnullOwnPtr<Region> region)
{
    ASSERT(!m_tls_region);
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
    auto buffer = ByteBuffer::create_uninitialized(size);
    copy_from_vm(buffer.data(), source, size);
    return buffer;
}

SharedBufferRegion* SoftMMU::shbuf_region(int shbuf_id)
{
    return (SharedBufferRegion*)m_shbuf_regions.get(shbuf_id).value_or(nullptr);
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
