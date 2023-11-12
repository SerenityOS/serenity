/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGfx/Size.h>

namespace Gfx {

namespace TIFF {

enum class Type {
    Byte = 1,
    ASCII = 2,
    UnsignedShort = 3,
    UnsignedLong = 4,
    UnsignedRational = 5,
    Undefined = 7,
    SignedLong = 9,
    SignedRational = 10,
    Float = 11,
    Double = 12,
    UTF8 = 129,
};

template<OneOf<u32, i32> x32>
struct Rational {
    using Type = x32;
    x32 numerator;
    x32 denominator;
};

using Value = Variant<u8, String, u16, u32, Rational<u32>, i32, Rational<i32>>;

// This enum is progessively defined across sections but summarized in:
// Appendix A: TIFF Tags Sorted by Number
enum class Compression {
    NoCompression = 1,
    CCITT = 2,
    Group3Fax = 3,
    Group4Fax = 4,
    LZW = 5,
    JPEG = 6,
    PackBits = 32773,
};

enum class Predictor {
    None = 1,
    HorizontalDifferencing = 2,
};

}

struct Metadata {
    IntSize size {};
    Array<u16, 3> bits_per_sample {};
    TIFF::Compression compression {};
    TIFF::Predictor predictor {};
    Vector<u32> strip_offsets {};
    u32 rows_per_strip {};
    Vector<u32> strip_bytes_count {};
};

}
