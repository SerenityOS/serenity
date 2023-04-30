/*
 * Copyright (c) 2022-2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/Image.h>

namespace GL {

ErrorOr<GPU::PixelType> get_validated_pixel_type(GLenum target, GLenum internal_format, GLenum format, GLenum type)
{
    // We accept GL_NONE as target for non-texture related calls (such as `glDrawPixels`)
    if (target != GL_NONE
        && target != GL_TEXTURE_1D
        && target != GL_TEXTURE_2D
        && target != GL_TEXTURE_3D
        && target != GL_TEXTURE_1D_ARRAY
        && target != GL_TEXTURE_2D_ARRAY
        && target != GL_TEXTURE_CUBE_MAP
        && target != GL_PROXY_TEXTURE_1D
        && target != GL_PROXY_TEXTURE_2D
        && target != GL_PROXY_TEXTURE_3D)
        return Error::from_errno(GL_INVALID_ENUM);

    // Internal format can be a number between 1 and 4. Symbolic formats were only added with EXT_texture, promoted to core in OpenGL 1.1
    if (internal_format == 1)
        internal_format = GL_ALPHA;
    else if (internal_format == 2)
        internal_format = GL_LUMINANCE_ALPHA;
    else if (internal_format == 3)
        internal_format = GL_RGB;
    else if (internal_format == 4)
        internal_format = GL_RGBA;

    if (internal_format != GL_NONE
        && internal_format != GL_ALPHA
        && internal_format != GL_ALPHA4
        && internal_format != GL_ALPHA8
        && internal_format != GL_ALPHA12
        && internal_format != GL_ALPHA16
        && internal_format != GL_COMPRESSED_ALPHA
        && internal_format != GL_COMPRESSED_LUMINANCE
        && internal_format != GL_COMPRESSED_LUMINANCE_ALPHA
        && internal_format != GL_COMPRESSED_INTENSITY
        && internal_format != GL_COMPRESSED_RGB
        && internal_format != GL_COMPRESSED_RGBA
        && internal_format != GL_DEPTH_COMPONENT
        && internal_format != GL_DEPTH_COMPONENT16
        && internal_format != GL_DEPTH_COMPONENT24
        && internal_format != GL_DEPTH_COMPONENT32
        && internal_format != GL_DEPTH_STENCIL
        && internal_format != GL_LUMINANCE
        && internal_format != GL_LUMINANCE4
        && internal_format != GL_LUMINANCE8
        && internal_format != GL_LUMINANCE12
        && internal_format != GL_LUMINANCE16
        && internal_format != GL_LUMINANCE_ALPHA
        && internal_format != GL_LUMINANCE4_ALPHA4
        && internal_format != GL_LUMINANCE6_ALPHA2
        && internal_format != GL_LUMINANCE8_ALPHA8
        && internal_format != GL_LUMINANCE12_ALPHA4
        && internal_format != GL_LUMINANCE12_ALPHA12
        && internal_format != GL_LUMINANCE16_ALPHA16
        && internal_format != GL_INTENSITY
        && internal_format != GL_INTENSITY4
        && internal_format != GL_INTENSITY8
        && internal_format != GL_INTENSITY12
        && internal_format != GL_INTENSITY16
        && internal_format != GL_R3_G3_B2
        && internal_format != GL_RED
        && internal_format != GL_RG
        && internal_format != GL_RGB
        && internal_format != GL_RGB4
        && internal_format != GL_RGB5
        && internal_format != GL_RGB8
        && internal_format != GL_RGB10
        && internal_format != GL_RGB12
        && internal_format != GL_RGB16
        && internal_format != GL_RGBA
        && internal_format != GL_RGBA2
        && internal_format != GL_RGBA4
        && internal_format != GL_RGB5_A1
        && internal_format != GL_RGBA8
        && internal_format != GL_RGB10_A2
        && internal_format != GL_RGBA12
        && internal_format != GL_RGBA16
        && internal_format != GL_SLUMINANCE
        && internal_format != GL_SLUMINANCE8
        && internal_format != GL_SLUMINANCE_ALPHA
        && internal_format != GL_SLUMINANCE8_ALPHA8
        && internal_format != GL_SRGB
        && internal_format != GL_SRGB8
        && internal_format != GL_SRGB_ALPHA
        && internal_format != GL_SRGB8_ALPHA8)
        return Error::from_errno(GL_INVALID_ENUM);

    if (format != GL_NONE
        && (format < GL_COLOR_INDEX || format > GL_LUMINANCE_ALPHA)
        && format != GL_BGR
        && format != GL_BGRA)
        return Error::from_errno(GL_INVALID_ENUM);

    if (type != GL_NONE
        && type != GL_BITMAP
        && (type < GL_BYTE || type > GL_FLOAT)
        && type != GL_HALF_FLOAT
        && (type < GL_UNSIGNED_BYTE_3_3_2 || type > GL_UNSIGNED_INT_10_10_10_2)
        && (type < GL_UNSIGNED_BYTE_2_3_3_REV || type > GL_UNSIGNED_INT_2_10_10_10_REV))
        return Error::from_errno(GL_INVALID_ENUM);

    if (type == GL_BITMAP && format != GL_COLOR_INDEX && format != GL_STENCIL_INDEX)
        return Error::from_errno(GL_INVALID_ENUM);

    if (format != GL_RGB && (type == GL_UNSIGNED_BYTE_3_3_2 || type == GL_UNSIGNED_BYTE_2_3_3_REV || type == GL_UNSIGNED_SHORT_5_6_5 || type == GL_UNSIGNED_SHORT_5_6_5_REV))
        return Error::from_errno(GL_INVALID_OPERATION);

    if ((type == GL_UNSIGNED_SHORT_4_4_4_4
            || type == GL_UNSIGNED_SHORT_4_4_4_4_REV
            || type == GL_UNSIGNED_SHORT_5_5_5_1
            || type == GL_UNSIGNED_SHORT_1_5_5_5_REV
            || type == GL_UNSIGNED_INT_8_8_8_8
            || type == GL_UNSIGNED_INT_8_8_8_8_REV
            || type == GL_UNSIGNED_INT_10_10_10_2
            || type == GL_UNSIGNED_INT_2_10_10_10_REV)
        && format != GL_RGBA
        && format != GL_BGRA)
        return Error::from_errno(GL_INVALID_OPERATION);

    if (internal_format != GL_NONE) {
        auto const internal_format_is_depth = internal_format == GL_DEPTH_COMPONENT
            || internal_format == GL_DEPTH_COMPONENT16
            || internal_format == GL_DEPTH_COMPONENT24
            || internal_format == GL_DEPTH_COMPONENT32;

        if ((target != GL_TEXTURE_2D && target != GL_PROXY_TEXTURE_2D && internal_format_is_depth)
            || (format == GL_DEPTH_COMPONENT && !internal_format_is_depth)
            || (format != GL_DEPTH_COMPONENT && internal_format_is_depth))
            return Error::from_errno(GL_INVALID_OPERATION);
    }

    return get_format_specification(format, type);
}

GPU::PixelType get_format_specification(GLenum format, GLenum type)
{
    auto get_format = [](GLenum format) -> GPU::PixelFormat {
        switch (format) {
        case GL_ALPHA:
            return GPU::PixelFormat::Alpha;
        case GL_BGR:
            return GPU::PixelFormat::BGR;
        case GL_BGRA:
            return GPU::PixelFormat::BGRA;
        case GL_BLUE:
            return GPU::PixelFormat::Blue;
        case GL_COLOR_INDEX:
            return GPU::PixelFormat::ColorIndex;
        case GL_DEPTH_COMPONENT:
            return GPU::PixelFormat::DepthComponent;
        case GL_GREEN:
            return GPU::PixelFormat::Green;
        case GL_LUMINANCE:
            return GPU::PixelFormat::Luminance;
        case GL_LUMINANCE_ALPHA:
            return GPU::PixelFormat::LuminanceAlpha;
        case GL_RED:
            return GPU::PixelFormat::Red;
        case GL_RGB:
            return GPU::PixelFormat::RGB;
        case GL_RGBA:
            return GPU::PixelFormat::RGBA;
        case GL_STENCIL_INDEX:
            return GPU::PixelFormat::StencilIndex;
        }
        VERIFY_NOT_REACHED();
    };
    auto pixel_format = get_format(format);

    switch (type) {
    case GL_BITMAP:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::Bitmap, GPU::ComponentsOrder::Normal };
    case GL_BYTE:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::Byte, GPU::ComponentsOrder::Normal };
    case GL_FLOAT:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::Float, GPU::ComponentsOrder::Normal };
    case GL_HALF_FLOAT:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::HalfFloat, GPU::ComponentsOrder::Normal };
    case GL_INT:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::Int, GPU::ComponentsOrder::Normal };
    case GL_SHORT:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::Short, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_BYTE:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::UnsignedByte, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_BYTE_2_3_3_REV:
        return { pixel_format, GPU::PixelComponentBits::B2_3_3, GPU::PixelDataType::UnsignedByte, GPU::ComponentsOrder::Reversed };
    case GL_UNSIGNED_BYTE_3_3_2:
        return { pixel_format, GPU::PixelComponentBits::B3_3_2, GPU::PixelDataType::UnsignedByte, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_INT:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::UnsignedInt, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_INT_2_10_10_10_REV:
        return { pixel_format, GPU::PixelComponentBits::B2_10_10_10, GPU::PixelDataType::UnsignedInt, GPU::ComponentsOrder::Reversed };
    case GL_UNSIGNED_INT_8_8_8_8:
        return { pixel_format, GPU::PixelComponentBits::B8_8_8_8, GPU::PixelDataType::UnsignedInt, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_INT_8_8_8_8_REV:
        return { pixel_format, GPU::PixelComponentBits::B8_8_8_8, GPU::PixelDataType::UnsignedInt, GPU::ComponentsOrder::Reversed };
    case GL_UNSIGNED_INT_10_10_10_2:
        return { pixel_format, GPU::PixelComponentBits::B10_10_10_2, GPU::PixelDataType::UnsignedInt, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_SHORT:
        return { pixel_format, GPU::PixelComponentBits::AllBits, GPU::PixelDataType::UnsignedShort, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_SHORT_1_5_5_5_REV:
        return { pixel_format, GPU::PixelComponentBits::B1_5_5_5, GPU::PixelDataType::UnsignedShort, GPU::ComponentsOrder::Reversed };
    case GL_UNSIGNED_SHORT_4_4_4_4:
        return { pixel_format, GPU::PixelComponentBits::B4_4_4_4, GPU::PixelDataType::UnsignedShort, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_SHORT_4_4_4_4_REV:
        return { pixel_format, GPU::PixelComponentBits::B4_4_4_4, GPU::PixelDataType::UnsignedShort, GPU::ComponentsOrder::Reversed };
    case GL_UNSIGNED_SHORT_5_6_5:
        return { pixel_format, GPU::PixelComponentBits::B5_6_5, GPU::PixelDataType::UnsignedShort, GPU::ComponentsOrder::Normal };
    case GL_UNSIGNED_SHORT_5_6_5_REV:
        return { pixel_format, GPU::PixelComponentBits::B5_6_5, GPU::PixelDataType::UnsignedShort, GPU::ComponentsOrder::Reversed };
    case GL_UNSIGNED_SHORT_5_5_5_1:
        return { pixel_format, GPU::PixelComponentBits::B5_5_5_1, GPU::PixelDataType::UnsignedShort, GPU::ComponentsOrder::Normal };
    }
    VERIFY_NOT_REACHED();
}

GPU::PixelFormat pixel_format_for_internal_format(GLenum internal_format)
{
    // FIXME: add support for all the SRGB formats

    // Numbers 1-4 are supported deprecated values
    switch (internal_format) {
    case 1:
    case GL_ALPHA:
    case GL_ALPHA4:
    case GL_ALPHA8:
    case GL_ALPHA12:
    case GL_ALPHA16:
    case GL_COMPRESSED_ALPHA:
        return GPU::PixelFormat::Alpha;
    case GL_DEPTH_COMPONENT:
    case GL_DEPTH_COMPONENT16:
    case GL_DEPTH_COMPONENT24:
    case GL_DEPTH_COMPONENT32:
        return GPU::PixelFormat::DepthComponent;
    case GL_INTENSITY:
    case GL_INTENSITY4:
    case GL_INTENSITY8:
    case GL_INTENSITY12:
    case GL_INTENSITY16:
    case GL_COMPRESSED_INTENSITY:
        return GPU::PixelFormat::Intensity;
    case GL_LUMINANCE:
    case GL_LUMINANCE4:
    case GL_LUMINANCE8:
    case GL_LUMINANCE12:
    case GL_LUMINANCE16:
    case GL_COMPRESSED_LUMINANCE:
        return GPU::PixelFormat::Luminance;
    case 2:
    case GL_LUMINANCE_ALPHA:
    case GL_LUMINANCE4_ALPHA4:
    case GL_LUMINANCE6_ALPHA2:
    case GL_LUMINANCE8_ALPHA8:
    case GL_LUMINANCE12_ALPHA4:
    case GL_LUMINANCE12_ALPHA12:
    case GL_LUMINANCE16_ALPHA16:
        return GPU::PixelFormat::LuminanceAlpha;
    case 3:
    case GL_RGB:
    case GL_R3_G3_B2:
    case GL_RGB4:
    case GL_RGB5:
    case GL_RGB8:
    case GL_RGB10:
    case GL_RGB12:
    case GL_RGB16:
    case GL_COMPRESSED_RGB:
        return GPU::PixelFormat::RGB;
    case 4:
    case GL_RGBA:
    case GL_RGBA2:
    case GL_RGBA4:
    case GL_RGB5_A1:
    case GL_RGBA8:
    case GL_RGB10_A2:
    case GL_RGBA12:
    case GL_RGBA16:
    case GL_COMPRESSED_RGBA:
        return GPU::PixelFormat::RGBA;
    }

    dbgln("{}({:#x}): unsupported internal format", __FUNCTION__, internal_format);
    VERIFY_NOT_REACHED();
}

}
