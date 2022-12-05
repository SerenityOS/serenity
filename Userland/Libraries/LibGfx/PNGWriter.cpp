/*
 * Copyright (c) 2021, Pierre Hoffmeister
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Concepts.h>
#include <AK/DeprecatedString.h>
#include <AK/SIMDExtras.h>
#include <LibCompress/Zlib.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGWriter.h>

#pragma GCC diagnostic ignored "-Wpsabi"

namespace Gfx {

class PNGChunk {
    using data_length_type = u32;

public:
    explicit PNGChunk(DeprecatedString);
    auto const& data() const { return m_data; };
    DeprecatedString const& type() const { return m_type; };
    void reserve(size_t bytes) { m_data.ensure_capacity(bytes); }

    template<typename T>
    void add_as_big_endian(T);

    template<typename T>
    void add_as_little_endian(T);

    void add_u8(u8);

    template<typename T>
    void add(T*, size_t);

    void store_type();
    void store_data_length();
    u32 crc();

private:
    template<typename T>
    requires(IsUnsigned<T>) void add(T);

    ByteBuffer m_data;
    DeprecatedString m_type;
};

PNGChunk::PNGChunk(DeprecatedString type)
    : m_type(move(type))
{
    add<data_length_type>(0);
    store_type();
}

void PNGChunk::store_type()
{
    m_data.append(type().bytes());
}

void PNGChunk::store_data_length()
{
    auto data_length = BigEndian<u32>(m_data.size() - sizeof(data_length_type) - m_type.length());
    __builtin_memcpy(m_data.offset_pointer(0), &data_length, sizeof(u32));
}

u32 PNGChunk::crc()
{
    u32 crc = Crypto::Checksum::CRC32({ m_data.offset_pointer(sizeof(data_length_type)), m_data.size() - sizeof(data_length_type) }).digest();
    return crc;
}

template<typename T>
requires(IsUnsigned<T>) void PNGChunk::add(T data)
{
    m_data.append(&data, sizeof(T));
}

template<typename T>
void PNGChunk::add(T* data, size_t size)
{
    m_data.append(data, size);
}

template<typename T>
void PNGChunk::add_as_little_endian(T data)
{
    auto data_out = AK::convert_between_host_and_little_endian(data);
    add(data_out);
}

template<typename T>
void PNGChunk::add_as_big_endian(T data)
{
    auto data_out = AK::convert_between_host_and_big_endian(data);
    add(data_out);
}

void PNGChunk::add_u8(u8 data)
{
    add(data);
}

void PNGWriter::add_chunk(PNGChunk& png_chunk)
{
    png_chunk.store_data_length();
    u32 crc = png_chunk.crc();
    png_chunk.add_as_big_endian(crc);
    m_data.append(png_chunk.data().data(), png_chunk.data().size());
}

void PNGWriter::add_png_header()
{
    m_data.append(PNG::header.data(), PNG::header.size());
}

void PNGWriter::add_IHDR_chunk(u32 width, u32 height, u8 bit_depth, PNG::ColorType color_type, u8 compression_method, u8 filter_method, u8 interlace_method)
{
    PNGChunk png_chunk { "IHDR" };
    png_chunk.add_as_big_endian(width);
    png_chunk.add_as_big_endian(height);
    png_chunk.add_u8(bit_depth);
    png_chunk.add_u8(to_underlying(color_type));
    png_chunk.add_u8(compression_method);
    png_chunk.add_u8(filter_method);
    png_chunk.add_u8(interlace_method);
    add_chunk(png_chunk);
}

void PNGWriter::add_IEND_chunk()
{
    PNGChunk png_chunk { "IEND" };
    add_chunk(png_chunk);
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

void PNGWriter::add_IDAT_chunk(Gfx::Bitmap const& bitmap)
{
    PNGChunk png_chunk { "IDAT" };
    png_chunk.reserve(bitmap.size_in_bytes());

    ByteBuffer uncompressed_block_data;
    uncompressed_block_data.ensure_capacity(bitmap.size_in_bytes() + bitmap.height());

    Pixel dummy_scanline[bitmap.width()];
    auto const* scanline_minus_1 = dummy_scanline;

    for (int y = 0; y < bitmap.height(); ++y) {
        auto* scanline = reinterpret_cast<Pixel const*>(bitmap.scanline(y));

        struct Filter {
            PNG::FilterType type;
            ByteBuffer buffer {};
            int sum = 0;

            void append(u8 byte)
            {
                buffer.append(byte);
                sum += static_cast<i8>(byte);
            }

            void append(AK::SIMD::u8x4 simd)
            {
                append(simd[0]);
                append(simd[1]);
                append(simd[2]);
                append(simd[3]);
            }
        };

        Filter none_filter { .type = PNG::FilterType::None };
        none_filter.buffer.ensure_capacity(sizeof(Pixel) * bitmap.height());

        Filter sub_filter { .type = PNG::FilterType::Sub };
        sub_filter.buffer.ensure_capacity(sizeof(Pixel) * bitmap.height());

        Filter up_filter { .type = PNG::FilterType::Up };
        up_filter.buffer.ensure_capacity(sizeof(Pixel) * bitmap.height());

        Filter average_filter { .type = PNG::FilterType::Average };
        average_filter.buffer.ensure_capacity(sizeof(ARGB32) * bitmap.height());

        Filter paeth_filter { .type = PNG::FilterType::Paeth };
        paeth_filter.buffer.ensure_capacity(sizeof(ARGB32) * bitmap.height());

        auto pixel_x_minus_1 = Pixel::gfx_to_png(*dummy_scanline);
        auto pixel_xy_minus_1 = Pixel::gfx_to_png(*dummy_scanline);

        for (int x = 0; x < bitmap.width(); ++x) {
            auto pixel = Pixel::gfx_to_png(scanline[x]);
            auto pixel_y_minus_1 = Pixel::gfx_to_png(scanline_minus_1[x]);

            none_filter.append(pixel);

            sub_filter.append(pixel - pixel_x_minus_1);

            up_filter.append(pixel - pixel_y_minus_1);

            // The sum Orig(a) + Orig(b) shall be performed without overflow (using at least nine-bit arithmetic).
            auto sum = AK::SIMD::to_u16x4(pixel_x_minus_1) + AK::SIMD::to_u16x4(pixel_y_minus_1);
            auto average = AK::SIMD::to_u8x4(sum / 2);
            average_filter.append(pixel - average);

            paeth_filter.append(pixel - PNG::paeth_predictor(pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1));

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

        uncompressed_block_data.append(to_underlying(best_filter.type));
        uncompressed_block_data.append(best_filter.buffer);
    }

    auto maybe_zlib_buffer = Compress::ZlibCompressor::compress_all(uncompressed_block_data, Compress::ZlibCompressionLevel::Best);
    if (!maybe_zlib_buffer.has_value()) {
        // FIXME: Handle errors.
        VERIFY_NOT_REACHED();
    }
    auto zlib_buffer = maybe_zlib_buffer.release_value();

    png_chunk.add(zlib_buffer.data(), zlib_buffer.size());
    add_chunk(png_chunk);
}

ErrorOr<ByteBuffer> PNGWriter::encode(Gfx::Bitmap const& bitmap)
{
    PNGWriter writer;
    writer.add_png_header();
    writer.add_IHDR_chunk(bitmap.width(), bitmap.height(), 8, PNG::ColorType::TruecolorWithAlpha, 0, 0, 0);
    writer.add_IDAT_chunk(bitmap);
    writer.add_IEND_chunk();
    return ByteBuffer::copy(writer.m_data);
}

}
