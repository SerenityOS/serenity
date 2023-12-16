/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Error.h>
#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <AK/Try.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/DDSLoader.h>
#include <fcntl.h>
#include <math.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Gfx {

struct DDSLoadingContext {
    DDSLoadingContext(FixedMemoryStream stream)
        : stream(move(stream))
    {
    }

    enum State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        BitmapDecoded,
    };

    State state { State::NotDecoded };

    FixedMemoryStream stream;

    DDSHeader header;
    DDSHeaderDXT10 header10;
    DXGIFormat format;
    RefPtr<Gfx::Bitmap> bitmap;

    void dump_debug();
};

static constexpr u32 create_four_cc(char c0, char c1, char c2, char c3)
{
    return c0 | c1 << 8 | c2 << 16 | c3 << 24;
}

static u64 get_width(DDSHeader header, size_t mipmap_level)
{
    if (mipmap_level >= header.mip_map_count) {
        return header.width;
    }

    return header.width >> mipmap_level;
}

static u64 get_height(DDSHeader header, size_t mipmap_level)
{
    if (mipmap_level >= header.mip_map_count) {
        return header.height;
    }

    return header.height >> mipmap_level;
}

static constexpr bool has_bitmask(DDSPixelFormat format, u32 r, u32 g, u32 b, u32 a)
{
    return format.r_bit_mask == r && format.g_bit_mask == g && format.b_bit_mask == b && format.a_bit_mask == a;
}

static DXGIFormat get_format(DDSPixelFormat format)
{
    if ((format.flags & PixelFormatFlags::DDPF_RGB) == PixelFormatFlags::DDPF_RGB) {
        switch (format.rgb_bit_count) {
        case 32: {
            if (has_bitmask(format, 0x000000FF, 0x0000FF00, 0x00FF0000, 0xFF000000))
                return DXGI_FORMAT_R8G8B8A8_UNORM;
            if (has_bitmask(format, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000))
                return DXGI_FORMAT_B8G8R8A8_UNORM;
            if (has_bitmask(format, 0x00FF0000, 0x0000FF00, 0x000000FF, 0x00000000))
                return DXGI_FORMAT_B8G8R8X8_UNORM;
            if (has_bitmask(format, 0x3FF00000, 0x000FFC00, 0x000003FF, 0xC0000000))
                return DXGI_FORMAT_R10G10B10A2_UNORM;
            if (has_bitmask(format, 0x0000FFFF, 0xFFFF0000, 0x00000000, 0x00000000))
                return DXGI_FORMAT_R16G16_UNORM;
            if (has_bitmask(format, 0xFFFFFFFF, 0x00000000, 0x00000000, 0x00000000))
                return DXGI_FORMAT_R32_FLOAT;
            break;
        }
        case 24:
            break;
        case 16: {
            if (has_bitmask(format, 0x7C00, 0x03E0, 0x001F, 0x8000))
                return DXGI_FORMAT_B5G5R5A1_UNORM;
            if (has_bitmask(format, 0xF800, 0x07E0, 0x001F, 0x0000))
                return DXGI_FORMAT_B5G6R5_UNORM;
            if (has_bitmask(format, 0xF800, 0x07E0, 0x001F, 0x0000))
                return DXGI_FORMAT_B5G6R5_UNORM;
            if (has_bitmask(format, 0x0F00, 0x00F0, 0x000F, 0xF000))
                return DXGI_FORMAT_B4G4R4A4_UNORM;
            if (has_bitmask(format, 0x00FF, 0x0000, 0x0000, 0xFF00))
                return DXGI_FORMAT_R8G8_UNORM;
            if (has_bitmask(format, 0xFFFF, 0x0000, 0x0000, 0x0000))
                return DXGI_FORMAT_R16_UNORM;
            break;
        }
        case 8: {
            if (has_bitmask(format, 0xFF, 0x00, 0x00, 0x00))
                return DXGI_FORMAT_R8_UNORM;
            break;
        }
        }
    } else if ((format.flags & PixelFormatFlags::DDPF_LUMINANCE) == PixelFormatFlags::DDPF_LUMINANCE) {
        switch (format.rgb_bit_count) {
        case 16: {
            if (has_bitmask(format, 0xFFFF, 0x0000, 0x0000, 0x0000))
                return DXGI_FORMAT_R16_UNORM;
            if (has_bitmask(format, 0x00FF, 0x0000, 0x0000, 0xFF00))
                return DXGI_FORMAT_R8G8_UNORM;
            break;
        }
        case 8: {
            if (has_bitmask(format, 0xFF, 0x00, 0x00, 0x00))
                return DXGI_FORMAT_R8_UNORM;

            // Some writers mistakenly write this as 8 bpp.
            if (has_bitmask(format, 0x00FF, 0x0000, 0x0000, 0xFF00))
                return DXGI_FORMAT_R8G8_UNORM;
            break;
        }
        }
    } else if ((format.flags & PixelFormatFlags::DDPF_ALPHA) == PixelFormatFlags::DDPF_ALPHA) {
        if (format.rgb_bit_count == 8)
            return DXGI_FORMAT_A8_UNORM;
    } else if ((format.flags & PixelFormatFlags::DDPF_BUMPDUDV) == PixelFormatFlags::DDPF_BUMPDUDV) {
        switch (format.rgb_bit_count) {
        case 32: {
            if (has_bitmask(format, 0x000000FF, 0x0000FF00, 0x00FF0000, 0xFF000000))
                return DXGI_FORMAT_R8G8B8A8_SNORM;
            if (has_bitmask(format, 0x0000FFFF, 0xFFFF0000, 0x00000000, 0x00000000))
                return DXGI_FORMAT_R16G16_SNORM;
            break;
        }
        case 16: {
            if (has_bitmask(format, 0x00FF, 0xFF00, 0x0000, 0x0000))
                return DXGI_FORMAT_R8G8_SNORM;
            break;
        }
        }
    } else if ((format.flags & PixelFormatFlags::DDPF_FOURCC) == PixelFormatFlags::DDPF_FOURCC) {
        if (format.four_cc == create_four_cc('D', 'X', 'T', '1'))
            return DXGI_FORMAT_BC1_UNORM;
        if (format.four_cc == create_four_cc('D', 'X', 'T', '2'))
            return DXGI_FORMAT_BC2_UNORM;
        if (format.four_cc == create_four_cc('D', 'X', 'T', '3'))
            return DXGI_FORMAT_BC2_UNORM;
        if (format.four_cc == create_four_cc('D', 'X', 'T', '4'))
            return DXGI_FORMAT_BC3_UNORM;
        if (format.four_cc == create_four_cc('D', 'X', 'T', '5'))
            return DXGI_FORMAT_BC3_UNORM;
        if (format.four_cc == create_four_cc('A', 'T', 'I', '1'))
            return DXGI_FORMAT_BC4_UNORM;
        if (format.four_cc == create_four_cc('B', 'C', '4', 'U'))
            return DXGI_FORMAT_BC4_UNORM;
        if (format.four_cc == create_four_cc('B', 'C', '4', 'S'))
            return DXGI_FORMAT_BC4_SNORM;
        if (format.four_cc == create_four_cc('A', 'T', 'I', '2'))
            return DXGI_FORMAT_BC5_UNORM;
        if (format.four_cc == create_four_cc('B', 'C', '5', 'U'))
            return DXGI_FORMAT_BC5_UNORM;
        if (format.four_cc == create_four_cc('B', 'C', '5', 'S'))
            return DXGI_FORMAT_BC5_SNORM;
        if (format.four_cc == create_four_cc('R', 'G', 'B', 'G'))
            return DXGI_FORMAT_R8G8_B8G8_UNORM;
        if (format.four_cc == create_four_cc('G', 'R', 'G', 'B'))
            return DXGI_FORMAT_G8R8_G8B8_UNORM;
        if (format.four_cc == create_four_cc('Y', 'U', 'Y', '2'))
            return DXGI_FORMAT_YUY2;

        switch (format.four_cc) {
        case 36:
            return DXGI_FORMAT_R16G16B16A16_UNORM;
        case 110:
            return DXGI_FORMAT_R16G16B16A16_SNORM;
        case 111:
            return DXGI_FORMAT_R16_FLOAT;
        case 112:
            return DXGI_FORMAT_R16G16_FLOAT;
        case 113:
            return DXGI_FORMAT_R16G16B16A16_FLOAT;
        case 114:
            return DXGI_FORMAT_R32_FLOAT;
        case 115:
            return DXGI_FORMAT_R32G32_FLOAT;
        case 116:
            return DXGI_FORMAT_R32G32B32A32_FLOAT;
        }
    }

    return DXGI_FORMAT_UNKNOWN;
}

static ErrorOr<void> decode_dx5_alpha_block(Stream& stream, DDSLoadingContext& context, u64 bitmap_x, u64 bitmap_y)
{
    auto color0 = TRY(stream.read_value<LittleEndian<u8>>());
    auto color1 = TRY(stream.read_value<LittleEndian<u8>>());

    auto code0 = TRY(stream.read_value<LittleEndian<u8>>());
    auto code1 = TRY(stream.read_value<LittleEndian<u8>>());
    auto code2 = TRY(stream.read_value<LittleEndian<u8>>());
    auto code3 = TRY(stream.read_value<LittleEndian<u8>>());
    auto code4 = TRY(stream.read_value<LittleEndian<u8>>());
    auto code5 = TRY(stream.read_value<LittleEndian<u8>>());

    u32 codes[6] = { 0 };
    codes[0] = code0 + 256 * (code1 + 256);
    codes[1] = code1 + 256 * (code2 + 256);
    codes[2] = code2 + 256 * (code3 + 256);
    codes[3] = code3 + 256 * (code4 + 256);
    codes[4] = code4 + 256 * code5;
    codes[5] = code5;

    u32 color[8] = { 0 };

    if (color0 > 128) {
        color[0] = color0;
    }

    if (color1 > 128) {
        color[1] = color1;
    }

    if (color0 > color1) {
        color[2] = (6 * color[0] + 1 * color[1]) / 7;
        color[3] = (5 * color[0] + 2 * color[1]) / 7;
        color[4] = (4 * color[0] + 3 * color[1]) / 7;
        color[5] = (3 * color[0] + 4 * color[1]) / 7;
        color[6] = (2 * color[0] + 5 * color[1]) / 7;
        color[7] = (1 * color[0] + 6 * color[1]) / 7;
    } else {
        color[2] = (4 * color[0] + 1 * color[1]) / 5;
        color[3] = (3 * color[0] + 2 * color[1]) / 5;
        color[4] = (2 * color[0] + 3 * color[1]) / 5;
        color[5] = (1 * color[0] + 4 * color[1]) / 5;
        color[6] = 0;
        color[7] = 255;
    }

    for (size_t y = 0; y < 4 && bitmap_y + y < static_cast<u64>(context.bitmap->height()); y++) {
        for (size_t x = 0; x < 4 && bitmap_x + x < static_cast<u64>(context.bitmap->width()); x++) {
            u8 index = 3 * (4 * y + x);
            u8 bit_location = floor(index / 8.0);
            u8 adjusted_index = index - (bit_location * 8);

            u8 code = (codes[bit_location] >> adjusted_index) & 7;
            u8 alpha = color[code];

            Color color = Color(0, 0, 0, alpha);
            context.bitmap->set_pixel(bitmap_x + x, bitmap_y + y, color);
        }
    }

    return {};
}

static ErrorOr<void> decode_dx3_alpha_block(Stream& stream, DDSLoadingContext& context, u64 bitmap_x, u64 bitmap_y)
{
    auto a0 = TRY(stream.read_value<LittleEndian<u8>>());
    auto a1 = TRY(stream.read_value<LittleEndian<u8>>());
    auto a2 = TRY(stream.read_value<LittleEndian<u8>>());
    auto a3 = TRY(stream.read_value<LittleEndian<u8>>());
    auto a4 = TRY(stream.read_value<LittleEndian<u8>>());
    auto a5 = TRY(stream.read_value<LittleEndian<u8>>());
    auto a6 = TRY(stream.read_value<LittleEndian<u8>>());
    auto a7 = TRY(stream.read_value<LittleEndian<u8>>());

    u64 alpha_0 = a0 + 256u * (a1 + 256u * (a2 + 256u * (a3 + 256u)));
    u64 alpha_1 = a4 + 256u * (a5 + 256u * (a6 + 256u * a7));

    for (size_t y = 0; y < 4 && bitmap_y + y < static_cast<u64>(context.bitmap->height()); y++) {
        for (size_t x = 0; x < 4 && bitmap_x + x < static_cast<u64>(context.bitmap->width()); x++) {
            u8 code = 4 * (4 * y + x);

            if (code >= 32) {
                code = code - 32;
                u8 alpha = ((alpha_1 >> code) & 0x0F) * 17;

                Color color = Color(0, 0, 0, alpha);
                context.bitmap->set_pixel(bitmap_x + x, bitmap_y + y, color);
            } else {
                u8 alpha = ((alpha_0 >> code) & 0x0F) * 17;

                Color color = Color(0, 0, 0, alpha);
                context.bitmap->set_pixel(bitmap_x + x, bitmap_y + y, color);
            }
        }
    }

    return {};
}

static void unpack_rbg_565(u32 rgb, u8* output)
{
    u8 r = (rgb >> 11) & 0x1F;
    u8 g = (rgb >> 5) & 0x3F;
    u8 b = rgb & 0x1F;

    output[0] = (r << 3) | (r >> 2);
    output[1] = (g << 2) | (g >> 4);
    output[2] = (b << 3) | (b >> 2);
    output[3] = 255;
}

static ErrorOr<void> decode_color_block(Stream& stream, DDSLoadingContext& context, bool dxt1, u64 bitmap_x, u64 bitmap_y)
{
    auto c0_low = TRY(stream.read_value<LittleEndian<u8>>());
    auto c0_high = TRY(stream.read_value<LittleEndian<u8>>());
    auto c1_low = TRY(stream.read_value<LittleEndian<u8>>());
    auto c1_high = TRY(stream.read_value<LittleEndian<u8>>());

    auto codes_0 = TRY(stream.read_value<LittleEndian<u8>>());
    auto codes_1 = TRY(stream.read_value<LittleEndian<u8>>());
    auto codes_2 = TRY(stream.read_value<LittleEndian<u8>>());
    auto codes_3 = TRY(stream.read_value<LittleEndian<u8>>());

    u64 code = codes_0 + 256ul * (codes_1 + 256ul * (codes_2 + 256ul * codes_3));
    u32 color_0 = c0_low + (c0_high * 256);
    u32 color_1 = c1_low + (c1_high * 256);

    u8 rgba[4][4];
    unpack_rbg_565(color_0, rgba[0]);
    unpack_rbg_565(color_1, rgba[1]);

    if (color_0 > color_1) {
        for (size_t i = 0; i < 3; i++) {
            rgba[2][i] = (2 * rgba[0][i] + rgba[1][i]) / 3;
            rgba[3][i] = (rgba[0][i] + 2 * rgba[1][i]) / 3;
        }

        rgba[2][3] = 255;
        rgba[3][3] = 255;
    } else {
        for (size_t i = 0; i < 3; i++) {
            rgba[2][i] = (rgba[0][i] + rgba[1][i]) / 2;
            rgba[3][i] = 0;
        }

        rgba[2][3] = 255;
        rgba[3][3] = dxt1 ? 0 : 255;
    }

    size_t i = 0;
    for (size_t y = 0; y < 4 && bitmap_y + y < static_cast<u64>(context.bitmap->height()); y++) {
        for (size_t x = 0; x < 4 && bitmap_x + x < static_cast<u64>(context.bitmap->width()); x++) {
            u8 code_byte = (code >> (i * 2)) & 3;
            u8 r = rgba[code_byte][0];
            u8 g = rgba[code_byte][1];
            u8 b = rgba[code_byte][2];
            u8 a = dxt1 ? rgba[code_byte][3] : context.bitmap->get_pixel(bitmap_x + x, bitmap_y + y).alpha();

            Color color = Color(r, g, b, a);
            context.bitmap->set_pixel(bitmap_x + x, bitmap_y + y, color);
            i++;
        }
    }

    return {};
}

static ErrorOr<void> decode_dxt(Stream& stream, DDSLoadingContext& context, u64 width, u64 y)
{
    if (context.format == DXGI_FORMAT_BC1_UNORM) {
        for (size_t x = 0; x < width; x += 4) {
            TRY(decode_color_block(stream, context, true, x, y));
        }
    }

    if (context.format == DXGI_FORMAT_BC2_UNORM) {
        for (size_t x = 0; x < width; x += 4) {
            TRY(decode_dx3_alpha_block(stream, context, x, y));
            TRY(decode_color_block(stream, context, false, x, y));
        }
    }

    if (context.format == DXGI_FORMAT_BC3_UNORM) {
        for (size_t x = 0; x < width; x += 4) {
            TRY(decode_dx5_alpha_block(stream, context, x, y));
            TRY(decode_color_block(stream, context, false, x, y));
        }
    }

    return {};
}
static ErrorOr<void> decode_bitmap(Stream& stream, DDSLoadingContext& context, u64 width, u64 height)
{
    static constexpr Array dxt_formats = { DXGI_FORMAT_BC1_UNORM, DXGI_FORMAT_BC2_UNORM, DXGI_FORMAT_BC3_UNORM };
    if (dxt_formats.contains_slow(context.format)) {
        for (u64 y = 0; y < height; y += 4) {
            TRY(decode_dxt(stream, context, width, y));
        }
    }

    // FIXME: Support more encodings (ATI, YUV, RAW, etc...).
    return {};
}

static ErrorOr<void> decode_header(DDSLoadingContext& context)
{
    // All valid DDS files are at least 128 bytes long.
    if (TRY(context.stream.size()) < 128) {
        dbgln_if(DDS_DEBUG, "File is too short for DDS");
        context.state = DDSLoadingContext::State::Error;
        return Error::from_string_literal("File is too short for DDS");
    }

    auto magic = TRY(context.stream.read_value<u32>());

    if (magic != create_four_cc('D', 'D', 'S', ' ')) {
        dbgln_if(DDS_DEBUG, "Missing magic number");
        context.state = DDSLoadingContext::State::Error;
        return Error::from_string_literal("Missing magic number");
    }

    context.header = TRY(context.stream.read_value<DDSHeader>());

    if (context.header.size != 124) {
        dbgln_if(DDS_DEBUG, "Header size is malformed");
        context.state = DDSLoadingContext::State::Error;
        return Error::from_string_literal("Header size is malformed");
    }
    if (context.header.pixel_format.size != 32) {
        dbgln_if(DDS_DEBUG, "Pixel format size is malformed");
        context.state = DDSLoadingContext::State::Error;
        return Error::from_string_literal("Pixel format size is malformed");
    }

    if ((context.header.pixel_format.flags & PixelFormatFlags::DDPF_FOURCC) == PixelFormatFlags::DDPF_FOURCC) {
        if (context.header.pixel_format.four_cc == create_four_cc('D', 'X', '1', '0')) {
            if (TRY(context.stream.size()) < 148) {
                dbgln_if(DDS_DEBUG, "DX10 header is too short");
                context.state = DDSLoadingContext::State::Error;
                return Error::from_string_literal("DX10 header is too short");
            }

            context.header10 = TRY(context.stream.read_value<DDSHeaderDXT10>());
        }
    }

    if constexpr (DDS_DEBUG) {
        context.dump_debug();
    }

    context.format = get_format(context.header.pixel_format);

    static constexpr Array supported_formats = { DXGI_FORMAT_BC1_UNORM, DXGI_FORMAT_BC2_UNORM, DXGI_FORMAT_BC3_UNORM };
    if (!supported_formats.contains_slow(context.format)) {
        dbgln_if(DDS_DEBUG, "Format of type {} is not supported at the moment", to_underlying(context.format));
        context.state = DDSLoadingContext::State::Error;
        return Error::from_string_literal("Format type is not supported at the moment");
    }

    context.state = DDSLoadingContext::HeaderDecoded;

    return {};
}

static ErrorOr<void> decode_dds(DDSLoadingContext& context)
{
    VERIFY(context.state == DDSLoadingContext::HeaderDecoded);

    // We support parsing mipmaps, but we only care about the largest one :^) (At least for now)
    if (size_t mipmap_level = 0; mipmap_level < max(context.header.mip_map_count, 1u)) {
        u64 width = get_width(context.header, mipmap_level);
        u64 height = get_height(context.header, mipmap_level);

        context.bitmap = TRY(Bitmap::create(BitmapFormat::BGRA8888, { width, height }));

        TRY(decode_bitmap(context.stream, context, width, height));
    }

    context.state = DDSLoadingContext::State::BitmapDecoded;

    return {};
}

void DDSLoadingContext::dump_debug()
{
    StringBuilder builder;

    builder.append("\nDDS:\n"sv);
    builder.appendff("\tHeader Size: {}\n", header.size);

    builder.append("\tFlags:"sv);
    if ((header.flags & DDSFlags::DDSD_CAPS) == DDSFlags::DDSD_CAPS)
        builder.append(" DDSD_CAPS"sv);
    if ((header.flags & DDSFlags::DDSD_HEIGHT) == DDSFlags::DDSD_HEIGHT)
        builder.append(" DDSD_HEIGHT"sv);
    if ((header.flags & DDSFlags::DDSD_WIDTH) == DDSFlags::DDSD_WIDTH)
        builder.append(" DDSD_WIDTH"sv);
    if ((header.flags & DDSFlags::DDSD_PITCH) == DDSFlags::DDSD_PITCH)
        builder.append(" DDSD_PITCH"sv);
    if ((header.flags & DDSFlags::DDSD_PIXELFORMAT) == DDSFlags::DDSD_PIXELFORMAT)
        builder.append(" DDSD_PIXELFORMAT"sv);
    if ((header.flags & DDSFlags::DDSD_MIPMAPCOUNT) == DDSFlags::DDSD_MIPMAPCOUNT)
        builder.append(" DDSD_MIPMAPCOUNT"sv);
    if ((header.flags & DDSFlags::DDSD_LINEARSIZE) == DDSFlags::DDSD_LINEARSIZE)
        builder.append(" DDSD_LINEARSIZE"sv);
    if ((header.flags & DDSFlags::DDSD_DEPTH) == DDSFlags::DDSD_DEPTH)
        builder.append(" DDSD_DEPTH"sv);
    builder.append("\n"sv);

    builder.appendff("\tHeight: {}\n", header.height);
    builder.appendff("\tWidth: {}\n", header.width);
    builder.appendff("\tPitch: {}\n", header.pitch);
    builder.appendff("\tDepth: {}\n", header.depth);
    builder.appendff("\tMipmap Count: {}\n", header.mip_map_count);

    builder.append("\tCaps:"sv);
    if ((header.caps1 & Caps1Flags::DDSCAPS_COMPLEX) == Caps1Flags::DDSCAPS_COMPLEX)
        builder.append(" DDSCAPS_COMPLEX"sv);
    if ((header.caps1 & Caps1Flags::DDSCAPS_MIPMAP) == Caps1Flags::DDSCAPS_MIPMAP)
        builder.append(" DDSCAPS_MIPMAP"sv);
    if ((header.caps1 & Caps1Flags::DDSCAPS_TEXTURE) == Caps1Flags::DDSCAPS_TEXTURE)
        builder.append(" DDSCAPS_TEXTURE"sv);
    builder.append("\n"sv);

    builder.append("\tCaps2:"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP) == Caps2Flags::DDSCAPS2_CUBEMAP)
        builder.append(" DDSCAPS2_CUBEMAP"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEX) == Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEX)
        builder.append(" DDSCAPS2_CUBEMAP_POSITIVEX"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEX) == Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEX)
        builder.append(" DDSCAPS2_CUBEMAP_NEGATIVEX"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEY) == Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEY)
        builder.append(" DDSCAPS2_CUBEMAP_POSITIVEY"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEY) == Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEY)
        builder.append(" DDSCAPS2_CUBEMAP_NEGATIVEY"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEZ) == Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEZ)
        builder.append(" DDSCAPS2_CUBEMAP_POSITIVEZ"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEZ) == Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEZ)
        builder.append(" DDSCAPS2_CUBEMAP_NEGATIVEZ"sv);
    if ((header.caps2 & Caps2Flags::DDSCAPS2_VOLUME) == Caps2Flags::DDSCAPS2_VOLUME)
        builder.append(" DDSCAPS2_VOLUME"sv);
    builder.append("\n"sv);

    builder.append("Pixel Format:\n"sv);
    builder.appendff("\tStruct Size: {}\n", header.pixel_format.size);

    builder.append("\tFlags:"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_ALPHAPIXELS) == PixelFormatFlags::DDPF_ALPHAPIXELS)
        builder.append(" DDPF_ALPHAPIXELS"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_ALPHA) == PixelFormatFlags::DDPF_ALPHA)
        builder.append(" DDPF_ALPHA"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_FOURCC) == PixelFormatFlags::DDPF_FOURCC)
        builder.append(" DDPF_FOURCC"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_PALETTEINDEXED8) == PixelFormatFlags::DDPF_PALETTEINDEXED8)
        builder.append(" DDPF_PALETTEINDEXED8"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_RGB) == PixelFormatFlags::DDPF_RGB)
        builder.append(" DDPF_RGB"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_YUV) == PixelFormatFlags::DDPF_YUV)
        builder.append(" DDPF_YUV"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_LUMINANCE) == PixelFormatFlags::DDPF_LUMINANCE)
        builder.append(" DDPF_LUMINANCE"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_BUMPDUDV) == PixelFormatFlags::DDPF_BUMPDUDV)
        builder.append(" DDPF_BUMPDUDV"sv);
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_NORMAL) == PixelFormatFlags::DDPF_NORMAL)
        builder.append(" DDPF_NORMAL"sv);
    builder.append("\n"sv);

    builder.append("\tFour CC: "sv);
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 0)) & 0xFF);
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 1)) & 0xFF);
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 2)) & 0xFF);
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 3)) & 0xFF);
    builder.append("\n"sv);
    builder.appendff("\tRGB Bit Count: {}\n", header.pixel_format.rgb_bit_count);
    builder.appendff("\tR Bit Mask: {}\n", header.pixel_format.r_bit_mask);
    builder.appendff("\tG Bit Mask: {}\n", header.pixel_format.g_bit_mask);
    builder.appendff("\tB Bit Mask: {}\n", header.pixel_format.b_bit_mask);
    builder.appendff("\tA Bit Mask: {}\n", header.pixel_format.a_bit_mask);

    builder.append("DDS10:\n"sv);
    builder.appendff("\tFormat: {}\n", static_cast<u32>(header10.format));

    builder.append("\tResource Dimension:"sv);
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_UNKNOWN) == ResourceDimensions::DDS_DIMENSION_UNKNOWN)
        builder.append(" DDS_DIMENSION_UNKNOWN"sv);
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_BUFFER) == ResourceDimensions::DDS_DIMENSION_BUFFER)
        builder.append(" DDS_DIMENSION_BUFFER"sv);
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_TEXTURE1D) == ResourceDimensions::DDS_DIMENSION_TEXTURE1D)
        builder.append(" DDS_DIMENSION_TEXTURE1D"sv);
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_TEXTURE2D) == ResourceDimensions::DDS_DIMENSION_TEXTURE2D)
        builder.append(" DDS_DIMENSION_TEXTURE2D"sv);
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_TEXTURE3D) == ResourceDimensions::DDS_DIMENSION_TEXTURE3D)
        builder.append(" DDS_DIMENSION_TEXTURE3D"sv);
    builder.append("\n"sv);

    builder.appendff("\tArray Size: {}\n", header10.array_size);

    builder.append("\tMisc Flags:"sv);
    if ((header10.misc_flag & MiscFlags::DDS_RESOURCE_MISC_TEXTURECUBE) == MiscFlags::DDS_RESOURCE_MISC_TEXTURECUBE)
        builder.append(" DDS_RESOURCE_MISC_TEXTURECUBE"sv);
    builder.append("\n"sv);

    builder.append("\tMisc Flags 2:"sv);
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_UNKNOWN) == Misc2Flags::DDS_ALPHA_MODE_UNKNOWN)
        builder.append(" DDS_ALPHA_MODE_UNKNOWN"sv);
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_STRAIGHT) == Misc2Flags::DDS_ALPHA_MODE_STRAIGHT)
        builder.append(" DDS_ALPHA_MODE_STRAIGHT"sv);
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_PREMULTIPLIED) == Misc2Flags::DDS_ALPHA_MODE_PREMULTIPLIED)
        builder.append(" DDS_ALPHA_MODE_PREMULTIPLIED"sv);
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_OPAQUE) == Misc2Flags::DDS_ALPHA_MODE_OPAQUE)
        builder.append(" DDS_ALPHA_MODE_OPAQUE"sv);
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_CUSTOM) == Misc2Flags::DDS_ALPHA_MODE_CUSTOM)
        builder.append(" DDS_ALPHA_MODE_CUSTOM"sv);
    builder.append("\n"sv);

    dbgln("{}", builder.to_byte_string());
}

DDSImageDecoderPlugin::DDSImageDecoderPlugin(FixedMemoryStream stream)
{
    m_context = make<DDSLoadingContext>(move(stream));
}

DDSImageDecoderPlugin::~DDSImageDecoderPlugin() = default;

IntSize DDSImageDecoderPlugin::size()
{
    return { m_context->header.width, m_context->header.height };
}

bool DDSImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    // The header is always at least 128 bytes, so if the file is smaller, it can't be a DDS.
    return data.size() > 128
        && data.data()[0] == 0x44
        && data.data()[1] == 0x44
        && data.data()[2] == 0x53
        && data.data()[3] == 0x20;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> DDSImageDecoderPlugin::create(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DDSImageDecoderPlugin(move(stream))));
    TRY(decode_header(*plugin->m_context));
    return plugin;
}

ErrorOr<ImageFrameDescriptor> DDSImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("DDSImageDecoderPlugin: Invalid frame index");

    if (m_context->state == DDSLoadingContext::State::Error)
        return Error::from_string_literal("DDSImageDecoderPlugin: Decoding failed");

    if (m_context->state < DDSLoadingContext::State::BitmapDecoded) {
        TRY(decode_dds(*m_context));
    }

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
