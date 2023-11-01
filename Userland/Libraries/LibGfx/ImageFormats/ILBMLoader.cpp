/*
 * Copyright (c) 2023, Nicolas Ramz <nicolas.ramz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/FixedArray.h>
#include <LibGfx/ImageFormats/ILBMLoader.h>

namespace Gfx {

struct IFFHeader {
    FourCC form;
    BigEndian<u32> file_size;
    FourCC format;
};

static_assert(AssertSize<IFFHeader, 12>());

struct Chunk {
    FourCC type;
    ReadonlyBytes data;
};

enum class CompressionType : u8 {
    None = 0,
    ByteRun = 1,
    __Count
};

enum class MaskType : u8 {
    None = 0,
    HasMask = 1,
    HasTransparentColor = 2,
    HasLasso = 3,
    __Count
};

enum class ViewportMode : u32 {
    EHB = 0x80,
    HAM = 0x800
};

AK_ENUM_BITWISE_OPERATORS(ViewportMode);

struct ChunkHeader {
    FourCC chunk_type;
    BigEndian<u32> chunk_size;
};

struct BMHDHeader {
    BigEndian<u16> width;
    BigEndian<u16> height;
    BigEndian<i16> x;
    BigEndian<i16> y;
    u8 planes;
    MaskType mask;
    CompressionType compression;
    u8 pad;
    BigEndian<u16> transparent_color;
    u8 x_aspect;
    u8 y_aspect;
    BigEndian<u16> page_width;
    BigEndian<u16> page_height;
};

static_assert(sizeof(BMHDHeader) == 20);

struct ILBMLoadingContext {
    enum class State {
        NotDecoded = 0,
        HeaderDecoded,
        BitmapDecoded
    };
    State state { State::NotDecoded };
    ReadonlyBytes data;

    // points to current chunk
    ReadonlyBytes chunks_cursor;

    // max number of bytes per plane row
    u16 pitch;

    ViewportMode viewport_mode;

    Vector<Color> color_table;

    RefPtr<Gfx::Bitmap> bitmap;

    BMHDHeader bm_header;
};

static ErrorOr<void> decode_iff_ilbm_header(ILBMLoadingContext& context)
{
    if (context.state >= ILBMLoadingContext::State::HeaderDecoded)
        return {};

    if (context.data.size() < sizeof(IFFHeader))
        return Error::from_string_literal("Missing IFF header");

    auto& header = *bit_cast<IFFHeader const*>(context.data.data());
    if (header.form != FourCC("FORM") || header.format != FourCC("ILBM"))
        return Error::from_string_literal("Invalid IFF-ILBM header");

    return {};
}

static ErrorOr<Vector<Color>> decode_cmap_chunk(Chunk cmap_chunk)
{
    size_t const size = cmap_chunk.data.size() / 3;
    Vector<Color> color_table;
    TRY(color_table.try_ensure_capacity(size));

    for (size_t i = 0; i < size; ++i) {
        color_table.unchecked_append(Color(cmap_chunk.data[i * 3], cmap_chunk.data[(i * 3) + 1], cmap_chunk.data[(i * 3) + 2]));
    }

    return color_table;
}

static ErrorOr<RefPtr<Gfx::Bitmap>> chunky_to_bitmap(ILBMLoadingContext& context, ByteBuffer const& chunky)
{
    auto const width = context.bm_header.width;
    auto const height = context.bm_header.height;

    RefPtr<Gfx::Bitmap> bitmap = TRY(Bitmap::create(BitmapFormat::BGRA8888, { width, height }));

    dbgln_if(ILBM_DEBUG, "created Bitmap {}x{}", width, height);

    for (int row = 0; row < height; ++row) {
        for (int col = 0; col < width; ++col) {
            u8 index = chunky[(width * row) + col];
            bitmap->set_pixel(col, row, context.color_table[index]);
        }
    }

    dbgln_if(ILBM_DEBUG, "filled Bitmap");

    return bitmap;
}

static ErrorOr<ByteBuffer> planar_to_chunky(ReadonlyBytes bitplanes, ILBMLoadingContext& context)
{
    dbgln_if(ILBM_DEBUG, "planar_to_chunky");
    u16 pitch = context.pitch;
    u16 width = context.bm_header.width;
    u16 height = context.bm_header.height;
    u8 planes = context.bm_header.planes;
    auto chunky = TRY(ByteBuffer::create_zeroed(static_cast<size_t>(width) * height));

    for (u16 y = 0; y < height; y++) {
        size_t scanline = y * width;
        for (u8 p = 0; p < planes; p++) {
            u8 const plane_mask = 1 << p;
            size_t offset_base = (pitch * planes * y) + (p * pitch);
            if (offset_base + pitch > bitplanes.size() || scanline + ((pitch - 1) * 8) + 7 >= chunky.size())
                return Error::from_string_literal("Malformed bitplane data");

            for (u16 i = 0; i < pitch; i++) {
                u8 bit = bitplanes[offset_base + i];

                for (u8 b = 0; b < 8; b++) {
                    u8 mask = 1 << (7 - b);
                    // get current plane
                    if (bit & mask) {
                        u16 x = (i * 8) + b;
                        chunky[scanline + x] |= plane_mask;
                    }
                }
            }
        }
    }

    return chunky;
}

static ErrorOr<ByteBuffer> uncompress_byte_run(ReadonlyBytes data, ILBMLoadingContext& context)
{
    auto length = data.size();
    dbgln_if(ILBM_DEBUG, "uncompress_byte_run pitch={} size={}", context.pitch, data.size());

    size_t plane_data_size = context.pitch * context.bm_header.height * context.bm_header.planes;

    // The maximum run length of this compression method is 127 bytes, so the uncompressed size
    // cannot be more than 127 times the size of the chunk we are decompressing.
    if (plane_data_size > NumericLimits<u32>::max() || ceil_div(plane_data_size, 127ul) > length)
        return Error::from_string_literal("Uncompressed data size too large");

    auto plane_data = TRY(ByteBuffer::create_uninitialized(plane_data_size));

    u32 index = 0;
    u32 read_bytes = 0;
    while (read_bytes < length) {
        auto const byte = static_cast<i8>(data[read_bytes++]);
        if (byte >= -127 && byte <= -1) {
            // read next byte
            if (read_bytes == data.size())
                return Error::from_string_literal("Malformed compressed data");

            u8 next_byte = data[read_bytes++];
            if (index + -byte >= plane_data.size())
                return Error::from_string_literal("Malformed compressed data");

            for (u16 i = 0; i < -byte + 1; ++i) {
                plane_data[index++] = next_byte;
            }
        } else if (byte >= 0) {
            if (index + byte >= plane_data_size || read_bytes + byte >= length)
                return Error::from_string_literal("Malformed compressed data");

            for (u16 i = 0; i < byte + 1; ++i) {
                plane_data[index] = data[read_bytes];
                read_bytes++;
                index++;
            }
        }
    }

    if (index != plane_data_size)
        return Error::from_string_literal("Unexpected end of chunk while decompressing data");

    return plane_data;
}

static ErrorOr<void> extend_ehb_palette(ILBMLoadingContext& context)
{
    dbgln_if(ILBM_DEBUG, "need to extend palette");
    for (size_t i = 0; i < 32; ++i) {
        auto const color = context.color_table[i];
        TRY(context.color_table.try_append(color.darkened()));
    }

    return {};
}

static ErrorOr<void> decode_body_chunk(Chunk body_chunk, ILBMLoadingContext& context)
{
    dbgln_if(ILBM_DEBUG, "decode_body_chunk {}", body_chunk.data.size());

    ByteBuffer pixel_data;

    if (context.bm_header.compression == CompressionType::ByteRun) {
        auto plane_data = TRY(uncompress_byte_run(body_chunk.data, context));
        pixel_data = TRY(planar_to_chunky(plane_data, context));
    } else {
        pixel_data = TRY(planar_to_chunky(body_chunk.data, context));
    }

    // Some files already have 64 colours defined in the palette,
    // maybe for upward compatibility with 256 colours software/hardware.
    // DPaint 4 & previous files only have 32 colours so the
    // palette needs to be extended only for these files.
    if (has_flag(context.viewport_mode, ViewportMode::EHB) && context.color_table.size() < 64) {
        TRY(extend_ehb_palette(context));
    }

    context.bitmap = TRY(chunky_to_bitmap(context, pixel_data));

    return {};
}

static ErrorOr<Chunk> decode_iff_chunk_header(ReadonlyBytes chunks)
{
    if (chunks.size() < sizeof(ChunkHeader))
        return Error::from_string_literal("Not enough data for IFF chunk header");

    auto const& header = *bit_cast<ChunkHeader const*>(chunks.data());

    if (chunks.size() < sizeof(ChunkHeader) + header.chunk_size)
        return Error::from_string_literal("Not enough data for IFF chunk");

    return Chunk { header.chunk_type, { chunks.data() + sizeof(ChunkHeader), header.chunk_size } };
}

static ErrorOr<Chunk> decode_iff_advance_chunk(ReadonlyBytes& chunks)
{
    auto chunk = TRY(decode_iff_chunk_header(chunks));

    chunks = chunks.slice(sizeof(ChunkHeader) + chunk.data.size());

    // add padding if needed
    if (chunk.data.size() % 2 != 0) {
        if (chunks.is_empty())
            return Error::from_string_literal("Missing data for padding byte");
        if (*chunks.data() != 0)
            return Error::from_string_literal("Padding byte is not 0");
        chunks = chunks.slice(1);
    }

    return chunk;
}

static ErrorOr<void> decode_iff_chunks(ILBMLoadingContext& context)
{
    auto& chunks = context.chunks_cursor;

    dbgln_if(ILBM_DEBUG, "decode_iff_chunks");

    while (!chunks.is_empty()) {
        auto chunk = TRY(decode_iff_advance_chunk(chunks));
        if (chunk.type == FourCC("CMAP")) {
            if (chunk.data.size() != (1ul << context.bm_header.planes) * 3)
                return Error::from_string_literal("Invalid CMAP chunk size");

            context.color_table = TRY(decode_cmap_chunk(chunk));
        } else if (chunk.type == FourCC("BODY")) {
            if (context.color_table.is_empty())
                return Error::from_string_literal("Decoding BODY chunk without a color map is not currently supported");

            TRY(decode_body_chunk(chunk, context));
            context.state = ILBMLoadingContext::State::BitmapDecoded;
        } else if (chunk.type == FourCC("CRNG")) {
            dbgln_if(ILBM_DEBUG, "Chunk:CRNG");
        } else if (chunk.type == FourCC("CAMG")) {
            context.viewport_mode = static_cast<ViewportMode>(AK::convert_between_host_and_big_endian(ByteReader::load32(chunk.data.data())));
            dbgln_if(ILBM_DEBUG, "Chunk:CAMG, Viewport={}, EHB={}, HAM={}", (u32)context.viewport_mode, has_flag(context.viewport_mode, ViewportMode::EHB), has_flag(context.viewport_mode, ViewportMode::HAM));
        }
    }

    if (context.state != ILBMLoadingContext::State::BitmapDecoded)
        return Error::from_string_literal("Missing body chunk");

    return {};
}

static ErrorOr<void> decode_bmhd_chunk(ILBMLoadingContext& context)
{
    context.chunks_cursor = context.data.slice(sizeof(IFFHeader));
    auto first_chunk = TRY(decode_iff_advance_chunk(context.chunks_cursor));

    if (first_chunk.type != FourCC("BMHD"))
        return Error::from_string_literal("IFFImageDecoderPlugin: Invalid chunk type, expected BMHD");

    if (first_chunk.data.size() < sizeof(BMHDHeader))
        return Error::from_string_literal("IFFImageDecoderPlugin: Not enough data for header chunk");

    context.bm_header = *bit_cast<BMHDHeader const*>(first_chunk.data.data());

    if (context.bm_header.planes > 8)
        return Error::from_string_literal("IFFImageDecoderPlugin: Deep ILBMs are not currently supported");

    if (context.bm_header.mask >= MaskType::__Count)
        return Error::from_string_literal("IFFImageDecoderPlugin: Unsupported mask type");

    if (context.bm_header.compression >= CompressionType::__Count)
        return Error::from_string_literal("IFFImageDecoderPlugin: Unsupported compression type");

    context.pitch = ceil_div((u16)context.bm_header.width, (u16)16) * 2;

    context.state = ILBMLoadingContext::State::HeaderDecoded;

    dbgln_if(ILBM_DEBUG, "IFFImageDecoderPlugin: BMHD: {}x{} ({},{}), p={}, m={}, c={}",
        context.bm_header.width,
        context.bm_header.height,
        context.bm_header.x,
        context.bm_header.y,
        context.bm_header.planes,
        to_underlying(context.bm_header.mask),
        to_underlying(context.bm_header.compression));

    return {};
}

ILBMImageDecoderPlugin::ILBMImageDecoderPlugin(ReadonlyBytes data, NonnullOwnPtr<ILBMLoadingContext> context)
    : m_context(move(context))
{
    m_context->data = data;
}

ILBMImageDecoderPlugin::~ILBMImageDecoderPlugin() = default;

IntSize ILBMImageDecoderPlugin::size()
{
    return IntSize { m_context->bm_header.width, m_context->bm_header.height };
}

bool ILBMImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    ILBMLoadingContext context;
    context.data = data;

    return !decode_iff_ilbm_header(context).is_error();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> ILBMImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto context = TRY(try_make<ILBMLoadingContext>());
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) ILBMImageDecoderPlugin(data, move(context))));
    TRY(decode_iff_ilbm_header(*plugin->m_context));
    TRY(decode_bmhd_chunk(*plugin->m_context));
    return plugin;
}

ErrorOr<ImageFrameDescriptor> ILBMImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("ILBMImageDecoderPlugin: frame index must be 0");

    if (m_context->state < ILBMLoadingContext::State::BitmapDecoded)
        TRY(decode_iff_chunks(*m_context));

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
