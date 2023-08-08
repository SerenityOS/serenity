/*
 * Copyright (c) 2021, Pierre Hoffmeister
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Concepts.h>
#include <AK/FixedArray.h>
#include <AK/SIMDExtras.h>
#include <AK/String.h>
#include <LibCompress/Zlib.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/PNGWriter.h>

#pragma GCC diagnostic ignored "-Wpsabi"

namespace Gfx {

class PNGChunk {
    using data_length_type = u32;

public:
    explicit PNGChunk(String);
    auto const& data() const { return m_data; }
    String const& type() const { return m_type; }
    ErrorOr<void> reserve(size_t bytes) { return m_data.try_ensure_capacity(bytes); }

    template<typename T>
    ErrorOr<void> add_as_big_endian(T);

    ErrorOr<void> add_u8(u8);

    ErrorOr<void> compress_and_add(ReadonlyBytes);
    ErrorOr<void> add(ReadonlyBytes);

    ErrorOr<void> store_type();
    void store_data_length();
    u32 crc();

private:
    ByteBuffer m_data;
    String m_type;
};

PNGChunk::PNGChunk(String type)
    : m_type(move(type))
{
    VERIFY(m_type.bytes().size() == 4);

    // NOTE: These are MUST() because they should always be able to fit in m_data's inline capacity.
    MUST(add_as_big_endian<data_length_type>(0));
    MUST(store_type());
}

ErrorOr<void> PNGChunk::store_type()
{
    TRY(add(type().bytes()));
    return {};
}

void PNGChunk::store_data_length()
{
    auto data_length = BigEndian<u32>(m_data.size() - sizeof(data_length_type) - m_type.bytes().size());
    __builtin_memcpy(m_data.offset_pointer(0), &data_length, sizeof(u32));
}

u32 PNGChunk::crc()
{
    u32 crc = Crypto::Checksum::CRC32({ m_data.offset_pointer(sizeof(data_length_type)), m_data.size() - sizeof(data_length_type) }).digest();
    return crc;
}

ErrorOr<void> PNGChunk::compress_and_add(ReadonlyBytes uncompressed_bytes)
{
    return add(TRY(Compress::ZlibCompressor::compress_all(uncompressed_bytes, Compress::ZlibCompressionLevel::Best)));
}

ErrorOr<void> PNGChunk::add(ReadonlyBytes bytes)
{
    TRY(m_data.try_append(bytes));
    return {};
}

template<typename T>
ErrorOr<void> PNGChunk::add_as_big_endian(T data)
{
    auto data_out = AK::convert_between_host_and_big_endian(data);
    TRY(m_data.try_append(&data_out, sizeof(T)));
    return {};
}

ErrorOr<void> PNGChunk::add_u8(u8 data)
{
    TRY(m_data.try_append(data));
    return {};
}

ErrorOr<void> PNGWriter::add_chunk(PNGChunk& png_chunk)
{
    png_chunk.store_data_length();
    u32 crc = png_chunk.crc();
    TRY(png_chunk.add_as_big_endian(crc));
    TRY(m_data.try_append(png_chunk.data().data(), png_chunk.data().size()));
    return {};
}

ErrorOr<void> PNGWriter::add_png_header()
{
    TRY(m_data.try_append(PNG::header.data(), PNG::header.size()));
    return {};
}

ErrorOr<void> PNGWriter::add_IHDR_chunk(u32 width, u32 height, u8 bit_depth, PNG::ColorType color_type, u8 compression_method, u8 filter_method, u8 interlace_method)
{
    PNGChunk png_chunk { "IHDR"_string };
    TRY(png_chunk.add_as_big_endian(width));
    TRY(png_chunk.add_as_big_endian(height));
    TRY(png_chunk.add_u8(bit_depth));
    TRY(png_chunk.add_u8(to_underlying(color_type)));
    TRY(png_chunk.add_u8(compression_method));
    TRY(png_chunk.add_u8(filter_method));
    TRY(png_chunk.add_u8(interlace_method));
    TRY(add_chunk(png_chunk));
    return {};
}

ErrorOr<void> PNGWriter::add_iCCP_chunk(ReadonlyBytes icc_data)
{
    // https://www.w3.org/TR/png/#11iCCP
    PNGChunk chunk { "iCCP"_string };

    TRY(chunk.add("embedded profile"sv.bytes()));
    TRY(chunk.add_u8(0)); // \0-terminate profile name

    TRY(chunk.add_u8(0)); // compression method deflate
    TRY(chunk.compress_and_add(icc_data));

    TRY(add_chunk(chunk));
    return {};
}

ErrorOr<void> PNGWriter::add_IEND_chunk()
{
    PNGChunk png_chunk { "IEND"_string };
    TRY(add_chunk(png_chunk));
    return {};
}

union [[gnu::packed]] Pixel {
    ARGB32 rgba { 0 };
    struct {
        u8 red;
        u8 green;
        u8 blue;
        u8 alpha;
    };
    AK::SIMD::u8x4 simd;

    ALWAYS_INLINE static AK::SIMD::u8x4 gfx_to_png(Pixel pixel)
    {
        swap(pixel.red, pixel.blue);
        return pixel.simd;
    }
};
static_assert(AssertSize<Pixel, 4>());

ErrorOr<void> PNGWriter::add_IDAT_chunk(Gfx::Bitmap const& bitmap)
{
    PNGChunk png_chunk { "IDAT"_string };
    TRY(png_chunk.reserve(bitmap.size_in_bytes()));

    ByteBuffer uncompressed_block_data;
    TRY(uncompressed_block_data.try_ensure_capacity(bitmap.size_in_bytes() + bitmap.height()));

    auto dummy_scanline = TRY(FixedArray<Pixel>::create(bitmap.width()));
    auto const* scanline_minus_1 = dummy_scanline.data();

    for (int y = 0; y < bitmap.height(); ++y) {
        auto* scanline = reinterpret_cast<Pixel const*>(bitmap.scanline(y));

        struct Filter {
            PNG::FilterType type;
            ByteBuffer buffer {};
            int sum = 0;

            ErrorOr<void> append(u8 byte)
            {
                TRY(buffer.try_append(byte));
                sum += static_cast<i8>(byte);
                return {};
            }

            ErrorOr<void> append(AK::SIMD::u8x4 simd)
            {
                TRY(append(simd[0]));
                TRY(append(simd[1]));
                TRY(append(simd[2]));
                TRY(append(simd[3]));
                return {};
            }
        };

        Filter none_filter { .type = PNG::FilterType::None };
        TRY(none_filter.buffer.try_ensure_capacity(sizeof(Pixel) * bitmap.width()));

        Filter sub_filter { .type = PNG::FilterType::Sub };
        TRY(sub_filter.buffer.try_ensure_capacity(sizeof(Pixel) * bitmap.width()));

        Filter up_filter { .type = PNG::FilterType::Up };
        TRY(up_filter.buffer.try_ensure_capacity(sizeof(Pixel) * bitmap.width()));

        Filter average_filter { .type = PNG::FilterType::Average };
        TRY(average_filter.buffer.try_ensure_capacity(sizeof(ARGB32) * bitmap.width()));

        Filter paeth_filter { .type = PNG::FilterType::Paeth };
        TRY(paeth_filter.buffer.try_ensure_capacity(sizeof(ARGB32) * bitmap.width()));

        auto pixel_x_minus_1 = Pixel::gfx_to_png(dummy_scanline[0]);
        auto pixel_xy_minus_1 = Pixel::gfx_to_png(dummy_scanline[0]);

        for (int x = 0; x < bitmap.width(); ++x) {
            auto pixel = Pixel::gfx_to_png(scanline[x]);
            auto pixel_y_minus_1 = Pixel::gfx_to_png(scanline_minus_1[x]);

            TRY(none_filter.append(pixel));

            TRY(sub_filter.append(pixel - pixel_x_minus_1));

            TRY(up_filter.append(pixel - pixel_y_minus_1));

            // The sum Orig(a) + Orig(b) shall be performed without overflow (using at least nine-bit arithmetic).
            auto sum = AK::SIMD::to_u16x4(pixel_x_minus_1) + AK::SIMD::to_u16x4(pixel_y_minus_1);
            auto average = AK::SIMD::to_u8x4(sum / 2);
            TRY(average_filter.append(pixel - average));

            TRY(paeth_filter.append(pixel - PNG::paeth_predictor(pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1)));

            pixel_x_minus_1 = pixel;
            pixel_xy_minus_1 = pixel_y_minus_1;
        }

        scanline_minus_1 = scanline;

        // 12.8 Filter selection: https://www.w3.org/TR/PNG/#12Filter-selection
        // For best compression of truecolour and greyscale images, the recommended approach
        // is adaptive filtering in which a filter is chosen for each scanline.
        // The following simple heuristic has performed well in early tests:
        // compute the output scanline using all five filters, and select the filter that gives the smallest sum of absolute values of outputs.
        // (Consider the output bytes as signed differences for this test.)
        Filter& best_filter = none_filter;
        if (abs(best_filter.sum) > abs(sub_filter.sum))
            best_filter = sub_filter;
        if (abs(best_filter.sum) > abs(up_filter.sum))
            best_filter = up_filter;
        if (abs(best_filter.sum) > abs(average_filter.sum))
            best_filter = average_filter;
        if (abs(best_filter.sum) > abs(paeth_filter.sum))
            best_filter = paeth_filter;

        TRY(uncompressed_block_data.try_append(to_underlying(best_filter.type)));
        TRY(uncompressed_block_data.try_append(best_filter.buffer));
    }

    TRY(png_chunk.compress_and_add(uncompressed_block_data));
    TRY(add_chunk(png_chunk));
    return {};
}

ErrorOr<ByteBuffer> PNGWriter::encode(Gfx::Bitmap const& bitmap, Options options)
{
    PNGWriter writer;
    TRY(writer.add_png_header());
    TRY(writer.add_IHDR_chunk(bitmap.width(), bitmap.height(), 8, PNG::ColorType::TruecolorWithAlpha, 0, 0, 0));
    if (options.icc_data.has_value())
        TRY(writer.add_iCCP_chunk(options.icc_data.value()));
    TRY(writer.add_IDAT_chunk(bitmap));
    TRY(writer.add_IEND_chunk());
    return ByteBuffer::copy(writer.m_data);
}

}
