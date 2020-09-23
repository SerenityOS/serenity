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

#include "SimpleRegion.h"
#include <string.h>

namespace UserspaceEmulator {

SimpleRegion::SimpleRegion(u32 base, u32 size)
    : Region(base, size)
{
    m_data = (u8*)calloc(1, size);
    m_shadow_data = (u8*)malloc(size);
    memset(m_shadow_data, 1, size);
}

SimpleRegion::~SimpleRegion()
{
    free(m_shadow_data);
    free(m_data);
}

ValueWithShadow<u8> SimpleRegion::read8(FlatPtr offset)
{
    ASSERT(offset < size());
    return { *reinterpret_cast<const u8*>(m_data + offset), *reinterpret_cast<const u8*>(m_shadow_data + offset) };
}

ValueWithShadow<u16> SimpleRegion::read16(u32 offset)
{
    ASSERT(offset + 1 < size());
    return { *reinterpret_cast<const u16*>(m_data + offset), *reinterpret_cast<const u16*>(m_shadow_data + offset) };
}

ValueWithShadow<u32> SimpleRegion::read32(u32 offset)
{
    ASSERT(offset + 3 < size());
    return { *reinterpret_cast<const u32*>(m_data + offset), *reinterpret_cast<const u32*>(m_shadow_data + offset) };
}

ValueWithShadow<u64> SimpleRegion::read64(u32 offset)
{
    ASSERT(offset + 7 < size());
    return { *reinterpret_cast<const u64*>(m_data + offset), *reinterpret_cast<const u64*>(m_shadow_data + offset) };
}

void SimpleRegion::write8(u32 offset, ValueWithShadow<u8> value)
{
    ASSERT(offset < size());
    *reinterpret_cast<u8*>(m_data + offset) = value.value();
    *reinterpret_cast<u8*>(m_shadow_data + offset) = value.shadow();
}

void SimpleRegion::write16(u32 offset, ValueWithShadow<u16> value)
{
    ASSERT(offset + 1 < size());
    *reinterpret_cast<u16*>(m_data + offset) = value.value();
    *reinterpret_cast<u16*>(m_shadow_data + offset) = value.shadow();
}

void SimpleRegion::write32(u32 offset, ValueWithShadow<u32> value)
{
    ASSERT(offset + 3 < size());
    *reinterpret_cast<u32*>(m_data + offset) = value.value();
    *reinterpret_cast<u32*>(m_shadow_data + offset) = value.shadow();
}

void SimpleRegion::write64(u32 offset, ValueWithShadow<u64> value)
{
    ASSERT(offset + 7 < size());
    *reinterpret_cast<u64*>(m_data + offset) = value.value();
    *reinterpret_cast<u64*>(m_shadow_data + offset) = value.shadow();
}

u8* SimpleRegion::cacheable_ptr(u32 offset)
{
    return m_data + offset;
}

}
