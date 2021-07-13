/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SimpleRegion.h"
#include <AK/ByteReader.h>
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
    return { m_data[offset], m_shadow_data[offset] };
}

ValueWithShadow<u16> SimpleRegion::read16(u32 offset)
{
    VERIFY(offset + 1 < size());

    u16 value, shadow;
    ByteReader::load<u16>(m_data + offset, value);
    ByteReader::load<u16>(m_shadow_data + offset, shadow);

    return { value, shadow };
}

ValueWithShadow<u32> SimpleRegion::read32(u32 offset)
{
    VERIFY(offset + 3 < size());

    u32 value, shadow;
    ByteReader::load<u32>(m_data + offset, value);
    ByteReader::load<u32>(m_shadow_data + offset, shadow);

    return { value, shadow };
}

ValueWithShadow<u64> SimpleRegion::read64(u32 offset)
{
    VERIFY(offset + 7 < size());

    u64 value, shadow;
    ByteReader::load<u64>(m_data + offset, value);
    ByteReader::load<u64>(m_shadow_data + offset, shadow);

    return { value, shadow };
}

ValueWithShadow<u128> SimpleRegion::read128(u32 offset)
{
    VERIFY(offset + 15 < size());
    u128 value, shadow;
    ByteReader::load(m_data + offset, value);
    ByteReader::load(m_shadow_data + offset, shadow);
    return { value, shadow };
}

ValueWithShadow<u256> SimpleRegion::read256(u32 offset)
{
    VERIFY(offset + 31 < size());
    u256 value, shadow;
    ByteReader::load(m_data + offset, value);
    ByteReader::load(m_shadow_data + offset, shadow);
    return { value, shadow };
}

void SimpleRegion::write8(u32 offset, ValueWithShadow<u8> value)
{
    VERIFY(offset < size());
    m_data[offset] = value.value();
    m_shadow_data[offset] = value.shadow();
}

void SimpleRegion::write16(u32 offset, ValueWithShadow<u16> value)
{
    VERIFY(offset + 1 < size());
    ByteReader::store(m_data + offset, value.value());
    ByteReader::store(m_shadow_data + offset, value.shadow());
}

void SimpleRegion::write32(u32 offset, ValueWithShadow<u32> value)
{
    VERIFY(offset + 3 < size());
    ByteReader::store(m_data + offset, value.value());
    ByteReader::store(m_shadow_data + offset, value.shadow());
}

void SimpleRegion::write64(u32 offset, ValueWithShadow<u64> value)
{
    VERIFY(offset + 7 < size());
    ByteReader::store(m_data + offset, value.value());
    ByteReader::store(m_shadow_data + offset, value.shadow());
}
void SimpleRegion::write128(u32 offset, ValueWithShadow<u128> value)
{
    VERIFY(offset + 15 < size());
    ByteReader::store(m_data + offset, value.value());
    ByteReader::store(m_shadow_data + offset, value.shadow());
}
void SimpleRegion::write256(u32 offset, ValueWithShadow<u256> value)
{
    VERIFY(offset + 31 < size());
    ByteReader::store(m_data + offset, value.value());
    ByteReader::store(m_shadow_data + offset, value.shadow());
}

u8* SimpleRegion::cacheable_ptr(u32 offset)
{
    return m_data + offset;
}

}
