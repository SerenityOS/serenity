/*
 * Copyright (c) 2023, Nicolas Ramz <nicolas.ramz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/FixedArray.h>
#include <AK/IntegralMath.h>
#include <LibCompress/PackBitsDecoder.h>
#include <LibGfx/FourCC.h>
#include <LibGfx/ImageFormats/ILBMLoader.h>
#include <LibRIFF/IFF.h>

namespace Gfx {

static constexpr size_t const ilbm_header_size = 12;

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

enum class Format : u8 {
    // Amiga interleaved format
    ILBM = 0,
    // PC-DeluxePaint chunky format
    PBM = 1
};

AK_ENUM_BITWISE_OPERATORS(ViewportMode);

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

    // number of bits needed to describe current palette
    u8 cmap_bits;

    RefPtr<Gfx::Bitmap> bitmap;

    BMHDHeader bm_header;

    Format format;
};

static ErrorOr<void> decode_iff_ilbm_header(ILBMLoadingContext& context)
{
    if (context.state >= ILBMLoadingContext::State::HeaderDecoded)
        return {};

    if (context.data.size() < ilbm_header_size)
        return Error::from_string_literal("Missing IFF header");

    auto header_stream = FixedMemoryStream { context.data };
    auto header = TRY(IFF::FileHeader::read_from_stream(header_stream));
    if (header.magic() != "FORM"sv || (header.subformat != "ILBM"sv && header.subformat != "PBM "sv))
        return Error::from_string_literal("Invalid IFF-ILBM header");

    context.format = header.subformat == "ILBM" ? Format::ILBM : Format::PBM;

    return {};
}

static ErrorOr<Vector<Color>> decode_cmap_chunk(IFF::Chunk cmap_chunk)
{
    size_t const size = cmap_chunk.size() / 3;
    Vector<Color> color_table;
    TRY(color_table.try_ensure_capacity(size));

    for (size_t i = 0; i < size; ++i) {
        color_table.unchecked_append(Color(cmap_chunk[i * 3], cmap_chunk[(i * 3) + 1], cmap_chunk[(i * 3) + 2]));
    }

    return color_table;
}

static ErrorOr<RefPtr<Gfx::Bitmap>> chunky_to_bitmap(ILBMLoadingContext& context, ByteBuffer const& chunky)
{
    auto const width = context.bm_header.width;
    auto const height = context.bm_header.height;

    RefPtr<Gfx::Bitmap> bitmap = TRY(Bitmap::create(BitmapFormat::BGRA8888, { width, height }));

    dbgln_if(ILBM_DEBUG, "created Bitmap {}x{}", width, height);

    // - For 24bit pictures: the chunky buffer contains 3 bytes (R,G,B) per pixel
    // - For indexed colored pictures: chunky buffer contains a single byte per pixel
    u8 pixel_size = AK::max(1, context.bm_header.planes / 8);

    for (int row = 0; row < height; ++row) {
        // Keep color: in HAM mode, current color
        // may be based on previous color instead of coming from
        // the palette.
        Color color = Color::Black;
        for (int col = 0; col < width; col++) {
            size_t index = (width * row * pixel_size) + (col * pixel_size);
            if (context.bm_header.planes == 24) {
                color = Color(chunky[index], chunky[index + 1], chunky[index + 2]);
            } else if (chunky[index] < context.color_table.size()) {
                color = context.color_table[chunky[index]];
                if (context.bm_header.mask == MaskType::HasTransparentColor && chunky[index] == context.bm_header.transparent_color)
                    color = color.with_alpha(0);
            } else if (has_flag(context.viewport_mode, ViewportMode::HAM)) {
                // Get the control bit which will tell use how current pixel should be calculated
                u8 control = (chunky[index] >> context.cmap_bits) & 0x3;
                // Since we only have (cmap_bits - 2) bits to define the component,
                // we need to pad it to 8 bits.
                u8 component = (chunky[index] % context.color_table.size()) << (8 - context.cmap_bits);

                if (control == 1) {
                    color.set_blue(component);
                } else if (control == 2) {
                    color.set_red(component);
                } else {
                    color.set_green(component);
                }
            } else {
                return Error::from_string_literal("Color map index out of bounds but HAM bit not set");
            }
            bitmap->set_pixel(col, row, color);
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
    // mask is added as an extra plane
    u8 planes = context.bm_header.mask == MaskType::HasMask ? context.bm_header.planes + 1 : context.bm_header.planes;
    size_t buffer_size = static_cast<size_t>(width) * height;
    // If planes number is 24 we'll store R,G,B components so buffer needs to be 3 times width*height
    // otherwise we'll store a single 8bit index to the CMAP.
    if (planes == 24)
        buffer_size *= 3;

    auto chunky = TRY(ByteBuffer::create_zeroed(buffer_size));

    u8 const pixel_size = AK::max(1, planes / 8);

    for (u16 y = 0; y < height; y++) {
        size_t scanline = static_cast<size_t>(y) * width;
        for (u8 p = 0; p < planes; p++) {
            u8 const plane_mask = 1 << (p % 8);
            size_t offset_base = (pitch * planes * y) + (p * pitch);
            if (offset_base + pitch > bitplanes.size())
                return Error::from_string_literal("Malformed bitplane data");

            for (u16 i = 0; i < pitch; i++) {
                u8 bit = bitplanes[offset_base + i];
                u8 rgb_shift = p / 8;

                // Some encoders don't pad bytes rows with 0: make sure we stop
                // when enough data for current bitplane row has been read
                for (u8 b = 0; b < 8 && (i * 8) + b < width; b++) {
                    u8 mask = 1 << (7 - b);
                    // get current plane: simply skip mask plane for now
                    if (bit & mask && p < context.bm_header.planes) {
                        u16 x = (i * 8) + b;
                        size_t offset = (scanline * pixel_size) + (x * pixel_size) + rgb_shift;
                        // Only throw an error if we would actually attempt to write
                        // outside of the chunky buffer. Some apps like PPaint produce
                        // malformed bitplane data but files are still accepted by most readers
                        // since they do not cause writing past the chunky buffer.
                        if (offset >= chunky.size())
                            return Error::from_string_literal("Malformed bitplane data");

                        chunky[offset] |= plane_mask;
                    }
                }
            }
        }
    }

    dbgln_if(ILBM_DEBUG, "planar_to_chunky: end");

    return chunky;
}

static ErrorOr<ByteBuffer> uncompress_byte_run(ReadonlyBytes data, ILBMLoadingContext& context)
{
    auto length = data.size();
    dbgln_if(ILBM_DEBUG, "uncompress_byte_run pitch={} size={}", context.pitch, data.size());

    size_t plane_data_size = context.pitch * context.bm_header.height * context.bm_header.planes;

    // The mask is encoded as an extra bitplane but is not counted in the bm_header planes
    if (context.bm_header.mask == MaskType::HasMask)
        plane_data_size += context.pitch * context.bm_header.height;

    // The maximum run length of this compression method is 127 bytes, so the uncompressed size
    // cannot be more than 127 times the size of the chunk we are decompressing.
    if (plane_data_size > NumericLimits<u32>::max() || ceil_div(plane_data_size, 127ul) > length)
        return Error::from_string_literal("Uncompressed data size too large");

    auto plane_data = TRY(Compress::PackBits::decode_all(data, plane_data_size));

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

static ErrorOr<void> reduce_ham_palette(ILBMLoadingContext& context)
{
    u8 bits = context.cmap_bits;

    dbgln_if(ILBM_DEBUG, "reduce palette planes={} bits={}", context.bm_header.planes, context.cmap_bits);

    if (bits > context.bm_header.planes) {
        dbgln_if(ILBM_DEBUG, "need to reduce palette");
        bits -= (bits - context.bm_header.planes) + 2;
        // bits shouldn't theorically be less than 4 bits in HAM mode.
        if (bits < 4)
            return Error::from_string_literal("Error while reducing CMAP for HAM: bits too small");

        context.color_table.resize((context.color_table.size() >> bits));
        context.cmap_bits = bits;
    }

    return {};
}

static ErrorOr<void> decode_body_chunk(IFF::Chunk body_chunk, ILBMLoadingContext& context)
{
    dbgln_if(ILBM_DEBUG, "decode_body_chunk {}", body_chunk.size());

    ByteBuffer pixel_data;

    if (context.bm_header.compression == CompressionType::ByteRun) {
        auto plane_data = TRY(uncompress_byte_run(body_chunk.data(), context));
        if (context.format == Format::ILBM)
            pixel_data = TRY(planar_to_chunky(plane_data, context));
        else
            pixel_data = plane_data;
    } else {
        if (context.format == Format::ILBM)
            pixel_data = TRY(planar_to_chunky(body_chunk.data(), context));
        else
            pixel_data = TRY(ByteBuffer::copy(body_chunk.data().data(), body_chunk.size()));
    }

    // Some files already have 64 colors defined in the palette,
    // maybe for upward compatibility with 256 colors software/hardware.
    // DPaint 4 & previous files only have 32 colors so the
    // palette needs to be extended only for these files.
    if (has_flag(context.viewport_mode, ViewportMode::EHB) && context.color_table.size() < 64) {
        TRY(extend_ehb_palette(context));
    } else if (has_flag(context.viewport_mode, ViewportMode::HAM)) {
        TRY(reduce_ham_palette(context));
    }

    context.bitmap = TRY(chunky_to_bitmap(context, pixel_data));

    return {};
}

static ErrorOr<void> decode_iff_chunks(ILBMLoadingContext& context)
{
    auto& chunks = context.chunks_cursor;

    dbgln_if(ILBM_DEBUG, "decode_iff_chunks");

    while (!chunks.is_empty()) {
        auto chunk = TRY(IFF::Chunk::decode_and_advance(chunks));
        if (chunk.id() == "CMAP"sv) {
            // Some files (HAM mainly) have CMAP chunks larger than the planes they advertise: I'm not sure
            // why but we should not return an error in this case.

            context.color_table = TRY(decode_cmap_chunk(chunk));
            context.cmap_bits = AK::ceil_log2(context.color_table.size());
        } else if (chunk.id() == "BODY"sv) {
            if (context.color_table.is_empty() && context.bm_header.planes != 24)
                return Error::from_string_literal("Decoding indexed BODY chunk without a color map is not currently supported");

            // Apparently 32bit ilbm files exist: but I wasn't able to find any,
            // nor is it documented anywhere, so let's make it clear it's not supported.
            if (context.bm_header.planes != 24 && context.bm_header.planes > 8)
                return Error::from_string_literal("Invalid number of bitplanes");

            TRY(decode_body_chunk(chunk, context));
            context.state = ILBMLoadingContext::State::BitmapDecoded;
        } else if (chunk.id() == "CRNG"sv) {
            dbgln_if(ILBM_DEBUG, "Chunk:CRNG");
        } else if (chunk.id() == "CAMG"sv) {
            context.viewport_mode = static_cast<ViewportMode>(AK::convert_between_host_and_big_endian(ByteReader::load32(chunk.data().data())));
            dbgln_if(ILBM_DEBUG, "Chunk:CAMG, Viewport={}, EHB={}, HAM={}", (u32)context.viewport_mode, has_flag(context.viewport_mode, ViewportMode::EHB), has_flag(context.viewport_mode, ViewportMode::HAM));
        }
    }

    if (context.state != ILBMLoadingContext::State::BitmapDecoded)
        return Error::from_string_literal("Missing body chunk");

    return {};
}

static ErrorOr<void> decode_bmhd_chunk(ILBMLoadingContext& context)
{
    context.chunks_cursor = context.data.slice(sizeof(IFF::FileHeader));
    auto first_chunk = TRY(IFF::Chunk::decode_and_advance(context.chunks_cursor));

    if (first_chunk.id() != "BMHD"sv)
        return Error::from_string_literal("IFFImageDecoderPlugin: Invalid chunk type, expected BMHD");

    if (first_chunk.size() < sizeof(BMHDHeader))
        return Error::from_string_literal("IFFImageDecoderPlugin: Not enough data for header chunk");

    context.bm_header = *bit_cast<BMHDHeader const*>(first_chunk.data().data());

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
