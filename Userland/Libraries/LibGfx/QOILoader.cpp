/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/QOILoader.h>

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

static ErrorOr<QOIHeader> decode_qoi_header(InputMemoryStream& stream)
{
    QOIHeader header;
    stream >> Bytes { &header, sizeof(header) };
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading header"sv);
    if (StringView { header.magic, array_size(header.magic) } != QOI_MAGIC)
        return Error::from_string_literal("Invalid QOI image: incorrect header magic"sv);
    header.width = AK::convert_between_host_and_big_endian(header.width);
    header.height = AK::convert_between_host_and_big_endian(header.height);
    return header;
}

static ErrorOr<Color> decode_qoi_op_rgb(InputMemoryStream& stream, Color pixel)
{
    u8 bytes[4];
    stream >> Bytes { &bytes, array_size(bytes) };
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading QOI_OP_RGB chunk"sv);
    VERIFY(bytes[0] == QOI_OP_RGB);

    // The alpha value remains unchanged from the previous pixel.
    return Color { bytes[1], bytes[2], bytes[3], pixel.alpha() };
}

static ErrorOr<Color> decode_qoi_op_rgba(InputMemoryStream& stream)
{
    u8 bytes[5];
    stream >> Bytes { &bytes, array_size(bytes) };
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading QOI_OP_RGBA chunk"sv);
    VERIFY(bytes[0] == QOI_OP_RGBA);
    return Color { bytes[1], bytes[2], bytes[3], bytes[4] };
}

static ErrorOr<u8> decode_qoi_op_index(InputMemoryStream& stream)
{
    u8 byte;
    stream >> byte;
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading QOI_OP_INDEX chunk"sv);
    VERIFY((byte & QOI_MASK_2) == QOI_OP_INDEX);
    u8 index = byte & ~QOI_MASK_2;
    VERIFY(index <= 63);
    return index;
}

static ErrorOr<Color> decode_qoi_op_diff(InputMemoryStream& stream, Color pixel)
{
    u8 byte;
    stream >> byte;
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading QOI_OP_DIFF chunk"sv);
    VERIFY((byte & QOI_MASK_2) == QOI_OP_DIFF);
    u8 dr = (byte & 0b00110000) >> 4;
    u8 dg = (byte & 0b00001100) >> 2;
    u8 db = (byte & 0b00000011);
    VERIFY(dr <= 3 && dg <= 3 && db <= 3);

    // Values are stored as unsigned integers with a bias of 2.
    return Color {
        static_cast<u8>(pixel.red() + static_cast<i8>(dr - 2)),
        static_cast<u8>(pixel.green() + static_cast<i8>(dg - 2)),
        static_cast<u8>(pixel.blue() + static_cast<i8>(db - 2)),
        pixel.alpha(),
    };
}

static ErrorOr<Color> decode_qoi_op_luma(InputMemoryStream& stream, Color pixel)
{
    u8 bytes[2];
    stream >> Bytes { &bytes, array_size(bytes) };
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading QOI_OP_LUMA chunk"sv);
    VERIFY((bytes[0] & QOI_MASK_2) == QOI_OP_LUMA);
    u8 diff_green = (bytes[0] & ~QOI_MASK_2);
    u8 dr_dg = (bytes[1] & 0b11110000) >> 4;
    u8 db_dg = (bytes[1] & 0b00001111);

    // Values are stored as unsigned integers with a bias of 32 for the green channel and a bias of 8 for the red and blue channel.
    return Color {
        static_cast<u8>(pixel.red() + static_cast<i8>((diff_green - 32) + (dr_dg - 8))),
        static_cast<u8>(pixel.green() + static_cast<i8>(diff_green - 32)),
        static_cast<u8>(pixel.blue() + static_cast<i8>((diff_green - 32) + (db_dg - 8))),
        pixel.alpha(),
    };
}

static ErrorOr<u8> decode_qoi_op_run(InputMemoryStream& stream)
{
    u8 byte;
    stream >> byte;
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading QOI_OP_RUN chunk"sv);
    VERIFY((byte & QOI_MASK_2) == QOI_OP_RUN);
    u8 run = byte & ~QOI_MASK_2;

    // The run-length is stored with a bias of -1.
    run += 1;

    // Note that the run-lengths 63 and 64 (b111110 and b111111) are illegal as they are occupied by the QOI_OP_RGB and QOI_OP_RGBA tags.
    if (run == QOI_OP_RGB || run == QOI_OP_RGBA)
        return Error::from_string_literal("Invalid QOI image: illegal run length"sv);

    VERIFY(run >= 1 && run <= 62);
    return run;
}

static ErrorOr<void> decode_qoi_end_marker(InputMemoryStream& stream)
{
    u8 bytes[array_size(END_MARKER)];
    stream >> Bytes { &bytes, array_size(bytes) };
    if (stream.handle_any_error())
        return Error::from_string_literal("Invalid QOI image: end of stream while reading end marker"sv);
    if (!stream.eof())
        return Error::from_string_literal("Invalid QOI image: expected end of stream but more bytes are available"sv);
    if (memcmp(&END_MARKER, &bytes, array_size(bytes)) != 0)
        return Error::from_string_literal("Invalid QOI image: incorrect end marker"sv);
    return {};
}

static ErrorOr<NonnullRefPtr<Bitmap>> decode_qoi_image(InputMemoryStream& stream, u32 width, u32 height)
{
    // FIXME: Why is Gfx::Bitmap's size signed? Makes no sense whatsoever.
    if (width > NumericLimits<int>::max())
        return Error::from_string_literal("Cannot create bitmap for QOI image of valid size, width exceeds maximum Gfx::Bitmap width"sv);
    if (height > NumericLimits<int>::max())
        return Error::from_string_literal("Cannot create bitmap for QOI image of valid size, height exceeds maximum Gfx::Bitmap height"sv);

    auto bitmap = TRY(Bitmap::try_create(BitmapFormat::BGRA8888, { width, height }));

    u8 run = 0;
    Color pixel = { 0, 0, 0, 255 };
    Color previous_pixels[64] {};

    for (u32 y = 0; y < height; ++y) {
        for (u32 x = 0; x < width; ++x) {
            if (run > 0)
                --run;
            if (run == 0) {
                u8 tag = stream.peek_or_error();
                if (stream.handle_any_error())
                    return Error::from_string_literal("Invalid QOI image: end of stream while reading chunk tag"sv);
                if (tag == QOI_OP_RGB)
                    pixel = TRY(decode_qoi_op_rgb(stream, pixel));
                else if (tag == QOI_OP_RGBA)
                    pixel = TRY(decode_qoi_op_rgba(stream));
                else if ((tag & QOI_MASK_2) == QOI_OP_INDEX)
                    pixel = previous_pixels[TRY(decode_qoi_op_index(stream))];
                else if ((tag & QOI_MASK_2) == QOI_OP_DIFF)
                    pixel = TRY(decode_qoi_op_diff(stream, pixel));
                else if ((tag & QOI_MASK_2) == QOI_OP_LUMA)
                    pixel = TRY(decode_qoi_op_luma(stream, pixel));
                else if ((tag & QOI_MASK_2) == QOI_OP_RUN)
                    run = TRY(decode_qoi_op_run(stream));
                else
                    return Error::from_string_literal("Invalid QOI image: unknown chunk tag"sv);
            }
            auto index_position = (pixel.red() * 3 + pixel.green() * 5 + pixel.blue() * 7 + pixel.alpha() * 11) % 64;
            previous_pixels[index_position] = pixel;
            bitmap->set_pixel(x, y, pixel);
        }
    }
    TRY(decode_qoi_end_marker(stream));
    return { move(bitmap) };
}

QOIImageDecoderPlugin::QOIImageDecoderPlugin(u8 const* data, size_t size)
{
    m_context = make<QOILoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

IntSize QOIImageDecoderPlugin::size()
{
    if (m_context->state < QOILoadingContext::State::HeaderDecoded) {
        InputMemoryStream stream { { m_context->data, m_context->data_size } };
        // FIXME: This is a weird API (inherited from ImageDecoderPlugin), should probably propagate errors by returning ErrorOr<IntSize>.
        //        For the time being, ignore the result and rely on the context's state.
        (void)decode_header_and_update_context(stream);
    }

    if (m_context->state == QOILoadingContext::State::Error)
        return {};

    return { m_context->header.width, m_context->header.height };
}

void QOIImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool QOIImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;
    return m_context->bitmap->set_nonvolatile(was_purged);
}

bool QOIImageDecoderPlugin::sniff()
{
    InputMemoryStream stream { { m_context->data, m_context->data_size } };
    return !decode_qoi_header(stream).is_error();
}

ErrorOr<ImageFrameDescriptor> QOIImageDecoderPlugin::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("Invalid frame index"sv);

    if (m_context->state == QOILoadingContext::State::NotDecoded) {
        InputMemoryStream stream { { m_context->data, m_context->data_size } };
        TRY(decode_header_and_update_context(stream));
        TRY(decode_image_and_update_context(stream));
    } else if (m_context->state == QOILoadingContext::State::HeaderDecoded) {
        InputMemoryStream stream { { m_context->data, m_context->data_size } };
        VERIFY(stream.discard_or_error(sizeof(m_context->header)));
        TRY(decode_image_and_update_context(stream));
    }

    if (m_context->state == QOILoadingContext::State::ImageDecoded) {
        VERIFY(m_context->bitmap);
        return ImageFrameDescriptor { m_context->bitmap, 0 };
    }

    VERIFY(m_context->state == QOILoadingContext::State::Error);
    VERIFY(m_context->error.has_value());
    return *m_context->error;
}

ErrorOr<void> QOIImageDecoderPlugin::decode_header_and_update_context(InputMemoryStream& stream)
{
    VERIFY(m_context->state < QOILoadingContext::State::HeaderDecoded);
    auto error_or_header = decode_qoi_header(stream);
    if (error_or_header.is_error()) {
        m_context->state = QOILoadingContext::State::Error;
        m_context->error = error_or_header.release_error();
        return *m_context->error;
    }
    m_context->state = QOILoadingContext::State::HeaderDecoded;
    m_context->header = error_or_header.release_value();
    return {};
}

ErrorOr<void> QOIImageDecoderPlugin::decode_image_and_update_context(InputMemoryStream& stream)
{
    VERIFY(m_context->state < QOILoadingContext::State::ImageDecoded);
    auto error_or_bitmap = decode_qoi_image(stream, m_context->header.width, m_context->header.height);
    if (error_or_bitmap.is_error()) {
        m_context->state = QOILoadingContext::State::Error;
        m_context->error = error_or_bitmap.release_error();
        return *m_context->error;
    }
    m_context->state = QOILoadingContext::State::ImageDecoded;
    m_context->bitmap = error_or_bitmap.release_value();
    return {};
}

}
