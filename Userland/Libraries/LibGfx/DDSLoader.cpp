/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGfx/DDSLoader.h>
#include <fcntl.h>
#include <math.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#ifdef __serenity__
#    include <serenity.h>
#endif

namespace Gfx {

struct DDSLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        BitmapDecoded,
    };

    State state { State::NotDecoded };

    const u8* data { nullptr };
    size_t data_size { 0 };

    DDSHeader header;
    DDSHeaderDXT10 header10;
    RefPtr<Gfx::Bitmap> bitmap;

    void dump_debug();
};

static constexpr u32 create_four_cc(char c0, char c1, char c2, char c3)
{
    return c0 | c1 << 8 | c2 << 16 | c3 << 24;
}

static bool is_planar(DXGIFormat format)
{
    switch (format) {
    case DXGI_FORMAT_NV12:
    case DXGI_FORMAT_420_OPAQUE:
    case DXGI_FORMAT_P208:
    case DXGI_FORMAT_P010:
    case DXGI_FORMAT_P016:
        return true;
    default:
        return false;
    }
}

static bool is_packed(DXGIFormat format)
{
    switch (format) {
    case DXGI_FORMAT_R8G8_B8G8_UNORM:
    case DXGI_FORMAT_G8R8_G8B8_UNORM:
    case DXGI_FORMAT_YUY2:
    case DXGI_FORMAT_Y210:
    case DXGI_FORMAT_Y216:
        return true;
    default:
        return false;
    }
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

static bool is_block_compressed(DXGIFormat format)
{
    switch (format) {
    case DXGI_FORMAT_BC1_TYPELESS:
    case DXGI_FORMAT_BC1_UNORM:
    case DXGI_FORMAT_BC1_UNORM_SRGB:
    case DXGI_FORMAT_BC4_TYPELESS:
    case DXGI_FORMAT_BC4_UNORM:
    case DXGI_FORMAT_BC4_SNORM:
    case DXGI_FORMAT_BC2_TYPELESS:
    case DXGI_FORMAT_BC2_UNORM:
    case DXGI_FORMAT_BC2_UNORM_SRGB:
    case DXGI_FORMAT_BC3_TYPELESS:
    case DXGI_FORMAT_BC3_UNORM:
    case DXGI_FORMAT_BC3_UNORM_SRGB:
    case DXGI_FORMAT_BC5_TYPELESS:
    case DXGI_FORMAT_BC5_UNORM:
    case DXGI_FORMAT_BC5_SNORM:
    case DXGI_FORMAT_BC6H_TYPELESS:
    case DXGI_FORMAT_BC6H_UF16:
    case DXGI_FORMAT_BC6H_SF16:
    case DXGI_FORMAT_BC7_TYPELESS:
    case DXGI_FORMAT_BC7_UNORM:
    case DXGI_FORMAT_BC7_UNORM_SRGB:
        return true;

    default:
        return false;
    }
}

static size_t block_size(DXGIFormat format)
{
    switch (format) {
    case DXGI_FORMAT_BC2_TYPELESS:
    case DXGI_FORMAT_BC2_UNORM:
    case DXGI_FORMAT_BC2_UNORM_SRGB:
    case DXGI_FORMAT_BC3_TYPELESS:
    case DXGI_FORMAT_BC3_UNORM:
    case DXGI_FORMAT_BC3_UNORM_SRGB:
    case DXGI_FORMAT_BC5_TYPELESS:
    case DXGI_FORMAT_BC5_UNORM:
    case DXGI_FORMAT_BC5_SNORM:
    case DXGI_FORMAT_BC6H_TYPELESS:
    case DXGI_FORMAT_BC6H_UF16:
    case DXGI_FORMAT_BC6H_SF16:
    case DXGI_FORMAT_BC7_TYPELESS:
    case DXGI_FORMAT_BC7_UNORM:
    case DXGI_FORMAT_BC7_UNORM_SRGB:
        return 16;

    case DXGI_FORMAT_BC1_TYPELESS:
    case DXGI_FORMAT_BC1_UNORM:
    case DXGI_FORMAT_BC1_UNORM_SRGB:
    case DXGI_FORMAT_BC4_TYPELESS:
    case DXGI_FORMAT_BC4_UNORM:
    case DXGI_FORMAT_BC4_SNORM:
    case DXGI_FORMAT_Y210:
    case DXGI_FORMAT_Y216:
        return 8;

    case DXGI_FORMAT_R8G8_B8G8_UNORM:
    case DXGI_FORMAT_G8R8_G8B8_UNORM:
    case DXGI_FORMAT_YUY2:
    case DXGI_FORMAT_P010:
    case DXGI_FORMAT_P016:
        return 4;

    case DXGI_FORMAT_NV12:
    case DXGI_FORMAT_420_OPAQUE:
    case DXGI_FORMAT_P208:
        return 2;

    default:
        return 0;
    }
}

static size_t bits_per_pixel(DXGIFormat format)
{
    switch (format) {
    case DXGI_FORMAT_R32G32B32A32_TYPELESS:
    case DXGI_FORMAT_R32G32B32A32_FLOAT:
    case DXGI_FORMAT_R32G32B32A32_UINT:
    case DXGI_FORMAT_R32G32B32A32_SINT:
        return 128;

    case DXGI_FORMAT_R32G32B32_TYPELESS:
    case DXGI_FORMAT_R32G32B32_FLOAT:
    case DXGI_FORMAT_R32G32B32_UINT:
    case DXGI_FORMAT_R32G32B32_SINT:
        return 96;

    case DXGI_FORMAT_R16G16B16A16_TYPELESS:
    case DXGI_FORMAT_R16G16B16A16_FLOAT:
    case DXGI_FORMAT_R16G16B16A16_UNORM:
    case DXGI_FORMAT_R16G16B16A16_UINT:
    case DXGI_FORMAT_R16G16B16A16_SNORM:
    case DXGI_FORMAT_R16G16B16A16_SINT:
    case DXGI_FORMAT_R32G32_TYPELESS:
    case DXGI_FORMAT_R32G32_FLOAT:
    case DXGI_FORMAT_R32G32_UINT:
    case DXGI_FORMAT_R32G32_SINT:
    case DXGI_FORMAT_R32G8X24_TYPELESS:
    case DXGI_FORMAT_D32_FLOAT_S8X24_UINT:
    case DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS:
    case DXGI_FORMAT_X32_TYPELESS_G8X24_UINT:
    case DXGI_FORMAT_Y416:
    case DXGI_FORMAT_Y210:
    case DXGI_FORMAT_Y216:
        return 64;

    case DXGI_FORMAT_R10G10B10A2_TYPELESS:
    case DXGI_FORMAT_R10G10B10A2_UNORM:
    case DXGI_FORMAT_R10G10B10A2_UINT:
    case DXGI_FORMAT_R11G11B10_FLOAT:
    case DXGI_FORMAT_R8G8B8A8_TYPELESS:
    case DXGI_FORMAT_R8G8B8A8_UNORM:
    case DXGI_FORMAT_R8G8B8A8_UNORM_SRGB:
    case DXGI_FORMAT_R8G8B8A8_UINT:
    case DXGI_FORMAT_R8G8B8A8_SNORM:
    case DXGI_FORMAT_R8G8B8A8_SINT:
    case DXGI_FORMAT_R16G16_TYPELESS:
    case DXGI_FORMAT_R16G16_FLOAT:
    case DXGI_FORMAT_R16G16_UNORM:
    case DXGI_FORMAT_R16G16_UINT:
    case DXGI_FORMAT_R16G16_SNORM:
    case DXGI_FORMAT_R16G16_SINT:
    case DXGI_FORMAT_R32_TYPELESS:
    case DXGI_FORMAT_D32_FLOAT:
    case DXGI_FORMAT_R32_FLOAT:
    case DXGI_FORMAT_R32_UINT:
    case DXGI_FORMAT_R32_SINT:
    case DXGI_FORMAT_R24G8_TYPELESS:
    case DXGI_FORMAT_D24_UNORM_S8_UINT:
    case DXGI_FORMAT_R24_UNORM_X8_TYPELESS:
    case DXGI_FORMAT_X24_TYPELESS_G8_UINT:
    case DXGI_FORMAT_R9G9B9E5_SHAREDEXP:
    case DXGI_FORMAT_R8G8_B8G8_UNORM:
    case DXGI_FORMAT_G8R8_G8B8_UNORM:
    case DXGI_FORMAT_B8G8R8A8_UNORM:
    case DXGI_FORMAT_B8G8R8X8_UNORM:
    case DXGI_FORMAT_R10G10B10_XR_BIAS_A2_UNORM:
    case DXGI_FORMAT_B8G8R8A8_TYPELESS:
    case DXGI_FORMAT_B8G8R8A8_UNORM_SRGB:
    case DXGI_FORMAT_B8G8R8X8_TYPELESS:
    case DXGI_FORMAT_B8G8R8X8_UNORM_SRGB:
    case DXGI_FORMAT_AYUV:
    case DXGI_FORMAT_Y410:
    case DXGI_FORMAT_YUY2:
        return 32;

    case DXGI_FORMAT_P010:
    case DXGI_FORMAT_P016:
    case DXGI_FORMAT_V408:
        return 24;

    case DXGI_FORMAT_R8G8_TYPELESS:
    case DXGI_FORMAT_R8G8_UNORM:
    case DXGI_FORMAT_R8G8_UINT:
    case DXGI_FORMAT_R8G8_SNORM:
    case DXGI_FORMAT_R8G8_SINT:
    case DXGI_FORMAT_R16_TYPELESS:
    case DXGI_FORMAT_R16_FLOAT:
    case DXGI_FORMAT_D16_UNORM:
    case DXGI_FORMAT_R16_UNORM:
    case DXGI_FORMAT_R16_UINT:
    case DXGI_FORMAT_R16_SNORM:
    case DXGI_FORMAT_R16_SINT:
    case DXGI_FORMAT_B5G6R5_UNORM:
    case DXGI_FORMAT_B5G5R5A1_UNORM:
    case DXGI_FORMAT_A8P8:
    case DXGI_FORMAT_B4G4R4A4_UNORM:
    case DXGI_FORMAT_P208:
    case DXGI_FORMAT_V208:
        return 16;

    case DXGI_FORMAT_NV12:
    case DXGI_FORMAT_420_OPAQUE:
    case DXGI_FORMAT_NV11:
        return 12;

    case DXGI_FORMAT_R8_TYPELESS:
    case DXGI_FORMAT_R8_UNORM:
    case DXGI_FORMAT_R8_UINT:
    case DXGI_FORMAT_R8_SNORM:
    case DXGI_FORMAT_R8_SINT:
    case DXGI_FORMAT_A8_UNORM:
    case DXGI_FORMAT_BC2_TYPELESS:
    case DXGI_FORMAT_BC2_UNORM:
    case DXGI_FORMAT_BC2_UNORM_SRGB:
    case DXGI_FORMAT_BC3_TYPELESS:
    case DXGI_FORMAT_BC3_UNORM:
    case DXGI_FORMAT_BC3_UNORM_SRGB:
    case DXGI_FORMAT_BC5_TYPELESS:
    case DXGI_FORMAT_BC5_UNORM:
    case DXGI_FORMAT_BC5_SNORM:
    case DXGI_FORMAT_BC6H_TYPELESS:
    case DXGI_FORMAT_BC6H_UF16:
    case DXGI_FORMAT_BC6H_SF16:
    case DXGI_FORMAT_BC7_TYPELESS:
    case DXGI_FORMAT_BC7_UNORM:
    case DXGI_FORMAT_BC7_UNORM_SRGB:
    case DXGI_FORMAT_AI44:
    case DXGI_FORMAT_IA44:
    case DXGI_FORMAT_P8:
        return 8;

    case DXGI_FORMAT_R1_UNORM:
        return 1;

    case DXGI_FORMAT_BC1_TYPELESS:
    case DXGI_FORMAT_BC1_UNORM:
    case DXGI_FORMAT_BC1_UNORM_SRGB:
    case DXGI_FORMAT_BC4_TYPELESS:
    case DXGI_FORMAT_BC4_UNORM:
    case DXGI_FORMAT_BC4_SNORM:
        return 4;

    default:
        return 0;
    }
}

static void decode_dx5_alpha_block(InputMemoryStream& stream, DDSLoadingContext& context, u64 bitmap_x, u64 bitmap_y)
{
    LittleEndian<u8> color0 {}, color1 {};
    LittleEndian<u8> code0 {}, code1 {}, code2 {}, code3 {}, code4 {}, code5 {};

    stream >> color0;
    stream >> color1;
    stream >> code0;
    stream >> code1;
    stream >> code2;
    stream >> code3;
    stream >> code4;
    stream >> code5;

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

    for (size_t y = 0; y < 4; y++) {
        for (size_t x = 0; x < 4; x++) {
            u8 index = 3 * (4 * y + x);
            u8 bit_location = floor(index / 8.0);
            u8 adjusted_index = index - (bit_location * 8);

            u8 code = (codes[bit_location] >> adjusted_index) & 7;
            u8 alpha = color[code];

            Color color = Color(0, 0, 0, alpha);
            context.bitmap->set_pixel(bitmap_x + x, bitmap_y + y, color);
        }
    }
}

static void decode_dx3_alpha_block(InputMemoryStream& stream, DDSLoadingContext& context, u64 bitmap_x, u64 bitmap_y)
{
    LittleEndian<u8> a0 {}, a1 {}, a2 {}, a3 {}, a4 {}, a5 {}, a6 {}, a7 {};

    stream >> a0;
    stream >> a1;
    stream >> a2;
    stream >> a3;
    stream >> a4;
    stream >> a5;
    stream >> a6;
    stream >> a7;

    u64 alpha_0 = a0 + 256u * (a1 + 256u * (a2 + 256u * (a3 + 256u)));
    u64 alpha_1 = a4 + 256u * (a5 + 256u * (a6 + 256u * a7));

    for (size_t y = 0; y < 4; y++) {
        for (size_t x = 0; x < 4; x++) {
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

static void decode_color_block(InputMemoryStream& stream, DDSLoadingContext& context, bool dxt1, u64 bitmap_x, u64 bitmap_y)
{
    LittleEndian<u8> c0_low {}, c0_high {}, c1_low {}, c1_high {};
    LittleEndian<u8> codes_0 {}, codes_1 {}, codes_2 {}, codes_3 {};

    stream >> c0_low;
    stream >> c0_high;
    stream >> c1_low;
    stream >> c1_high;
    stream >> codes_0;
    stream >> codes_1;
    stream >> codes_2;
    stream >> codes_3;

    u64 code = codes_0 + 256 * (codes_1 + 256 * (codes_2 + 256 * codes_3));
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
    for (size_t y = 0; y < 4; y++) {
        for (size_t x = 0; x < 4; x++) {
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
}

static void decode_dxt(InputMemoryStream& stream, DDSLoadingContext& context, DXGIFormat format, u64 width, u64 y)
{
    if (format == DXGI_FORMAT_BC1_UNORM) {
        for (size_t x = 0; x < width; x += 4) {
            decode_color_block(stream, context, true, x, y);
        }
    }

    if (format == DXGI_FORMAT_BC2_UNORM) {
        for (size_t x = 0; x < width; x += 4) {
            decode_dx3_alpha_block(stream, context, x, y);
            decode_color_block(stream, context, false, x, y);
        }
    }

    if (format == DXGI_FORMAT_BC3_UNORM) {
        for (size_t x = 0; x < width; x += 4) {
            decode_dx5_alpha_block(stream, context, x, y);
            decode_color_block(stream, context, false, x, y);
        }
    }
}
static void decode_bitmap(InputMemoryStream& stream, DDSLoadingContext& context, DXGIFormat format, u64 width, u64 height)
{
    Vector<u32> dxt_formats = { DXGI_FORMAT_BC1_UNORM, DXGI_FORMAT_BC2_UNORM, DXGI_FORMAT_BC3_UNORM };
    if (dxt_formats.contains_slow(format)) {
        for (u64 y = 0; y < height; y += 4) {
            decode_dxt(stream, context, format, width, y);
        }
    }

    // FIXME: Support more encodings (ATI, YUV, RAW, etc...).
}

static size_t get_minimum_bytes_for_mipmap(DXGIFormat format, u64 width, u64 height)
{
    u64 row_bytes {};
    u64 row_count {};

    if (is_block_compressed(format)) {
        u64 width_in_blocks {};
        u64 height_in_blocks {};

        if (width > 0) {
            width_in_blocks = max(static_cast<u64>(1), (width + 3u) / 4u);
        }

        if (height > 0) {
            height_in_blocks = max(static_cast<u64>(1), (height + 3u) / 4u);
        }

        row_bytes = width_in_blocks * block_size(format);
        row_count = height_in_blocks;
        return row_bytes * row_count;
    } else if (is_packed(format)) {
        row_bytes = ((width + 1u) >> 1) * block_size(format);
        row_count = height;
        return row_bytes * row_count;
    } else if (format == DXGI_FORMAT_NV11) {
        row_bytes = ((width + 3u) >> 2) * 4u;
        row_count = height * 2u;
        return row_bytes * row_count;
    } else if (is_planar(format)) {
        row_bytes = ((width + 1u) >> 1) * block_size(format);
        row_count = height + ((height + 1u) >> 1);
        return (row_bytes * row_count) + (((row_bytes * row_count) + 1) >> 1);
    } else {
        u32 bpp = bits_per_pixel(format);

        row_bytes = (width * bpp + 7u) / 8u;
        row_count = height;
        return row_bytes * row_count;
    }
}

static bool decode_dds(DDSLoadingContext& context)
{
    InputMemoryStream stream({ context.data, context.data_size });

    // All valid DDS files are at least 128 bytes long.
    if (stream.remaining() < 128) {
        dbgln_if(DDS_DEBUG, "File is too short for DDS");
        context.state = DDSLoadingContext::State::Error;
        return false;
    }

    u32 magic;
    stream >> magic;

    if (magic != create_four_cc('D', 'D', 'S', ' ')) {
        dbgln_if(DDS_DEBUG, "Missing magic number");
        context.state = DDSLoadingContext::State::Error;
        return false;
    }

    stream >> context.header.size;
    stream >> context.header.flags;
    stream >> context.header.height;
    stream >> context.header.width;
    stream >> context.header.pitch;
    stream >> context.header.depth;
    stream >> context.header.mip_map_count;
    // The bytes in context.header.reserved are unused, so we just skip over them (11 * 4 bytes).
    stream.discard_or_error(44);
    stream >> context.header.pixel_format.size;
    stream >> context.header.pixel_format.flags;
    stream >> context.header.pixel_format.four_cc;
    stream >> context.header.pixel_format.rgb_bit_count;
    stream >> context.header.pixel_format.r_bit_mask;
    stream >> context.header.pixel_format.g_bit_mask;
    stream >> context.header.pixel_format.b_bit_mask;
    stream >> context.header.pixel_format.a_bit_mask;
    stream >> context.header.caps1;
    stream >> context.header.caps2;
    stream >> context.header.caps3;
    stream >> context.header.caps4;
    stream >> context.header.reserved2;

    if (context.header.size != 124) {
        dbgln_if(DDS_DEBUG, "Header size is malformed");
        context.state = DDSLoadingContext::State::Error;
        return false;
    }
    if (context.header.pixel_format.size != 32) {
        dbgln_if(DDS_DEBUG, "Pixel format size is malformed");
        context.state = DDSLoadingContext::State::Error;
        return false;
    }

    if ((context.header.pixel_format.flags & PixelFormatFlags::DDPF_FOURCC) == PixelFormatFlags::DDPF_FOURCC) {
        if (context.header.pixel_format.four_cc == create_four_cc('D', 'X', '1', '0')) {
            if (stream.bytes().size() < 148) {
                dbgln_if(DDS_DEBUG, "DX10 header is too short");
                context.state = DDSLoadingContext::State::Error;
                return false;
            }

            u32 format {};
            stream >> format;
            context.header10.format = static_cast<DXGIFormat>(format);
            stream >> context.header10.resource_dimension;
            stream >> context.header10.misc_flag;
            stream >> context.header10.array_size;
            stream >> context.header10.misc_flag2;
        }
    }

    if constexpr (DDS_DEBUG) {
        context.dump_debug();
    }

    DXGIFormat format = get_format(context.header.pixel_format);

    Vector<u32> supported_formats = { DXGI_FORMAT_BC1_UNORM, DXGI_FORMAT_BC2_UNORM, DXGI_FORMAT_BC3_UNORM };
    if (!supported_formats.contains_slow(format)) {
        dbgln_if(DDS_DEBUG, "Format of type {} is not supported at the moment", static_cast<u32>(format));
        context.state = DDSLoadingContext::State::Error;
        return false;
    }

    // We support parsing mipmaps, but we only care about the largest one :^) (At least for now)
    if (size_t mipmap_level = 0; mipmap_level < max(context.header.mip_map_count, 1u)) {
        u64 width = get_width(context.header, mipmap_level);
        u64 height = get_height(context.header, mipmap_level);

        u64 needed_bytes = get_minimum_bytes_for_mipmap(format, width, height);
        dbgln_if(DDS_DEBUG, "There are {} bytes remaining, we need {} for mipmap level {} of the image", stream.remaining(), needed_bytes, mipmap_level);
        VERIFY(stream.remaining() >= needed_bytes);

        context.bitmap = Bitmap::try_create(BitmapFormat::BGRA8888, { width, height }).release_value_but_fixme_should_propagate_errors();

        decode_bitmap(stream, context, format, width, height);
    }

    context.state = DDSLoadingContext::State::BitmapDecoded;

    return true;
}

void DDSLoadingContext::dump_debug()
{
    StringBuilder builder;

    builder.append("\nDDS:\n");
    builder.appendff("\tHeader Size: {}\n", header.size);

    builder.append("\tFlags:");
    if ((header.flags & DDSFlags::DDSD_CAPS) == DDSFlags::DDSD_CAPS)
        builder.append(" DDSD_CAPS");
    if ((header.flags & DDSFlags::DDSD_HEIGHT) == DDSFlags::DDSD_HEIGHT)
        builder.append(" DDSD_HEIGHT");
    if ((header.flags & DDSFlags::DDSD_WIDTH) == DDSFlags::DDSD_WIDTH)
        builder.append(" DDSD_WIDTH");
    if ((header.flags & DDSFlags::DDSD_PITCH) == DDSFlags::DDSD_PITCH)
        builder.append(" DDSD_PITCH");
    if ((header.flags & DDSFlags::DDSD_PIXELFORMAT) == DDSFlags::DDSD_PIXELFORMAT)
        builder.append(" DDSD_PIXELFORMAT");
    if ((header.flags & DDSFlags::DDSD_MIPMAPCOUNT) == DDSFlags::DDSD_MIPMAPCOUNT)
        builder.append(" DDSD_MIPMAPCOUNT");
    if ((header.flags & DDSFlags::DDSD_LINEARSIZE) == DDSFlags::DDSD_LINEARSIZE)
        builder.append(" DDSD_LINEARSIZE");
    if ((header.flags & DDSFlags::DDSD_DEPTH) == DDSFlags::DDSD_DEPTH)
        builder.append(" DDSD_DEPTH");
    builder.append("\n");

    builder.appendff("\tHeight: {}\n", header.height);
    builder.appendff("\tWidth: {}\n", header.width);
    builder.appendff("\tPitch: {}\n", header.pitch);
    builder.appendff("\tDepth: {}\n", header.depth);
    builder.appendff("\tMipmap Count: {}\n", header.mip_map_count);

    builder.append("\tCaps:");
    if ((header.caps1 & Caps1Flags::DDSCAPS_COMPLEX) == Caps1Flags::DDSCAPS_COMPLEX)
        builder.append(" DDSCAPS_COMPLEX");
    if ((header.caps1 & Caps1Flags::DDSCAPS_MIPMAP) == Caps1Flags::DDSCAPS_MIPMAP)
        builder.append(" DDSCAPS_MIPMAP");
    if ((header.caps1 & Caps1Flags::DDSCAPS_TEXTURE) == Caps1Flags::DDSCAPS_TEXTURE)
        builder.append(" DDSCAPS_TEXTURE");
    builder.append("\n");

    builder.append("\tCaps2:");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP) == Caps2Flags::DDSCAPS2_CUBEMAP)
        builder.append(" DDSCAPS2_CUBEMAP");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEX) == Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEX)
        builder.append(" DDSCAPS2_CUBEMAP_POSITIVEX");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEX) == Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEX)
        builder.append(" DDSCAPS2_CUBEMAP_NEGATIVEX");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEY) == Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEY)
        builder.append(" DDSCAPS2_CUBEMAP_POSITIVEY");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEY) == Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEY)
        builder.append(" DDSCAPS2_CUBEMAP_NEGATIVEY");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEZ) == Caps2Flags::DDSCAPS2_CUBEMAP_POSITIVEZ)
        builder.append(" DDSCAPS2_CUBEMAP_POSITIVEZ");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEZ) == Caps2Flags::DDSCAPS2_CUBEMAP_NEGATIVEZ)
        builder.append(" DDSCAPS2_CUBEMAP_NEGATIVEZ");
    if ((header.caps2 & Caps2Flags::DDSCAPS2_VOLUME) == Caps2Flags::DDSCAPS2_VOLUME)
        builder.append(" DDSCAPS2_VOLUME");
    builder.append("\n");

    builder.append("Pixel Format:\n");
    builder.appendff("\tStruct Size: {}\n", header.pixel_format.size);

    builder.append("\tFlags:");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_ALPHAPIXELS) == PixelFormatFlags::DDPF_ALPHAPIXELS)
        builder.append(" DDPF_ALPHAPIXELS");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_ALPHA) == PixelFormatFlags::DDPF_ALPHA)
        builder.append(" DDPF_ALPHA");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_FOURCC) == PixelFormatFlags::DDPF_FOURCC)
        builder.append(" DDPF_FOURCC");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_PALETTEINDEXED8) == PixelFormatFlags::DDPF_PALETTEINDEXED8)
        builder.append(" DDPF_PALETTEINDEXED8");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_RGB) == PixelFormatFlags::DDPF_RGB)
        builder.append(" DDPF_RGB");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_YUV) == PixelFormatFlags::DDPF_YUV)
        builder.append(" DDPF_YUV");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_LUMINANCE) == PixelFormatFlags::DDPF_LUMINANCE)
        builder.append(" DDPF_LUMINANCE");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_BUMPDUDV) == PixelFormatFlags::DDPF_BUMPDUDV)
        builder.append(" DDPF_BUMPDUDV");
    if ((header.pixel_format.flags & PixelFormatFlags::DDPF_NORMAL) == PixelFormatFlags::DDPF_NORMAL)
        builder.append(" DDPF_NORMAL");
    builder.append("\n");

    builder.append("\tFour CC: ");
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 0)) & 0xFF);
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 1)) & 0xFF);
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 2)) & 0xFF);
    builder.appendff("{:c}", (header.pixel_format.four_cc >> (8 * 3)) & 0xFF);
    builder.append("\n");
    builder.appendff("\tRGB Bit Count: {}\n", header.pixel_format.rgb_bit_count);
    builder.appendff("\tR Bit Mask: {}\n", header.pixel_format.r_bit_mask);
    builder.appendff("\tG Bit Mask: {}\n", header.pixel_format.g_bit_mask);
    builder.appendff("\tB Bit Mask: {}\n", header.pixel_format.b_bit_mask);
    builder.appendff("\tA Bit Mask: {}\n", header.pixel_format.a_bit_mask);

    builder.append("DDS10:\n");
    builder.appendff("\tFormat: {}\n", static_cast<u32>(header10.format));

    builder.append("\tResource Dimension:");
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_UNKNOWN) == ResourceDimensions::DDS_DIMENSION_UNKNOWN)
        builder.append(" DDS_DIMENSION_UNKNOWN");
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_BUFFER) == ResourceDimensions::DDS_DIMENSION_BUFFER)
        builder.append(" DDS_DIMENSION_BUFFER");
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_TEXTURE1D) == ResourceDimensions::DDS_DIMENSION_TEXTURE1D)
        builder.append(" DDS_DIMENSION_TEXTURE1D");
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_TEXTURE2D) == ResourceDimensions::DDS_DIMENSION_TEXTURE2D)
        builder.append(" DDS_DIMENSION_TEXTURE2D");
    if ((header10.resource_dimension & ResourceDimensions::DDS_DIMENSION_TEXTURE3D) == ResourceDimensions::DDS_DIMENSION_TEXTURE3D)
        builder.append(" DDS_DIMENSION_TEXTURE3D");
    builder.append("\n");

    builder.appendff("\tArray Size: {}\n", header10.array_size);

    builder.append("\tMisc Flags:");
    if ((header10.misc_flag & MiscFlags::DDS_RESOURCE_MISC_TEXTURECUBE) == MiscFlags::DDS_RESOURCE_MISC_TEXTURECUBE)
        builder.append(" DDS_RESOURCE_MISC_TEXTURECUBE");
    builder.append("\n");

    builder.append("\tMisc Flags 2:");
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_UNKNOWN) == Misc2Flags::DDS_ALPHA_MODE_UNKNOWN)
        builder.append(" DDS_ALPHA_MODE_UNKNOWN");
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_STRAIGHT) == Misc2Flags::DDS_ALPHA_MODE_STRAIGHT)
        builder.append(" DDS_ALPHA_MODE_STRAIGHT");
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_PREMULTIPLIED) == Misc2Flags::DDS_ALPHA_MODE_PREMULTIPLIED)
        builder.append(" DDS_ALPHA_MODE_PREMULTIPLIED");
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_OPAQUE) == Misc2Flags::DDS_ALPHA_MODE_OPAQUE)
        builder.append(" DDS_ALPHA_MODE_OPAQUE");
    if ((header10.misc_flag2 & Misc2Flags::DDS_ALPHA_MODE_CUSTOM) == Misc2Flags::DDS_ALPHA_MODE_CUSTOM)
        builder.append(" DDS_ALPHA_MODE_CUSTOM");
    builder.append("\n");

    dbgln("{}", builder.to_string());
}

DDSImageDecoderPlugin::DDSImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<DDSLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

DDSImageDecoderPlugin::~DDSImageDecoderPlugin()
{
}

IntSize DDSImageDecoderPlugin::size()
{
    if (m_context->state == DDSLoadingContext::State::Error)
        return {};

    if (m_context->state == DDSLoadingContext::State::BitmapDecoded)
        return { m_context->header.width, m_context->header.height };

    return {};
}

void DDSImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool DDSImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;
    return m_context->bitmap->set_nonvolatile(was_purged);
}

bool DDSImageDecoderPlugin::sniff()
{
    // The header is always at least 128 bytes, so if the file is smaller, it can't be a DDS.
    return m_context->data_size > 128
        && m_context->data[0] == 0x44
        && m_context->data[1] == 0x44
        && m_context->data[2] == 0x53
        && m_context->data[3] == 0x20;
}

bool DDSImageDecoderPlugin::is_animated()
{
    return false;
}

size_t DDSImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t DDSImageDecoderPlugin::frame_count()
{
    return 1;
}

ErrorOr<ImageFrameDescriptor> DDSImageDecoderPlugin::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("DDSImageDecoderPlugin: Invalid frame index"sv);

    if (m_context->state == DDSLoadingContext::State::Error)
        return Error::from_string_literal("DDSImageDecoderPlugin: Decoding failed"sv);

    if (m_context->state < DDSLoadingContext::State::BitmapDecoded) {
        bool success = decode_dds(*m_context);
        if (!success)
            return Error::from_string_literal("DDSImageDecoderPlugin: Decoding failed"sv);
    }

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
