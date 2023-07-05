/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ExifReader.h"
#include <AK/ScopeGuard.h>

#define EXIF_DEBUG 0

namespace Gfx {

ErrorOr<ExifMetadata> ExifReader::read(FixedMemoryStream& stream)
{
    ExifReader reader(stream);

    auto maybe_idf = TRY(reader.read_header());

    while (maybe_idf.has_value())
        maybe_idf = TRY(reader.read_idf(maybe_idf.value()));

    return reader.m_metadata;
}

ErrorOr<Optional<u32>> ExifReader::read_next_idf_offset()
{
    auto const next_block_position = TRY(read_value<u32>());

    if (next_block_position != 0)
        return Optional<u32> { next_block_position };

    return OptionalNone {};
}

ErrorOr<Optional<u32>> ExifReader::read_header()
{
    // Table 1. TIFF Headers
    auto const order = TRY(m_stream.read_value<u16>());
    switch (order) {
    case 0x4949:
        m_byte_order = ByteOrder::LittleEndian;
        break;
    case 0x4D4D:
        m_byte_order = ByteOrder::BigEndian;
        break;
    default:
        return Error::from_string_literal("Invalid TIFF header");
    }

    if (TRY(read_value<u16>()) != 0x2A)
        return Error::from_string_literal("Invalid TIFF header");

    return read_next_idf_offset();
}

ErrorOr<Optional<u32>> ExifReader::read_idf(u32 idf_offset)
{
    // 4.6.2. - IFD Structure

    TRY(m_stream.seek(idf_offset));

    auto const number_of_field = TRY(read_value<u16>());

    for (u16 i = 0; i < number_of_field; ++i)
        TRY(read_tag());

    return TRY(read_next_idf_offset());
}

ErrorOr<ExifReader::Type> ExifReader::read_type()
{
    switch (TRY(read_value<u16>())) {
    case to_underlying(Type::Byte):
        return Type::Byte;
    case to_underlying(Type::ASCII):
        return Type::ASCII;
    case to_underlying(Type::Short):
        return Type::Short;
    case to_underlying(Type::Long):
        return Type::Long;
    case to_underlying(Type::Rational):
        return Type::Rational;
    case to_underlying(Type::Undefined):
        return Type::Undefined;
    case to_underlying(Type::SLong):
        return Type::SLong;
    case to_underlying(Type::SRational):
        return Type::SRational;
    case to_underlying(Type::UTF8):
        return Type::UTF8;
    default:
        VERIFY_NOT_REACHED();
    }
}

constexpr u8 ExifReader::size_of_type(Type type)
{
    switch (type) {
    case Type::Byte:
        return 1;
    case Type::ASCII:
        return 1;
    case Type::Short:
        return 2;
    case Type::Long:
        return 4;
    case Type::Rational:
        return 8;
    case Type::Undefined:
        return 1;
    case Type::SLong:
        return 4;
    case Type::SRational:
        return 8;
    case Type::UTF8:
        return 1;
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<ExifReader::ExifValue> ExifReader::read_exif_value(ExifReader::Type type, u32 count, u32 offset)
{
    auto const old_offset = TRY(m_stream.tell());
    ScopeGuard reset_offset { [this, old_offset]() { MUST(m_stream.seek(old_offset)); } };

    TRY(m_stream.seek(offset));

    switch (type) {
    case Type::Byte:
    case Type::Undefined:
        return ExifValue { TRY(read_value<u8>()) };
    case Type::ASCII:
    case Type::UTF8: {
        auto string_data = TRY(ByteBuffer::create_uninitialized(count));
        TRY(m_stream.read_until_filled(string_data));
        return ExifValue { TRY(String::from_utf8(StringView { string_data.bytes() })) };
    }
    case Type::Short:
        return ExifValue { TRY(read_value<u16>()) };
    case Type::Long:
        return ExifValue { TRY(read_value<u32>()) };
    case Type::Rational:
        return ExifValue { Rational<u32> { TRY(read_value<u32>()), TRY(read_value<u32>()) } };
    case Type::SLong:
        return ExifValue { TRY(read_value<i32>()) };
    case Type::SRational:
        return ExifValue { Rational<i32> { TRY(read_value<i32>()), TRY(read_value<i32>()) } };
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<void> ExifReader::read_tag()
{
    // 4.6.2. - IFD Structure
    auto const tag = TRY(read_value<u16>());
    auto const type = TRY(read_type());
    auto const count = TRY(read_value<u32>());

    auto const total_data_size = size_of_type(type) * count;

    if (type != Type::UTF8 && type != Type::ASCII && count != 1) {
        dbgln_if(EXIF_DEBUG, "Read tag({}), type({}), count({})", tag, to_underlying(type), count);
        TRY(m_stream.discard(4));
        return {};
    }

    auto const exif_value = TRY(([=, this]() -> ErrorOr<ExifValue> {
        if (total_data_size <= 4) {
            auto const value = TRY(read_exif_value(type, count, TRY(m_stream.tell())));
            TRY(m_stream.discard(4));
            return value;
        }
        auto const offset = TRY(read_value<u32>());
        return read_exif_value(type, count, offset);
    }()));

    exif_value.visit(
        [&](auto const& value) {
            dbgln_if(EXIF_DEBUG, "Read tag({}), type({}), count({}): {}", tag, to_underlying(type), count, value);
        });

    if (tag == 34665 || tag == 40965) {
        // 4.6.3.1.1. - Exif IFD Pointer
        // and
        // 4.6.3.3.1. - Interoperability IFD Pointer

        auto const current_offset = TRY(m_stream.tell());
        ScopeGuard reset_position {
            [this, current_offset]() { MUST(m_stream.seek(current_offset)); }
        };

        VERIFY(!TRY(read_idf(exif_value.get<u32>())).has_value());
    }

    return {};
}

} // Gfx
