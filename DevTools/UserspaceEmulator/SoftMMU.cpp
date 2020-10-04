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
#include "SharedBufferRegion.h"
#include <AK/ByteBuffer.h>

namespace UserspaceEmulator {

SoftMMU::Region* SoftMMU::find_region(X86::LogicalAddress address)
{
    if (address.selector() == 0x28)
        return m_tls_region.ptr();

    for (auto& region : m_regions) {
        if (region.contains(address.offset()))
            return &region;
    }
    return nullptr;
}

void SoftMMU::add_region(NonnullOwnPtr<Region> region)
{
    ASSERT(!find_region({ 0x20, region->base() }));
    // FIXME: More sanity checks pls
    if (region->is_shared_buffer())
        m_shbuf_regions.set(static_cast<SharedBufferRegion*>(region.ptr())->shbuf_id(), region.ptr());
    m_regions.append(move(region));
}

void SoftMMU::remove_region(Region& region)
{
    if (region.is_shared_buffer())
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
        warnln("SoftMMU::read8: No region for @ {:p}", address.offset());
        TODO();
    }

    return region->read8(address.offset() - region->base());
}

ValueWithShadow<u16> SoftMMU::read16(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        warnln("SoftMMU::read16: No region for @ {:p}", address.offset());
        TODO();
    }

    return region->read16(address.offset() - region->base());
}

ValueWithShadow<u32> SoftMMU::read32(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        warnln("SoftMMU::read32: No region for @ {:p}", address.offset());
        TODO();
    }

    return region->read32(address.offset() - region->base());
}

ValueWithShadow<u64> SoftMMU::read64(X86::LogicalAddress address)
{
    auto* region = find_region(address);
    if (!region) {
        warnln("SoftMMU::read64: No region for @ {:p}", address.offset());
        TODO();
    }

    return region->read64(address.offset() - region->base());
}

void SoftMMU::write8(X86::LogicalAddress address, ValueWithShadow<u8> value)
{
    auto* region = find_region(address);
    if (!region) {
        warnln("SoftMMU::write8: No region for @ {:p}", address.offset());
        TODO();
    }

    region->write8(address.offset() - region->base(), value);
}

void SoftMMU::write16(X86::LogicalAddress address, ValueWithShadow<u16> value)
{
    auto* region = find_region(address);
    if (!region) {
        warnln("SoftMMU::write16: No region for @ {:p}", address.offset());
        TODO();
    }

    region->write16(address.offset() - region->base(), value);
}

void SoftMMU::write32(X86::LogicalAddress address, ValueWithShadow<u32> value)
{
    auto* region = find_region(address);
    if (!region) {
        warnln("SoftMMU::write32: No region for @ {:p}", address.offset());
        TODO();
    }

    region->write32(address.offset() - region->base(), value);
}

void SoftMMU::write64(X86::LogicalAddress address, ValueWithShadow<u64> value)
{
    auto* region = find_region(address);
    if (!region) {
        warnln("SoftMMU::write64: No region for @ {:p}", address.offset());
        TODO();
    }

    region->write64(address.offset() - region->base(), value);
}

void SoftMMU::copy_to_vm(FlatPtr destination, const void* source, size_t size)
{
    // FIXME: We should have a way to preserve the shadow data here as well.
    for (size_t i = 0; i < size; ++i)
        write8({ 0x20, destination + i }, shadow_wrap_as_initialized(((const u8*)source)[i]));
}

void SoftMMU::copy_from_vm(void* destination, const FlatPtr source, size_t size)
{
    // FIXME: We should have a way to preserve the shadow data here as well.
    for (size_t i = 0; i < size; ++i)
        ((u8*)destination)[i] = read8({ 0x20, source + i }).value();
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

}
