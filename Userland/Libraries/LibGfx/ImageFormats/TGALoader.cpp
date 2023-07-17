/*
 * Copyright (c) 2022, Tom Needham <06needhamt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/String.h>
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

union [[gnu::packed]] TGAPixel {
    struct TGAColor {
        u8 blue;
        u8 green;
        u8 red;
        u8 alpha;
    } components;

    u32 data;
};

struct TGAPixelPacket {
    bool raw;
    u8 pixels_count;
};

static_assert(AssertSize<TGAPixel, 4>());

class TGAReader {
public:
    TGAReader(ReadonlyBytes data)
        : m_data(move(data))
    {
    }

    TGAReader(ReadonlyBytes data, size_t index)
        : m_data(move(data))
        , m_index(index)
    {
    }

    ALWAYS_INLINE u8 read_u8()
    {
        u8 value = m_data[m_index];
        m_index++;
        return value;
    }

    ALWAYS_INLINE i8 read_i8()
    {
        return static_cast<i8>(read_u8());
    }

    ALWAYS_INLINE u16 read_u16()
    {
        return read_u8() | read_u8() << 8;
    }

    ALWAYS_INLINE i16 read_i16()
    {
        return read_i8() | read_i8() << 8;
    }

    ALWAYS_INLINE u32 read_u32()
    {
        return read_u16() | read_u16() << 16;
    }

    ALWAYS_INLINE i32 read_i32()
    {
        return read_i16() | read_i16() << 16;
    }

    ALWAYS_INLINE TGAPixelPacket read_packet_type()
    {
        auto pixel_packet_type = read_u8();
        auto pixel_packet = TGAPixelPacket();
        pixel_packet.raw = !(pixel_packet_type & 0x80);
        pixel_packet.pixels_count = (pixel_packet_type & 0x7f);

        // NOTE: Run-length-encoded/Raw pixel packets cannot encode zero pixels,
        // so value 0 stands for 1 pixel, 1 stands for 2, etc...
        pixel_packet.pixels_count++;
        return pixel_packet;
    }

    ALWAYS_INLINE TGAPixel read_pixel(u8 bits_per_pixel)
    {
        auto pixel = TGAPixel();

        switch (bits_per_pixel) {
        case 24:
            pixel.components.blue = read_u8();
            pixel.components.green = read_u8();
            pixel.components.red = read_u8();
            pixel.components.alpha = 0xFF;
            return pixel;

        case 32:
            pixel.components.blue = read_u8();
            pixel.components.green = read_u8();
            pixel.components.red = read_u8();
            pixel.components.alpha = read_u8();
            return pixel;

        default:
            VERIFY_NOT_REACHED();
        }
    }

    size_t index() const
    {
        return m_index;
    }

    ReadonlyBytes data() const
    {
        return m_data;
    }

private:
    ReadonlyBytes m_data;
    size_t m_index { 0 };
};

struct TGALoadingContext {
    TGAHeader header {};
    OwnPtr<TGAReader> reader = { nullptr };
    RefPtr<Gfx::Bitmap> bitmap;
};

TGAImageDecoderPlugin::TGAImageDecoderPlugin(u8 const* file_data, size_t file_size)
{
    m_context = make<TGALoadingContext>();
    m_context->reader = make<TGAReader>(ReadonlyBytes { file_data, file_size });
}

TGAImageDecoderPlugin::~TGAImageDecoderPlugin() = default;

IntSize TGAImageDecoderPlugin::size()
{
    return IntSize { m_context->header.width, m_context->header.height };
}

ErrorOr<void> TGAImageDecoderPlugin::decode_tga_header()
{
    auto& reader = m_context->reader;
    if (reader->data().size() < sizeof(TGAHeader))
        return Error::from_string_literal("Not enough data to be a TGA image");

    m_context->header.id_length = reader->read_u8();
    m_context->header.color_map_type = reader->read_u8();
    m_context->header.data_type_code = static_cast<TGADataType>(reader->read_u8());
    m_context->header.color_map_origin = reader->read_i16();
    m_context->header.color_map_length = reader->read_i16();
    m_context->header.color_map_depth = reader->read_u8();
    m_context->header.x_origin = reader->read_i16();
    m_context->header.y_origin = reader->read_i16();
    m_context->header.width = reader->read_u16();
    m_context->header.height = reader->read_u16();
    m_context->header.bits_per_pixel = reader->read_u8();
    m_context->header.image_descriptor = reader->read_u8();

    auto bytes_remaining = reader->data().size() - reader->index();

    // FIXME: Check for multiplication overflow!
    if (m_context->header.data_type_code == TGADataType::UncompressedRGB && bytes_remaining < static_cast<size_t>(m_context->header.width * m_context->header.height * (m_context->header.bits_per_pixel / 8)))
        return Error::from_string_literal("Not enough data to read an image with the expected size");

    if (m_context->header.bits_per_pixel < 8 || m_context->header.bits_per_pixel > 32)
        return Error::from_string_literal("Invalid bit depth");

    return {};
}

ErrorOr<bool> TGAImageDecoderPlugin::validate_before_create(ReadonlyBytes data)
{
    if (data.size() < sizeof(TGAHeader))
        return false;
    TGAHeader const& header = *reinterpret_cast<TGAHeader const*>(data.data());
    // FIXME: Check for multiplication overflow!
    if (header.data_type_code == TGADataType::UncompressedRGB && data.size() < static_cast<size_t>(header.width * header.height * (header.bits_per_pixel / 8)))
        return false;
    if (header.bits_per_pixel < 8 || header.bits_per_pixel > 32)
        return false;
    return true;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> TGAImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TGAImageDecoderPlugin(data.data(), data.size())));
    TRY(plugin->decode_tga_header());
    return plugin;
}

ErrorOr<ImageFrameDescriptor> TGAImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    auto bits_per_pixel = m_context->header.bits_per_pixel;
    auto color_map = m_context->header.color_map_type;
    auto data_type = m_context->header.data_type_code;
    auto width = m_context->header.width;
    auto height = m_context->header.height;
    auto x_origin = m_context->header.x_origin;
    auto y_origin = m_context->header.y_origin;

    if (index != 0)
        return Error::from_string_literal("TGAImageDecoderPlugin: frame index must be 0");

    if (color_map > 1)
        return Error::from_string_literal("TGAImageDecoderPlugin: Invalid color map type");

    if (m_context->bitmap) {
        return ImageFrameDescriptor { m_context->bitmap, 0 };
    } else {
        // NOTE: Just to be on the safe side, if m_context->bitmap is nullptr, then
        // just re-construct the reader object. This will ensure that if the bitmap
        // was set as volatile and therefore it is gone, we can always re-generate it
        // with a new call to this method!
        VERIFY(m_context->reader);
        m_context->reader = make<TGAReader>(m_context->reader->data(), sizeof(TGAHeader));
    }

    RefPtr<Gfx::Bitmap> bitmap;
    switch (bits_per_pixel) {
    case 24:
        bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, { m_context->header.width, m_context->header.height }));
        break;

    case 32:
        bitmap = TRY(Bitmap::create(BitmapFormat::BGRA8888, { m_context->header.width, m_context->header.height }));
        break;

    default:
        // FIXME: Implement other TGA bit depths
        return Error::from_string_literal("TGAImageDecoderPlugin: Can only handle 24 and 32 bits per pixel");
    }

    // FIXME: Try to understand the Image origin (instead of X and Y origin coordinates)
    // based on the Image descriptor, Field 5.6, bits 4 and 5.

    // NOTE: If Y origin is set to a negative number, just assume the generating software
    // meant that we start with Y origin at the top height of the picture.
    // At least this is the observed behavior when generating some pictures in GIMP.
    if (y_origin < 0)
        y_origin = height;
    if (y_origin != 0 && y_origin != height)
        return Error::from_string_literal("TGAImageDecoderPlugin: Can only handle Y origin which is 0 or the entire height");
    if (x_origin != 0 && x_origin != width)
        return Error::from_string_literal("TGAImageDecoderPlugin: Can only handle X origin which is 0 or the entire width");

    switch (data_type) {
    case TGADataType::UncompressedRGB: {
        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                auto pixel = m_context->reader->read_pixel(bits_per_pixel);
                auto actual_row = row;
                if (y_origin < height)
                    actual_row = height - 1 - row;
                auto actual_col = col;
                if (x_origin > width)
                    actual_col = width - 1 - col;
                bitmap->scanline(actual_row)[actual_col] = pixel.data;
            }
        }
        break;
    }
    case TGADataType::RunLengthEncodedRGB: {
        size_t pixel_index = 0;
        size_t pixel_count = height * width;
        while (pixel_index < pixel_count) {
            auto packet_type = m_context->reader->read_packet_type();
            VERIFY(packet_type.pixels_count > 0);
            TGAPixel pixel = m_context->reader->read_pixel(bits_per_pixel);
            auto max_pixel_index = min(pixel_index + packet_type.pixels_count, pixel_count);
            for (size_t current_pixel_index = pixel_index; current_pixel_index < max_pixel_index; ++current_pixel_index) {
                int row = current_pixel_index / width;
                int col = current_pixel_index % width;
                auto actual_row = row;
                if (y_origin < height)
                    actual_row = height - 1 - row;
                auto actual_col = col;
                if (x_origin > width)
                    actual_col = width - 1 - col;
                bitmap->scanline(actual_row)[actual_col] = pixel.data;
                if (packet_type.raw && (current_pixel_index + 1) < max_pixel_index)
                    pixel = m_context->reader->read_pixel(bits_per_pixel);
            }
            pixel_index += packet_type.pixels_count;
        }
        break;
    }
    default:
        // FIXME: Implement other TGA data types
        return Error::from_string_literal("TGAImageDecoderPlugin: Can currently only handle the UncompressedRGB or CompressedRGB data type");
    }

    m_context->bitmap = bitmap;
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

ErrorOr<Optional<ReadonlyBytes>> TGAImageDecoderPlugin::icc_data()
{
    return OptionalNone {};
}

}
