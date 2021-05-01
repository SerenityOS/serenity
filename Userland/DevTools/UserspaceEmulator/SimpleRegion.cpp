/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    VERIFY(offset < size());
    return { *reinterpret_cast<const u8*>(m_data + offset), *reinterpret_cast<const u8*>(m_shadow_data + offset) };
}

ValueWithShadow<u16> SimpleRegion::read16(u32 offset)
{
    VERIFY(offset + 1 < size());
    return { *reinterpret_cast<const u16*>(m_data + offset), *reinterpret_cast<const u16*>(m_shadow_data + offset) };
}

ValueWithShadow<u32> SimpleRegion::read32(u32 offset)
{
    VERIFY(offset + 3 < size());
    return { *reinterpret_cast<const u32*>(m_data + offset), *reinterpret_cast<const u32*>(m_shadow_data + offset) };
}

ValueWithShadow<u64> SimpleRegion::read64(u32 offset)
{
    VERIFY(offset + 7 < size());
    return { *reinterpret_cast<const u64*>(m_data + offset), *reinterpret_cast<const u64*>(m_shadow_data + offset) };
}

ValueWithShadow<u128> SimpleRegion::read128(u32 offset)
{
    VERIFY(offset + 15 < size());
    return { *reinterpret_cast<const u128*>(m_data + offset), *reinterpret_cast<const u128*>(m_shadow_data + offset) };
}

ValueWithShadow<u256> SimpleRegion::read256(u32 offset)
{
    VERIFY(offset + 31 < size());
    return { *reinterpret_cast<const u256*>(m_data + offset), *reinterpret_cast<const u256*>(m_shadow_data + offset) };
}

void SimpleRegion::write8(u32 offset, ValueWithShadow<u8> value)
{
    VERIFY(offset < size());
    *reinterpret_cast<u8*>(m_data + offset) = value.value();
    *reinterpret_cast<u8*>(m_shadow_data + offset) = value.shadow();
}

void SimpleRegion::write16(u32 offset, ValueWithShadow<u16> value)
{
    VERIFY(offset + 1 < size());
    *reinterpret_cast<u16*>(m_data + offset) = value.value();
    *reinterpret_cast<u16*>(m_shadow_data + offset) = value.shadow();
}

void SimpleRegion::write32(u32 offset, ValueWithShadow<u32> value)
{
    VERIFY(offset + 3 < size());
    *reinterpret_cast<u32*>(m_data + offset) = value.value();
    *reinterpret_cast<u32*>(m_shadow_data + offset) = value.shadow();
}

void SimpleRegion::write64(u32 offset, ValueWithShadow<u64> value)
{
    VERIFY(offset + 7 < size());
    *reinterpret_cast<u64*>(m_data + offset) = value.value();
    *reinterpret_cast<u64*>(m_shadow_data + offset) = value.shadow();
}
void SimpleRegion::write128(u32 offset, ValueWithShadow<u128> value)
{
    VERIFY(offset + 15 < size());
    *reinterpret_cast<u128*>(m_data + offset) = value.value();
    *reinterpret_cast<u128*>(m_shadow_data + offset) = value.shadow();
}
void SimpleRegion::write256(u32 offset, ValueWithShadow<u256> value)
{
    VERIFY(offset + 31 < size());
    *reinterpret_cast<u256*>(m_data + offset) = value.value();
    *reinterpret_cast<u256*>(m_shadow_data + offset) = value.shadow();
}

u8* SimpleRegion::cacheable_ptr(u32 offset)
{
    return m_data + offset;
}

}
