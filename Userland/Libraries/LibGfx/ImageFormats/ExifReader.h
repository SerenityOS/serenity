/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <AK/String.h>

// This implements "Exchangeable image file format for digital still cameras: Exif Version 3.0"
// The spec can be found at https://www.cipa.jp/e/std/std-sec.html

namespace Gfx {

template<OneOf<u32, i32> x32>
struct Rational {
    x32 numerator;
    x32 denominator;
};

struct ExifMetadata {
    Optional<String> manufacturer {};
    Optional<String> model {};

    Optional<Rational<u32>> exposure {};
    Optional<Rational<u32>> fnumber {};

    Optional<u32> width {};
    Optional<u32> height {};
};

class ExifReader {
public:
    static ErrorOr<ExifMetadata> read(FixedMemoryStream& stream);

private:
    enum class ByteOrder {
        LittleEndian,
        BigEndian,
    };

    enum class Type {
        Byte = 1,
        ASCII = 2,
        Short = 3,
        Long = 4,
        Rational = 5,
        Undefined = 7,
        SLong = 9,
        SRational = 10,
        UTF8 = 129,
    };

    using ExifValue = Variant<u8, String, u16, u32, Rational<u32>, i32, Rational<i32>>;

    ExifReader(FixedMemoryStream& stream)
        : m_stream(stream)
    {
    }

    template<typename T>
    ErrorOr<T> read_value()
    {
        if (m_byte_order == ByteOrder::LittleEndian)
            return TRY(m_stream.read_value<LittleEndian<T>>());
        if (m_byte_order == ByteOrder::BigEndian)
            return TRY(m_stream.read_value<BigEndian<T>>());
        VERIFY_NOT_REACHED();
    }

    ErrorOr<Optional<u32>> read_header();
    ErrorOr<Optional<u32>> read_next_idf_offset();
    ErrorOr<Optional<u32>> read_idf(u32 idf_offset);
    ErrorOr<void> read_tag();
    ErrorOr<Type> read_type();
    ErrorOr<ExifValue> read_exif_value(Type type, u32 count, u32 offset);

    static constexpr u8 size_of_type(Type);

    FixedMemoryStream& m_stream;
    ExifMetadata m_metadata {};

    ByteOrder m_byte_order {};
};

} // Gfx

template<typename T>
struct AK::Formatter<Gfx::Rational<T>> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::Rational<T> value)
    {
        return Formatter<FormatString>::format(builder, "{} ({}/{})"sv, static_cast<double>(value.numerator) / value.denominator, value.numerator, value.denominator);
    }
};
