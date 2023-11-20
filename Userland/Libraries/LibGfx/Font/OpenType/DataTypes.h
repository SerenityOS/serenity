/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Lukas Affolter <git@lukasach.dev>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>
#include <LibGfx/FourCC.h>

// https://learn.microsoft.com/en-us/typography/opentype/spec/otff#data-types
namespace OpenType {

using Uint8 = u8;
using Int8 = i8;
using Uint16 = BigEndian<u16>;
using Int16 = BigEndian<i16>;
// FIXME: Uint24
using Uint32 = BigEndian<u32>;
using Int32 = BigEndian<i32>;

struct [[gnu::packed]] Fixed {
    BigEndian<u16> integer;
    BigEndian<u16> fraction;
};
static_assert(AssertSize<Fixed, 4>());

using FWord = BigEndian<i16>;
using UFWord = BigEndian<u16>;

// FIXME: F2Dot14

struct [[gnu::packed]] LongDateTime {
    BigEndian<u64> value;
};
static_assert(AssertSize<LongDateTime, 8>());

using Tag = Gfx::FourCC;

using Offset16 = BigEndian<u16>;
// FIXME: Offset24
using Offset32 = BigEndian<u32>;

struct [[gnu::packed]] Version16Dot16 {
    BigEndian<u16> major;
    BigEndian<u16> minor;
};
static_assert(AssertSize<Version16Dot16, 4>());

}

namespace AK {
template<>
struct Traits<OpenType::Fixed> : public DefaultTraits<OpenType::Fixed> {
    static constexpr bool is_trivially_serializable() { return true; }
};
template<>
struct Traits<OpenType::LongDateTime> : public DefaultTraits<OpenType::LongDateTime> {
    static constexpr bool is_trivially_serializable() { return true; }
};
template<>
struct Traits<OpenType::Tag> : public DefaultTraits<OpenType::Tag> {
    static constexpr bool is_trivially_serializable() { return true; }
};
template<>
struct Traits<OpenType::Version16Dot16> : public DefaultTraits<OpenType::Version16Dot16> {
    static constexpr bool is_trivially_serializable() { return true; }
};
}
