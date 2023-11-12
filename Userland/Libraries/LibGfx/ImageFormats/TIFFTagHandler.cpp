/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/String.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>

namespace Gfx::TIFF {

ErrorOr<void> handle_tag(Metadata& metadata, u16 tag, Type type, u32 count, Vector<Value> const& value)
{
    // FIXME: Make that easy to extend
    switch (tag) {
    case 256:
        // ImageWidth
        if ((type != Type::UnsignedShort && type != Type::UnsignedLong) || count != 1)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 256");

        value[0].visit(
            [&metadata]<OneOf<u16, u32> T>(T const& width) {
                metadata.size.set_width(width);
            },
            [&](auto const&) {
                VERIFY_NOT_REACHED();
            });
        break;

    case 257:
        // ImageLength
        if ((type != Type::UnsignedShort && type != Type::UnsignedLong) || count != 1)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 257");

        value[0].visit(
            [&metadata]<OneOf<u16, u32> T>(T const& width) {
                metadata.size.set_height(width);
            },
            [&](auto const&) {
                VERIFY_NOT_REACHED();
            });
        break;

    case 258:
        // BitsPerSample
        if (type != Type::UnsignedShort || count != 3)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 258");

        for (u8 i = 0; i < metadata.bits_per_sample.size(); ++i) {
            value[i].visit(
                [&metadata, i](u16 const& bits_per_sample) {
                    metadata.bits_per_sample[i] = bits_per_sample;
                },
                [&](auto const&) {
                    VERIFY_NOT_REACHED();
                });
        }
        break;

    case 259:
        // Compression
        if (type != Type::UnsignedShort || count != 1)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 259");

        TRY(value[0].visit(
            [&metadata](u16 const& compression) -> ErrorOr<void> {
                if (compression > 6 && compression != to_underlying(Compression::PackBits))
                    return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid compression value");

                metadata.compression = static_cast<Compression>(compression);
                return {};
            },
            [&](auto const&) -> ErrorOr<void> {
                VERIFY_NOT_REACHED();
            }));
        break;

    case 273:
        // StripOffsets
        if (type != Type::UnsignedShort && type != Type::UnsignedLong)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 273");

        TRY(metadata.strip_offsets.try_ensure_capacity(count));
        for (u32 i = 0; i < count; ++i) {
            value[i].visit(
                [&metadata]<OneOf<u16, u32> T>(T const& offset) {
                    metadata.strip_offsets.append(offset);
                },
                [&](auto const&) {
                    VERIFY_NOT_REACHED();
                });
        }
        break;

    case 277:
        // SamplesPerPixel
        if (type != Type::UnsignedShort || count != 1)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 277");
        TRY(value[0].visit(
            [](u16 const& samples_per_pixels) -> ErrorOr<void> {
                if (samples_per_pixels != 3)
                    return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 277");
                return {};
            },
            [&](auto const&) -> ErrorOr<void> {
                VERIFY_NOT_REACHED();
            }));
        break;

    case 278:
        // RowsPerStrip
        if ((type != Type::UnsignedShort && type != Type::UnsignedLong) || count != 1)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 278");

        value[0].visit(
            [&metadata]<OneOf<u16, u32> T>(T const& rows_per_strip) {
                metadata.rows_per_strip = rows_per_strip;
            },
            [&](auto const&) {
                VERIFY_NOT_REACHED();
            });
        break;

    case 279:
        // StripByteCounts
        if (type != Type::UnsignedShort && type != Type::UnsignedLong)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 279");

        TRY(metadata.strip_bytes_count.try_ensure_capacity(count));
        for (u32 i = 0; i < count; ++i) {
            value[i].visit(
                [&metadata]<OneOf<u16, u32> T>(T const& offset) {
                    metadata.strip_bytes_count.append(offset);
                },
                [&](auto const&) {
                    VERIFY_NOT_REACHED();
                });
        }
        break;
    case 317:
        // Predictor
        if (type != Type::UnsignedShort || count != 1)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 317");

        TRY(value[0].visit(
            [&metadata](u16 const& predictor) -> ErrorOr<void> {
                if (predictor != 1 && predictor != 2)
                    return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid predictor value");

                metadata.predictor = static_cast<Predictor>(predictor);
                return {};
            },
            [&](auto const&) -> ErrorOr<void> {
                VERIFY_NOT_REACHED();
            }));
        break;
    default:
        dbgln_if(TIFF_DEBUG, "Unknown tag: {}", tag);
    }

    return {};
}

}
