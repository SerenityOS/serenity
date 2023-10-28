/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/QOILoader.h>

namespace Gfx {

static constexpr auto QOI_MAGIC = "qoif"sv;
static constexpr u8 QOI_OP_RGB = 0b11111110;
static constexpr u8 QOI_OP_RGBA = 0b11111111;
static constexpr u8 QOI_OP_INDEX = 0b00000000;
static constexpr u8 QOI_OP_DIFF = 0b01000000;
static constexpr u8 QOI_OP_LUMA = 0b10000000;
static constexpr u8 QOI_OP_RUN = 0b11000000;
static constexpr u8 QOI_MASK_2 = 0b11000000;
static constexpr u8 END_MARKER[] = { 0, 0, 0, 0, 0, 0, 0, 1 };

static ErrorOr<QOIHeader> decode_qoi_header(Stream& stream)
{
    auto header = TRY(stream.read_value<QOIHeader>());
    if (StringView { header.magic, array_size(header.magic) } != QOI_MAGIC)
        return Error::from_string_literal("Invalid QOI image: incorrect header magic");
    header.width = AK::convert_between_host_and_big_endian(header.width);
    header.height = AK::convert_between_host_and_big_endian(header.height);
    return header;
}

static ErrorOr<Color> decode_qoi_op_rgb(Stream& stream, u8 first_byte, Color pixel)
{
    VERIFY(first_byte == QOI_OP_RGB);
    u8 bytes[3];
    TRY(stream.read_until_filled({ &bytes, array_size(bytes) }));

    // The alpha value remains unchanged from the previous pixel.
    return Color { bytes[0], bytes[1], bytes[2], pixel.alpha() };
}

static ErrorOr<Color> decode_qoi_op_rgba(Stream& stream, u8 first_byte)
{
    VERIFY(first_byte == QOI_OP_RGBA);
    u8 bytes[4];
    TRY(stream.read_until_filled({ &bytes, array_size(bytes) }));
    return Color { bytes[0], bytes[1], bytes[2], bytes[3] };
}

static ErrorOr<u8> decode_qoi_op_index(Stream&, u8 first_byte)
{
    VERIFY((first_byte & QOI_MASK_2) == QOI_OP_INDEX);
    u8 index = first_byte & ~QOI_MASK_2;
    VERIFY(index <= 63);
    return index;
}

static ErrorOr<Color> decode_qoi_op_diff(Stream&, u8 first_byte, Color pixel)
{
    VERIFY((first_byte & QOI_MASK_2) == QOI_OP_DIFF);
    u8 dr = (first_byte & 0b00110000) >> 4;
    u8 dg = (first_byte & 0b00001100) >> 2;
    u8 db = (first_byte & 0b00000011);
    VERIFY(dr <= 3 && dg <= 3 && db <= 3);

    // Values are stored as unsigned integers with a bias of 2.
    return Color {
        static_cast<u8>(pixel.red() + static_cast<i8>(dr - 2)),
        static_cast<u8>(pixel.green() + static_cast<i8>(dg - 2)),
        static_cast<u8>(pixel.blue() + static_cast<i8>(db - 2)),
        pixel.alpha(),
    };
}

static ErrorOr<Color> decode_qoi_op_luma(Stream& stream, u8 first_byte, Color pixel)
{
    VERIFY((first_byte & QOI_MASK_2) == QOI_OP_LUMA);
    auto byte = TRY(stream.read_value<u8>());
    u8 diff_green = (first_byte & ~QOI_MASK_2);
    u8 dr_dg = (byte & 0b11110000) >> 4;
    u8 db_dg = (byte & 0b00001111);

    // Values are stored as unsigned integers with a bias of 32 for the green channel and a bias of 8 for the red and blue channel.
    return Color {
        static_cast<u8>(pixel.red() + static_cast<i8>((diff_green - 32) + (dr_dg - 8))),
        static_cast<u8>(pixel.green() + static_cast<i8>(diff_green - 32)),
        static_cast<u8>(pixel.blue() + static_cast<i8>((diff_green - 32) + (db_dg - 8))),
        pixel.alpha(),
    };
}

static ErrorOr<u8> decode_qoi_op_run(Stream&, u8 first_byte)
{
    VERIFY((first_byte & QOI_MASK_2) == QOI_OP_RUN);
    u8 run = first_byte & ~QOI_MASK_2;

    // The run-length is stored with a bias of -1.
    run += 1;

    // Note that the run-lengths 63 and 64 (b111110 and b111111) are illegal as they are occupied by the QOI_OP_RGB and QOI_OP_RGBA tags.
    if (run == QOI_OP_RGB || run == QOI_OP_RGBA)
        return Error::from_string_literal("Invalid QOI image: illegal run length");

    VERIFY(run >= 1 && run <= 62);
    return run;
}

static ErrorOr<void> decode_qoi_end_marker(Stream& stream)
{
    u8 bytes[array_size(END_MARKER)];
    TRY(stream.read_until_filled({ &bytes, array_size(bytes) }));
    if (!stream.is_eof())
        return Error::from_string_literal("Invalid QOI image: expected end of stream but more bytes are available");
    if (memcmp(&END_MARKER, &bytes, array_size(bytes)) != 0)
        return Error::from_string_literal("Invalid QOI image: incorrect end marker");
    return {};
}

static ErrorOr<NonnullRefPtr<Bitmap>> decode_qoi_image(Stream& stream, u32 width, u32 height)
{
    // FIXME: Why is Gfx::Bitmap's size signed? Makes no sense whatsoever.
    if (width > NumericLimits<int>::max())
        return Error::from_string_literal("Cannot create bitmap for QOI image of valid size, width exceeds maximum Gfx::Bitmap width");
    if (height > NumericLimits<int>::max())
        return Error::from_string_literal("Cannot create bitmap for QOI image of valid size, height exceeds maximum Gfx::Bitmap height");

    auto bitmap = TRY(Bitmap::create(BitmapFormat::BGRA8888, { width, height }));

    u8 run = 0;
    Color pixel = { 0, 0, 0, 255 };
    Color previous_pixels[64] {};

    for (u32 y = 0; y < height; ++y) {
        for (u32 x = 0; x < width; ++x) {
            if (run > 0)
                --run;
            if (run == 0) {
                auto first_byte = TRY(stream.read_value<u8>());
                if (first_byte == QOI_OP_RGB)
                    pixel = TRY(decode_qoi_op_rgb(stream, first_byte, pixel));
                else if (first_byte == QOI_OP_RGBA)
                    pixel = TRY(decode_qoi_op_rgba(stream, first_byte));
                else if ((first_byte & QOI_MASK_2) == QOI_OP_INDEX)
                    pixel = previous_pixels[TRY(decode_qoi_op_index(stream, first_byte))];
                else if ((first_byte & QOI_MASK_2) == QOI_OP_DIFF)
                    pixel = TRY(decode_qoi_op_diff(stream, first_byte, pixel));
                else if ((first_byte & QOI_MASK_2) == QOI_OP_LUMA)
                    pixel = TRY(decode_qoi_op_luma(stream, first_byte, pixel));
                else if ((first_byte & QOI_MASK_2) == QOI_OP_RUN)
                    run = TRY(decode_qoi_op_run(stream, first_byte));
                else
                    return Error::from_string_literal("Invalid QOI image: unknown chunk tag");
            }
            auto index_position = (pixel.red() * 3 + pixel.green() * 5 + pixel.blue() * 7 + pixel.alpha() * 11) % 64;
            previous_pixels[index_position] = pixel;
            bitmap->set_pixel(x, y, pixel);
        }
    }
    TRY(decode_qoi_end_marker(stream));
    return { move(bitmap) };
}

QOIImageDecoderPlugin::QOIImageDecoderPlugin(NonnullOwnPtr<Stream> stream)
{
    m_context = make<QOILoadingContext>();
    m_context->stream = move(stream);
}

IntSize QOIImageDecoderPlugin::size()
{
    return { m_context->header.width, m_context->header.height };
}

bool QOIImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    FixedMemoryStream stream { { data.data(), data.size() } };
    return !decode_qoi_header(stream).is_error();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> QOIImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) QOIImageDecoderPlugin(move(stream))));
    TRY(plugin->decode_header_and_update_context());
    return plugin;
}

ErrorOr<ImageFrameDescriptor> QOIImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("Invalid frame index");

    // No one should try to decode the frame again after an error was already returned.
    VERIFY(m_context->state != QOILoadingContext::State::Error);

    if (m_context->state == QOILoadingContext::State::HeaderDecoded)
        TRY(decode_image_and_update_context());

    VERIFY(m_context->state == QOILoadingContext::State::ImageDecoded);
    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

ErrorOr<void> QOIImageDecoderPlugin::decode_header_and_update_context()
{
    VERIFY(m_context->state < QOILoadingContext::State::HeaderDecoded);
    m_context->header = TRY(decode_qoi_header(*m_context->stream));
    m_context->state = QOILoadingContext::State::HeaderDecoded;
    return {};
}

ErrorOr<void> QOIImageDecoderPlugin::decode_image_and_update_context()
{
    VERIFY(m_context->state < QOILoadingContext::State::ImageDecoded);
    auto error_or_bitmap = decode_qoi_image(*m_context->stream, m_context->header.width, m_context->header.height);
    if (error_or_bitmap.is_error()) {
        m_context->state = QOILoadingContext::State::Error;
        return error_or_bitmap.release_error();
    }
    m_context->state = QOILoadingContext::State::ImageDecoded;
    m_context->bitmap = error_or_bitmap.release_value();
    return {};
}

}
