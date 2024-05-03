/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Container: https://developers.google.com/speed/webp/docs/riff_container
// Lossless format: https://developers.google.com/speed/webp/docs/webp_lossless_bitstream_specification

#include <AK/BitStream.h>
#include <LibCompress/DeflateTables.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/WebPWriter.h>
#include <LibRIFF/RIFF.h>

namespace Gfx {

// https://developers.google.com/speed/webp/docs/riff_container#webp_file_header
static ErrorOr<void> write_webp_header(Stream& stream, unsigned data_size)
{
    TRY(stream.write_until_depleted("RIFF"sv));
    TRY(stream.write_value<LittleEndian<u32>>(4 + data_size)); // Including size of "WEBP" and the data size itself.
    TRY(stream.write_until_depleted("WEBP"sv));
    return {};
}

static ErrorOr<void> write_chunk_header(Stream& stream, StringView chunk_fourcc, unsigned vp8l_data_size)
{
    TRY(stream.write_until_depleted(chunk_fourcc));
    TRY(stream.write_value<LittleEndian<u32>>(vp8l_data_size));
    return {};
}

// https://developers.google.com/speed/webp/docs/riff_container#simple_file_format_lossless
// https://developers.google.com/speed/webp/docs/webp_lossless_bitstream_specification#7_overall_structure_of_the_format
static ErrorOr<void> write_VP8L_header(Stream& stream, unsigned width, unsigned height, bool alpha_hint)
{
    // "The 14-bit precision for image width and height limits the maximum size of a WebP lossless image to 16384✕16384 pixels."
    if (width > 16384 || height > 16384)
        return Error::from_string_literal("WebP lossless images can't be larger than 16384x16384 pixels");

    if (width == 0 || height == 0)
        return Error::from_string_literal("WebP lossless images must be at least one pixel wide and tall");

    LittleEndianOutputBitStream bit_stream { MaybeOwned<Stream>(stream) };

    // Signature byte.
    TRY(bit_stream.write_bits(0x2fu, 8u)); // Signature byte

    // 14 bits width-1, 14 bits height-1, 1 bit alpha hint, 3 bit version_number.
    TRY(bit_stream.write_bits(width - 1, 14u));
    TRY(bit_stream.write_bits(height - 1, 14u));

    // "The alpha_is_used bit is a hint only, and should not impact decoding.
    //  It should be set to 0 when all alpha values are 255 in the picture, and 1 otherwise."
    TRY(bit_stream.write_bits(alpha_hint, 1u));

    // "The version_number is a 3 bit code that must be set to 0."
    TRY(bit_stream.write_bits(0u, 3u));

    // FIXME: Make ~LittleEndianOutputBitStream do this, or make it VERIFY() that it has happened at least.
    TRY(bit_stream.flush_buffer_to_stream());

    return {};
}

static ErrorOr<void> write_VP8L_image_data(Stream& stream, Bitmap const& bitmap)
{
    LittleEndianOutputBitStream bit_stream { MaybeOwned<Stream>(stream) };

    // optional-transform   =  (%b1 transform optional-transform) / %b0
    TRY(bit_stream.write_bits(0u, 1u)); // No transform for now.

    // https://developers.google.com/speed/webp/docs/webp_lossless_bitstream_specification#5_image_data
    // spatially-coded-image =  color-cache-info meta-prefix data

    // color-cache-info      =  %b0
    // color-cache-info      =/ (%b1 4BIT) ; 1 followed by color cache size
    TRY(bit_stream.write_bits(0u, 1u)); // No color cache for now.

    // meta-prefix           =  %b0 / (%b1 entropy-image)
    TRY(bit_stream.write_bits(0u, 1u)); // No meta prefix for now.

    // data                  =  prefix-codes lz77-coded-image
    // prefix-codes          =  prefix-code-group *prefix-codes
    // prefix-code-group     =
    //     5prefix-code ; See "Interpretation of Meta Prefix Codes" to
    //                  ; understand what each of these five prefix
    //                  ; codes are for.

    // We're writing a single prefix-code-group.
    // "These codes are (in bitstream order):

    //  Prefix code #1: Used for green channel, backward-reference length, and color cache.
    //  Prefix code #2, #3, and #4: Used for red, blue, and alpha channels, respectively.
    //  Prefix code #5: Used for backward-reference distance."

    // We use neither back-references not color cache entries yet.
    // We write prefix trees for 256 literals all of length 8, which means each byte is encoded as itself.
    // That doesn't give any compression, but is a valid bit stream.
    // We can make this smarter later on.

    size_t const color_cache_size = 0;
    constexpr Array alphabet_sizes = to_array<size_t>({ 256 + 24 + static_cast<size_t>(color_cache_size), 256, 256, 256, 40 }); // XXX Shared?

    // If you add support for color cache: At the moment, CanonicalCodes does not support writing more than 288 symbols.
    if (alphabet_sizes[0] > 288)
        return Error::from_string_literal("Invalid alphabet size");

    for (int i = 0; i < 4; ++i) {
        TRY(bit_stream.write_bits(0u, 1u)); // Normal code length code.

        // Write code length codes.
        constexpr int kCodeLengthCodes = 19;
        Array<int, kCodeLengthCodes> kCodeLengthCodeOrder = { 17, 18, 0, 1, 2, 3, 4, 5, 16, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        int num_code_lengths = max(4u, find_index(kCodeLengthCodeOrder.begin(), kCodeLengthCodeOrder.end(), 8) + 1);

        // "int num_code_lengths = 4 + ReadBits(4);"
        TRY(bit_stream.write_bits(num_code_lengths - 4u, 4u));

        for (int i = 0; i < num_code_lengths - 1; ++i)
            TRY(bit_stream.write_bits(0u, 3u));
        TRY(bit_stream.write_bits(1u, 3u));

        // Write code lengths.
        if (alphabet_sizes[i] == 256) {
            TRY(bit_stream.write_bits(0u, 1u)); // max_symbol is alphabet_size
        } else {
            TRY(bit_stream.write_bits(1u, 1u)); // max_symbol is explicitly coded
            // "int length_nbits = 2 + 2 * ReadBits(3);
            //  int max_symbol = 2 + ReadBits(length_nbits);"
            TRY(bit_stream.write_bits(3u, 3u));   // length_nbits = 2 + 2 * 3
            TRY(bit_stream.write_bits(254u, 8u)); // max_symbol = 2 + 254
        }

        // The code length codes only contain a single entry for '8'. WebP streams with a single element store 0 bits per element.
        // (This is different from deflate, which needs 1 bit per element.)
    }

    // For code #5, use a simple empty code, since we don't use this yet.
    TRY(bit_stream.write_bits(1u, 1u)); // Simple code length code.
    TRY(bit_stream.write_bits(0u, 1u)); // num_symbols - 1
    TRY(bit_stream.write_bits(0u, 1u)); // is_first_8bits
    TRY(bit_stream.write_bits(0u, 1u)); // symbol0

    // Image data.
    for (ARGB32 pixel : bitmap) {
        u8 a = pixel >> 24;
        u8 r = pixel >> 16;
        u8 g = pixel >> 8;
        u8 b = pixel;

        // We wrote a huffman table that gives every symbol 8 bits. That means we can write the image data
        // out uncompressed –- but we do need to reverse the bit order of the bytes.
        TRY(bit_stream.write_bits(Compress::reverse8_lookup_table[g], 8u));
        TRY(bit_stream.write_bits(Compress::reverse8_lookup_table[r], 8u));
        TRY(bit_stream.write_bits(Compress::reverse8_lookup_table[b], 8u));
        TRY(bit_stream.write_bits(Compress::reverse8_lookup_table[a], 8u));
    }

    // FIXME: Make ~LittleEndianOutputBitStream do this, or make it VERIFY() that it has happened at least.
    TRY(bit_stream.align_to_byte_boundary());
    TRY(bit_stream.flush_buffer_to_stream());

    return {};
}

// FIXME: Consider using LibRIFF for RIFF writing details. (It currently has no writing support.)
static ErrorOr<void> align_to_two(AllocatingMemoryStream& stream)
{
    // https://developers.google.com/speed/webp/docs/riff_container
    // "If Chunk Size is odd, a single padding byte -- which MUST be 0 to conform with RIFF -- is added."
    if (stream.used_buffer_size() % 2 != 0)
        TRY(stream.write_value<u8>(0));
    return {};
}

ErrorOr<void> WebPWriter::encode(Stream& stream, Bitmap const& bitmap, Options const& options)
{
    // The chunk headers need to know their size, so we either need a SeekableStream or need to buffer the data. We're doing the latter.
    // FIXME: The whole writing-and-reading-into-buffer over-and-over is awkward and inefficient.
    AllocatingMemoryStream vp8l_header_stream;
    TRY(write_VP8L_header(vp8l_header_stream, bitmap.width(), bitmap.height(), true));
    auto vp8l_header_bytes = TRY(vp8l_header_stream.read_until_eof());

    AllocatingMemoryStream vp8l_data_stream;
    TRY(write_VP8L_image_data(vp8l_data_stream, bitmap));
    auto vp8l_data_bytes = TRY(vp8l_data_stream.read_until_eof());

    AllocatingMemoryStream vp8l_chunk_stream;
    TRY(write_chunk_header(vp8l_chunk_stream, "VP8L"sv, vp8l_header_bytes.size() + vp8l_data_bytes.size()));
    TRY(vp8l_chunk_stream.write_until_depleted(vp8l_header_bytes));
    TRY(vp8l_chunk_stream.write_until_depleted(vp8l_data_bytes));
    TRY(align_to_two(vp8l_chunk_stream));
    auto vp8l_chunk_bytes = TRY(vp8l_chunk_stream.read_until_eof());

    if (options.icc_data.has_value()) {
        // FIXME: Write VP8X, ICCP chunks.
        dbgln("WebPWriter: Can't write ICC data to webp files yet, dropping it for now");
    }

    TRY(write_webp_header(stream, vp8l_chunk_bytes.size()));
    TRY(stream.write_until_depleted(vp8l_chunk_bytes));
    return {};
}

}
