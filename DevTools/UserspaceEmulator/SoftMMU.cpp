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

namespace UserspaceEmulator {

SoftMMU::Region* SoftMMU::find_region(u32 address)
{
    for (auto& region : m_regions) {
        if (region.contains(address))
            return &region;
    }
    return nullptr;
}

void SoftMMU::add_region(NonnullOwnPtr<Region> region)
{
    ASSERT(!find_region(region->base()));
    // FIXME: More sanity checks pls
    m_regions.append(move(region));
}

u8 SoftMMU::read8(u32 address)
{
    auto* region = find_region(address);
    if (!region) {
        warn() << "SoftMMU::read8: No region for @" << (const void*)address;
        TODO();
    }

    return region->read8(address - region->base());
}

u16 SoftMMU::read16(u32 address)
{
    auto* region = find_region(address);
    if (!region) {
        warn() << "SoftMMU::read16: No region for @" << (const void*)address;
        TODO();
    }

    return region->read16(address - region->base());
}

u32 SoftMMU::read32(u32 address)
{
    auto* region = find_region(address);
    if (!region) {
        warn() << "SoftMMU::read32: No region for @" << (const void*)address;
        TODO();
    }

    return region->read32(address - region->base());
}

void SoftMMU::write8(u32 address, u8 value)
{
    auto* region = find_region(address);
    if (!region) {
        warn() << "SoftMMU::write8: No region for @" << (const void*)address;
        TODO();
    }

    region->write8(address - region->base(), value);
}

void SoftMMU::write16(u32 address, u16 value)
{
    auto* region = find_region(address);
    if (!region) {
        warn() << "SoftMMU::write16: No region for @" << (const void*)address;
        TODO();
    }

    region->write16(address - region->base(), value);
}

void SoftMMU::write32(u32 address, u32 value)
{
    auto* region = find_region(address);
    if (!region) {
        warn() << "SoftMMU::write32: No region for @" << (const void*)address;
        TODO();
    }

    region->write32(address - region->base(), value);
}

}
