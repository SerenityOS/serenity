/*
 * Copyright (c) 2022, Tom Needham <06needhamt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/Span.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/String.h>
#include <AK/Traits.h>
#include <LibGfx/ImageFormats/TGALoader.h>

namespace Gfx {

enum TGADataType : u8 {
    None = 0,
    UncompressedColorMapped = 1,
    UncompressedRGB = 2,
    UncompressedBlackAndWhite = 3,
    RunLengthEncodedColorMapped = 9,
    RunLengthEncodedRGB = 10,
    CompressedBlackAndWhite = 11,
    CompressedColorMapped = 32,
    CompressedColorMappedFourPass = 33
};

struct [[gnu::packed]] TGAHeader {
    u8 id_length;
    u8 color_map_type;
    TGADataType data_type_code;
    i16 color_map_origin;
    i16 color_map_length;
    u8 color_map_depth;
    i16 x_origin;
    i16 y_origin;
    u16 width;
    u16 height;
    u8 bits_per_pixel;
    u8 image_descriptor;
};
static_assert(sizeof(TGAHeader) == 18);

}

template<>
struct AK::Traits<Gfx::TGAHeader> : public DefaultTraits<Gfx::TGAHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace Gfx {

struct TGALoadingContext {
    TGALoadingContext(ReadonlyBytes bytes, FixedMemoryStream stream)
        : bytes(bytes)
        , stream(move(stream))
    {
    }
    ReadonlyBytes bytes;
    FixedMemoryStream stream;
    TGAHeader header {};
    RefPtr<Gfx::Bitmap> bitmap;
};

TGAImageDecoderPlugin::TGAImageDecoderPlugin(NonnullOwnPtr<TGALoadingContext> context)
    : m_context(move(context))
{
}

TGAImageDecoderPlugin::~TGAImageDecoderPlugin() = default;

IntSize TGAImageDecoderPlugin::size()
{
    return IntSize { m_context->header.width, m_context->header.height };
}

static ErrorOr<void> ensure_header_validity(TGAHeader const& header, size_t whole_image_stream_size)
{
    if ((header.bits_per_pixel % 8) != 0 || header.bits_per_pixel < 8 || header.bits_per_pixel > 32)
        return Error::from_string_literal("Invalid bit depth");
    auto bytes_remaining = whole_image_stream_size - sizeof(TGAHeader);
    if (header.data_type_code == TGADataType::UncompressedRGB && bytes_remaining < static_cast<u64>(header.width) * header.height * (header.bits_per_pixel / 8))
        return Error::from_string_literal("Not enough data to read an image with the expected size");
    return {};
}

ErrorOr<void> TGAImageDecoderPlugin::decode_tga_header()
{
    m_context->header = TRY(m_context->stream.read_value<TGAHeader>());
    TRY(ensure_header_validity(m_context->header, m_context->bytes.size()));
    return {};
}

bool TGAImageDecoderPlugin::validate_before_create(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };
    auto header_or_err = stream.read_value<Gfx::TGAHeader>();
    if (header_or_err.is_error())
        return false;
    return !ensure_header_validity(header_or_err.release_value(), data.size()).is_error();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> TGAImageDecoderPlugin::create(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };
    auto context = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TGALoadingContext(data, move(stream))));
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TGAImageDecoderPlugin(move(context))));
    TRY(plugin->decode_tga_header());
    return plugin;
}

static ErrorOr<ARGB32> read_pixel_from_stream(Stream& stream, size_t bytes_size)
{
    // NOTE: We support 8-bit, 24-bit and 32-bit color pixels
    VERIFY(bytes_size == 1 || bytes_size == 3 || bytes_size == 4);

    if (bytes_size == 1) {
        auto raw = TRY(stream.read_value<u8>());
        return Color(raw, raw, raw).value();
    }
    if (bytes_size == 3) {
        Array<u8, 3> raw;
        TRY(stream.read_until_filled(raw.span()));
        return Color(raw[2], raw[1], raw[0]).value();
    }
    return stream.read_value<ARGB32>();
}

struct TGAPixelPacketHeader {
    bool raw { false };
    u8 pixels_count { 0 };
};

static ErrorOr<TGAPixelPacketHeader> read_pixel_packet_header(Stream& stream)
{
    auto const pixel_packet_header = TRY(stream.read_value<u8>());
    bool pixels_raw_in_packet = !(pixel_packet_header & 0x80);
    u8 pixels_count_in_packet = (pixel_packet_header & 0x7f);
    // NOTE: Run-length-encoded/Raw pixel packets cannot encode zero pixels,
    // so value 0 stands for 1 pixel, 1 stands for 2, etc...
    pixels_count_in_packet++;
    VERIFY(pixels_count_in_packet > 0);
    return TGAPixelPacketHeader { pixels_raw_in_packet, pixels_count_in_packet };
}

ErrorOr<ImageFrameDescriptor> TGAImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    auto bits_per_pixel = m_context->header.bits_per_pixel;
    auto color_map = m_context->header.color_map_type;
    auto data_type = m_context->header.data_type_code;
    auto width = m_context->header.width;
    auto height = m_context->header.height;
    auto image_descriptior = m_context->header.image_descriptor;

    if (index != 0)
        return Error::from_string_literal("TGAImageDecoderPlugin: frame index must be 0");

    if (color_map > 1)
        return Error::from_string_literal("TGAImageDecoderPlugin: Invalid color map type");

    if (m_context->bitmap)
        return ImageFrameDescriptor { m_context->bitmap, 0 };

    RefPtr<Gfx::Bitmap> bitmap;
    switch (bits_per_pixel) {
    case 8:
    case 24:
        bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, { m_context->header.width, m_context->header.height }));
        break;

    case 32:
        bitmap = TRY(Bitmap::create(BitmapFormat::BGRA8888, { m_context->header.width, m_context->header.height }));
        break;

    default:
        // FIXME: Implement other TGA bit depths
        return Error::from_string_literal("TGAImageDecoderPlugin: Can only handle 8, 24 and 32 bits per pixel");
    }

    auto is_top_to_bottom = (image_descriptior & 1 << 5) == 0;
    auto is_left_to_right = (image_descriptior & 1 << 4) == 0;

    VERIFY((bits_per_pixel % 8) == 0);
    auto bytes_per_pixel = bits_per_pixel / 8;

    switch (data_type) {
    case TGADataType::UncompressedBlackAndWhite:
    case TGADataType::UncompressedRGB: {
        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                auto actual_row = row;
                if (is_top_to_bottom)
                    actual_row = height - 1 - row;
                auto actual_col = col;
                if (!is_left_to_right)
                    actual_col = width - 1 - col;
                bitmap->scanline(actual_row)[actual_col] = TRY(read_pixel_from_stream(m_context->stream, bytes_per_pixel));
            }
        }
        break;
    }

    case TGADataType::RunLengthEncodedRGB: {
        size_t pixel_index = 0;
        size_t pixel_count = height * width;
        while (pixel_index < pixel_count) {
            auto pixel_packet_header = TRY(read_pixel_packet_header(m_context->stream));
            VERIFY(pixel_packet_header.pixels_count > 0);

            auto pixel = TRY(read_pixel_from_stream(m_context->stream, bytes_per_pixel));
            auto max_pixel_index = min(pixel_index + pixel_packet_header.pixels_count, pixel_count);
            for (size_t current_pixel_index = pixel_index; current_pixel_index < max_pixel_index; ++current_pixel_index) {
                int row = current_pixel_index / width;
                int col = current_pixel_index % width;
                auto actual_row = row;
                if (is_top_to_bottom)
                    actual_row = height - 1 - row;
                auto actual_col = col;
                if (!is_left_to_right)
                    actual_col = width - 1 - col;
                bitmap->scanline(actual_row)[actual_col] = pixel;
                if (pixel_packet_header.raw && (current_pixel_index + 1) < max_pixel_index)
                    pixel = TRY(read_pixel_from_stream(m_context->stream, bytes_per_pixel));
            }
            pixel_index += pixel_packet_header.pixels_count;
        }
        break;
    }
    default:
        // FIXME: Implement other TGA data types
        return Error::from_string_literal("TGAImageDecoderPlugin: Can currently only handle the UncompressedRGB, CompressedRGB or UncompressedBlackAndWhite data type");
    }

    m_context->bitmap = bitmap;
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
