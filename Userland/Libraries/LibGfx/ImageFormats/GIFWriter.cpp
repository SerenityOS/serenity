/*
 * Copyright (c) 2024, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <LibCompress/Lzw.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/GIFWriter.h>

namespace Gfx {

namespace {

ErrorOr<void> write_header(Stream& stream)
{
    // 17. Header
    TRY(stream.write_until_depleted("GIF87a"sv));
    return {};
}

ErrorOr<void> write_logical_descriptor(BigEndianOutputBitStream& stream, Bitmap const& bitmap)
{
    // 18. Logical Screen Descriptor

    if (bitmap.width() > NumericLimits<u16>::max() || bitmap.height() > NumericLimits<u16>::max())
        return Error::from_string_literal("Bitmap size is too big for a GIF");

    TRY(stream.write_value<u16>(bitmap.width()));
    TRY(stream.write_value<u16>(bitmap.height()));

    // Global Color Table Flag
    TRY(stream.write_bits(true, 1));
    // Color Resolution
    TRY(stream.write_bits(6u, 3));
    // Sort Flag
    TRY(stream.write_bits(false, 1));
    // Size of Global Color Table
    TRY(stream.write_bits(7u, 3));

    // Background Color Index
    TRY(stream.write_value<u8>(0));

    // Pixel Aspect Ratio
    // NOTE: We can write a zero as most decoders discard the value.
    TRY(stream.write_value<u8>(0));

    return {};
}

ErrorOr<void> write_global_color_table(Stream& stream)
{
    // 19. Global Color Table

    // FIXME: The color table should include color specific to the image
    for (u16 i = 0; i < 256; ++i) {
        TRY(stream.write_value<u8>(i));
        TRY(stream.write_value<u8>(i));
        TRY(stream.write_value<u8>(i));
    }
    return {};
}

ErrorOr<void> write_image_data(Stream& stream, Bitmap const& bitmap)
{
    // 22. Table Based Image Data
    auto const pixel_number = static_cast<u32>(bitmap.width() * bitmap.height());
    auto indexes = TRY(ByteBuffer::create_uninitialized(pixel_number));
    for (u32 i = 0; i < pixel_number; ++i) {
        auto const color = Color::from_argb(*(bitmap.begin() + i));
        if (color.red() != color.green() || color.green() != color.blue())
            return Error::from_string_literal("Non grayscale images are unsupported.");
        indexes[i] = Color::from_argb(*(bitmap.begin() + i)).red(); // Any channel is correct
    }

    constexpr u8 lzw_minimum_code_size = 8;
    auto const encoded = TRY(Compress::LzwCompressor::compress_all(move(indexes), lzw_minimum_code_size));

    auto const number_of_subblocks = ceil_div(encoded.size(), 255ul);

    TRY(stream.write_value<u8>(lzw_minimum_code_size));

    for (u32 i = 0; i < number_of_subblocks; ++i) {
        auto const offset = i * 255;
        auto const to_write = min(255, encoded.size() - offset);
        TRY(stream.write_value<u8>(to_write));
        TRY(stream.write_until_depleted(encoded.bytes().slice(offset, to_write)));
    }

    // Block terminator
    TRY(stream.write_value<u8>(0));

    return {};
}

ErrorOr<void> write_image_descriptor(BigEndianOutputBitStream& stream, Bitmap const& bitmap)
{
    // 20. Image Descriptor

    // Image Separator
    TRY(stream.write_value<u8>(0x2c));
    // Image Left Position
    TRY(stream.write_value<u16>(0));
    // Image Top Position
    TRY(stream.write_value<u16>(0));
    // Image Width
    TRY(stream.write_value<u16>(bitmap.width()));
    // Image Height
    TRY(stream.write_value<u16>(bitmap.height()));

    // Local Color Table Flag
    TRY(stream.write_bits(false, 1));
    // Interlace Flag
    TRY(stream.write_bits(false, 1));
    // Sort Flag
    TRY(stream.write_bits(false, 1));
    // Reserved
    TRY(stream.write_bits(0u, 2));
    // Size of Local Color Table
    TRY(stream.write_bits(0u, 3));
    return {};
}

ErrorOr<void> write_trailer(Stream& stream)
{
    TRY(stream.write_value<u8>(0x3B));
    return {};
}

}

ErrorOr<void> GIFWriter::encode(Stream& stream, Bitmap const& bitmap)
{
    TRY(write_header(stream));

    BigEndianOutputBitStream bit_stream { MaybeOwned<Stream> { stream } };
    TRY(write_logical_descriptor(bit_stream, bitmap));
    TRY(write_global_color_table(bit_stream));

    // Write a Table-Based Image
    TRY(write_image_descriptor(bit_stream, bitmap));
    TRY(write_image_data(stream, bitmap));

    TRY(write_trailer(bit_stream));

    return {};
}

}
