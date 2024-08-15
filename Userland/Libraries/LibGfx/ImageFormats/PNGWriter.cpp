/*
 * Copyright (c) 2021, Pierre Hoffmeister
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 * Copyright (c) 2024, Torben Jonas Virtmann
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Concepts.h>
#include <AK/FixedArray.h>
#include <AK/MemoryStream.h>
#include <AK/SIMDExtras.h>
#include <AK/String.h>
#include <LibCompress/Zlib.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/PNGWriter.h>

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

    ErrorOr<void> compress_and_add(ReadonlyBytes, Compress::ZlibCompressionLevel);
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

ErrorOr<void> PNGChunk::compress_and_add(ReadonlyBytes uncompressed_bytes, Compress::ZlibCompressionLevel compression_level)
{
    return add(TRY(Compress::ZlibCompressor::compress_all(uncompressed_bytes, compression_level)));
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

PNGWriter::PNGWriter(Stream& stream)
    : m_stream(stream)
{
}

ErrorOr<void> PNGWriter::add_chunk(PNGChunk& png_chunk)
{
    png_chunk.store_data_length();
    u32 crc = png_chunk.crc();
    TRY(png_chunk.add_as_big_endian(crc));
    TRY(m_stream.write_until_depleted(png_chunk.data()));
    return {};
}

ErrorOr<void> PNGWriter::add_png_header()
{
    TRY(m_stream.write_until_depleted(PNG::header));
    return {};
}

ErrorOr<void> PNGWriter::add_acTL_chunk(u32 num_frames, u32 loop_count)
{
    // https://www.w3.org/TR/png/#acTL-chunk
    PNGChunk png_chunk { "acTL"_string };
    TRY(png_chunk.add_as_big_endian(num_frames));
    TRY(png_chunk.add_as_big_endian(loop_count));
    TRY(add_chunk(png_chunk));
    return {};
}

struct fcTLData {
    u32 sequence_number { 0 };
    u32 width { 0 };
    u32 height { 0 };
    u32 x_offset { 0 };
    u32 y_offset { 0 };
    u16 delay_numerator { 0 };
    u16 delay_denominator { 1 };
    // dispose_op values
    // 0           APNG_DISPOSE_OP_NONE
    // 1           APNG_DISPOSE_OP_BACKGROUND
    // 2           APNG_DISPOSE_OP_PREVIOUS
    u8 dispose_operation { 0 };
    // blend_op values
    // value
    // 0       APNG_BLEND_OP_SOURCE
    // 1       APNG_BLEND_OP_OVER
    u8 blend_operation { 0 };
};

ErrorOr<void> PNGWriter::add_fcTL_chunk(fcTLData const& data)
{
    // https://www.w3.org/TR/png/#fcTL-chunk

    // TODO: Constraints on frame regions
    PNGChunk png_chunk { "fcTL"_string };
    TRY(png_chunk.add_as_big_endian(data.sequence_number));
    TRY(png_chunk.add_as_big_endian(data.width));
    TRY(png_chunk.add_as_big_endian(data.height));
    TRY(png_chunk.add_as_big_endian(data.x_offset));
    TRY(png_chunk.add_as_big_endian(data.y_offset));
    TRY(png_chunk.add_as_big_endian(data.delay_numerator));
    TRY(png_chunk.add_as_big_endian(data.delay_denominator));
    TRY(png_chunk.add_u8(data.dispose_operation));
    TRY(png_chunk.add_u8(data.blend_operation));
    TRY(add_chunk(png_chunk));
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

ErrorOr<void> PNGWriter::add_iCCP_chunk(ReadonlyBytes icc_data, Compress::ZlibCompressionLevel compression_level)
{
    // https://www.w3.org/TR/png/#11iCCP
    PNGChunk chunk { "iCCP"_string };

    TRY(chunk.add("embedded profile"sv.bytes()));
    TRY(chunk.add_u8(0)); // \0-terminate profile name

    TRY(chunk.add_u8(0)); // compression method deflate
    TRY(chunk.compress_and_add(icc_data, compression_level));

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
    AK::SIMD::u8x4 simd;

    ALWAYS_INLINE static AK::SIMD::u8x4 argb32_to_simd(Pixel pixel)
    {
        return pixel.simd;
    }
};
static_assert(AssertSize<Pixel, 4>());

template<bool include_alpha>
static ErrorOr<void> add_image_data_to_chunk(Gfx::Bitmap const& bitmap, PNGChunk& png_chunk, Compress::ZlibCompressionLevel compression_level)
{
    ByteBuffer uncompressed_block_data;
    TRY(uncompressed_block_data.try_ensure_capacity(bitmap.size_in_bytes() + bitmap.height()));

    auto dummy_scanline = TRY(FixedArray<Pixel>::create(bitmap.width()));
    auto const* scanline_minus_1 = dummy_scanline.data();

    for (int y = 0; y < bitmap.height(); ++y) {
        auto* scanline = reinterpret_cast<Pixel const*>(bitmap.scanline(y));

        struct Filter {
            PNG::FilterType type;
            AK::SIMD::u32x4 sum { 0, 0, 0, 0 };

            AK::SIMD::u8x4 predict(AK::SIMD::u8x4 pixel, AK::SIMD::u8x4 pixel_x_minus_1, AK::SIMD::u8x4 pixel_y_minus_1, AK::SIMD::u8x4 pixel_xy_minus_1)
            {
                switch (type) {
                case PNG::FilterType::None:
                    return pixel;
                case PNG::FilterType::Sub:
                    return pixel - pixel_x_minus_1;
                case PNG::FilterType::Up:
                    return pixel - pixel_y_minus_1;
                case PNG::FilterType::Average: {
                    // The sum Orig(a) + Orig(b) shall be performed without overflow (using at least nine-bit arithmetic).
                    auto sum = AK::SIMD::simd_cast<AK::SIMD::u16x4>(pixel_x_minus_1) + AK::SIMD::simd_cast<AK::SIMD::u16x4>(pixel_y_minus_1);
                    auto average = AK::SIMD::simd_cast<AK::SIMD::u8x4>(sum / 2);
                    return pixel - average;
                }
                case PNG::FilterType::Paeth:
                    return pixel - PNG::paeth_predictor(pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1);
                }
                VERIFY_NOT_REACHED();
            }

            void append(AK::SIMD::u8x4 simd)
            {
                using namespace AK::SIMD;
                sum += simd_cast<u32x4>(abs(simd_cast<i32x4>(simd_cast<i8x4>(simd))));
            }

            u32 sum_of_abs_values() const
            {
                u32 result = sum[0] + sum[1] + sum[2];
                if constexpr (include_alpha)
                    result += sum[3];
                return result;
            }
        };

        Filter none_filter { .type = PNG::FilterType::None };
        Filter sub_filter { .type = PNG::FilterType::Sub };
        Filter up_filter { .type = PNG::FilterType::Up };
        Filter average_filter { .type = PNG::FilterType::Average };
        Filter paeth_filter { .type = PNG::FilterType::Paeth };

        auto pixel_x_minus_1 = Pixel::argb32_to_simd(dummy_scanline[0]);
        auto pixel_xy_minus_1 = Pixel::argb32_to_simd(dummy_scanline[0]);

        for (int x = 0; x < bitmap.width(); ++x) {
            auto pixel = Pixel::argb32_to_simd(scanline[x]);
            auto pixel_y_minus_1 = Pixel::argb32_to_simd(scanline_minus_1[x]);

            none_filter.append(none_filter.predict(pixel, pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1));
            sub_filter.append(sub_filter.predict(pixel, pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1));
            up_filter.append(up_filter.predict(pixel, pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1));
            average_filter.append(average_filter.predict(pixel, pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1));
            paeth_filter.append(paeth_filter.predict(pixel, pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1));

            pixel_x_minus_1 = pixel;
            pixel_xy_minus_1 = pixel_y_minus_1;
        }

        // 12.8 Filter selection: https://www.w3.org/TR/PNG/#12Filter-selection
        // For best compression of truecolour and greyscale images, the recommended approach
        // is adaptive filtering in which a filter is chosen for each scanline.
        // The following simple heuristic has performed well in early tests:
        // compute the output scanline using all five filters, and select the filter that gives the smallest sum of absolute values of outputs.
        // (Consider the output bytes as signed differences for this test.)
        Filter& best_filter = none_filter;
        if (best_filter.sum_of_abs_values() > sub_filter.sum_of_abs_values())
            best_filter = sub_filter;
        if (best_filter.sum_of_abs_values() > up_filter.sum_of_abs_values())
            best_filter = up_filter;
        if (best_filter.sum_of_abs_values() > average_filter.sum_of_abs_values())
            best_filter = average_filter;
        if (best_filter.sum_of_abs_values() > paeth_filter.sum_of_abs_values())
            best_filter = paeth_filter;

        TRY(uncompressed_block_data.try_append(to_underlying(best_filter.type)));

        pixel_x_minus_1 = Pixel::argb32_to_simd(dummy_scanline[0]);
        pixel_xy_minus_1 = Pixel::argb32_to_simd(dummy_scanline[0]);

        for (int x = 0; x < bitmap.width(); ++x) {
            auto pixel = Pixel::argb32_to_simd(scanline[x]);
            auto pixel_y_minus_1 = Pixel::argb32_to_simd(scanline_minus_1[x]);

            auto predicted_pixel = best_filter.predict(pixel, pixel_x_minus_1, pixel_y_minus_1, pixel_xy_minus_1);
            TRY(uncompressed_block_data.try_append(predicted_pixel[2]));
            TRY(uncompressed_block_data.try_append(predicted_pixel[1]));
            TRY(uncompressed_block_data.try_append(predicted_pixel[0]));
            if constexpr (include_alpha)
                TRY(uncompressed_block_data.try_append(predicted_pixel[3]));

            pixel_x_minus_1 = pixel;
            pixel_xy_minus_1 = pixel_y_minus_1;
        }

        scanline_minus_1 = scanline;
    }

    return png_chunk.compress_and_add(uncompressed_block_data, compression_level);
}

template<bool include_alpha>
ErrorOr<void> PNGWriter::add_fdAT_chunk(Gfx::Bitmap const& bitmap, u32 sequence_number, Compress::ZlibCompressionLevel compression_level)
{
    // https://www.w3.org/TR/png/#fdAT-chunk
    PNGChunk png_chunk { "fdAT"_string };
    TRY(png_chunk.reserve(bitmap.size_in_bytes() + 4));
    TRY(png_chunk.add_as_big_endian(sequence_number));
    TRY(add_image_data_to_chunk<include_alpha>(bitmap, png_chunk, compression_level));
    return add_chunk(png_chunk);
}

template<bool include_alpha>
ErrorOr<void> PNGWriter::add_IDAT_chunk(Gfx::Bitmap const& bitmap, Compress::ZlibCompressionLevel compression_level)
{
    PNGChunk png_chunk { "IDAT"_string };
    TRY(png_chunk.reserve(bitmap.size_in_bytes()));
    TRY(add_image_data_to_chunk<include_alpha>(bitmap, png_chunk, compression_level));
    return add_chunk(png_chunk);
}

static bool bitmap_has_transparency(Bitmap const& bitmap)
{
    for (auto pixel : bitmap) {
        if (Color::from_argb(pixel).alpha() != 255)
            return true;
    }
    return false;
}

ErrorOr<void> PNGWriter::encode(Stream& stream, Bitmap const& bitmap, Options const& options)
{
    bool has_transparency = bitmap_has_transparency(bitmap);

    PNGWriter writer { stream };
    TRY(writer.add_png_header());
    auto color_type = has_transparency ? PNG::ColorType::TruecolorWithAlpha : PNG::ColorType::Truecolor;
    TRY(writer.add_IHDR_chunk(bitmap.width(), bitmap.height(), 8, color_type, 0, 0, 0));
    if (options.icc_data.has_value())
        TRY(writer.add_iCCP_chunk(options.icc_data.value(), options.compression_level));
    if (has_transparency)
        TRY(writer.add_IDAT_chunk<true>(bitmap, options.compression_level));
    else
        TRY(writer.add_IDAT_chunk<false>(bitmap, options.compression_level));
    TRY(writer.add_IEND_chunk());
    return {};
}

ErrorOr<ByteBuffer> PNGWriter::encode(Gfx::Bitmap const& bitmap, Options options)
{
    AllocatingMemoryStream stream;
    TRY(encode(stream, bitmap, options));
    return stream.read_until_eof();
}

class PNGAnimationWriter : public AnimationWriter {
public:
    PNGAnimationWriter(SeekableStream& stream, IntSize dimensions, int loop_count, PNGWriter::Options const& options)
        : m_writer(stream)
        , m_stream(stream)
        , m_dimensions(dimensions)
        , m_loop_count(loop_count)
        , m_options(options)
    {
    }

    virtual ErrorOr<void> add_frame(Bitmap&, int, IntPoint, BlendMode) override;
    virtual bool can_blend_frames() const override { return true; }

private:
    PNGWriter m_writer;
    SeekableStream& m_stream;

    IntSize const m_dimensions;
    int const m_loop_count { 0 };

    bool m_is_first_frame { true };

    u32 m_sequence_number { 0 };
    u32 m_number_of_frames { 0 };
    size_t m_acTL_offset { 0 };
    PNGWriter::Options const m_options;
};

ErrorOr<void> PNGAnimationWriter::add_frame(Bitmap& bitmap, int duration_ms, IntPoint at, BlendMode blend_mode)
{
    ++m_number_of_frames;
    bool const is_first_frame = m_number_of_frames == 1;

    if (is_first_frame) {
        // "The fcTL chunk corresponding to the default image, if it exists, has these restrictions:
        //  * The x_offset and y_offset fields must be 0.
        //  * The width and height fields must equal the corresponding fields from the IHDR chunk."
        // FIXME: If this ends up happening in practice, we should composite `bitmap` to a temporary bitmap and store that as first frame.
        if (at != IntPoint {})
            return Error::from_string_literal("First APNG frame must have x_offset and y_offset set to 0");
        if (bitmap.size() != m_dimensions)
            return Error::from_string_literal("First APNG frame must have the same dimensions as the APNG itself");

        // All frames in an APNG use the same IHDR chunk, which means they all have the same color type.
        // To decide if we should write RGB or RGBA, we'd really have to check all frames, but that needs a
        // lot of memory and makes streaming impossible.
        // Instead, we always include an alpha channel. In practice, inter-frame compression means that
        // even for animations without transparency, all but the first frame will have transparent pixels.
        // The APNG format doesn't give us super great options here.
        TRY(m_writer.add_png_header());
        TRY(m_writer.add_IHDR_chunk(m_dimensions.width(), m_dimensions.height(), 8, PNG::ColorType::TruecolorWithAlpha, 0, 0, 0));
        if (m_options.icc_data.has_value())
            TRY(m_writer.add_iCCP_chunk(m_options.icc_data.value(), m_options.compression_level));
        m_acTL_offset = TRY(m_stream.tell());
        TRY(m_writer.add_acTL_chunk(m_number_of_frames, m_loop_count));
    } else {
        // Overwrite previous acTL chunk to update its num_frames. Use add_acTL_chunk to make sure the chunk's crc is updated too.
        auto current_offset = TRY(m_stream.tell());
        TRY(m_stream.seek(m_acTL_offset, SeekMode::SetPosition));
        TRY(m_writer.add_acTL_chunk(m_number_of_frames, m_loop_count));
        TRY(m_stream.seek(current_offset, SeekMode::SetPosition));

        // Overwrite previous IEND marker.
        TRY(m_stream.seek(-12, SeekMode::FromCurrentPosition));
    }

    fcTLData fcTL_data;
    fcTL_data.sequence_number = m_sequence_number;
    fcTL_data.width = bitmap.width();
    fcTL_data.height = bitmap.height();
    fcTL_data.delay_numerator = duration_ms;
    fcTL_data.delay_denominator = 1000;
    fcTL_data.x_offset = at.x();
    fcTL_data.y_offset = at.y();
    if (blend_mode == BlendMode::Blend)
        fcTL_data.blend_operation = 1;
    TRY(m_writer.add_fcTL_chunk(fcTL_data));
    m_sequence_number++;

    if (is_first_frame) {
        TRY(m_writer.add_IDAT_chunk<true>(bitmap, m_options.compression_level));
    } else {
        TRY(m_writer.add_fdAT_chunk<true>(bitmap, m_sequence_number, m_options.compression_level));
        m_sequence_number++;
    }

    TRY(m_writer.add_IEND_chunk());

    return {};
}

ErrorOr<NonnullOwnPtr<AnimationWriter>> PNGWriter::start_encoding_animation(SeekableStream& stream, IntSize dimensions, int loop_count, Options const& options)
{
    auto writer = make<PNGAnimationWriter>(stream, dimensions, loop_count, options);
    return writer;
}

}
