/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibGL/GL/gl.h>
#include <LibGL/Tex/Texture2D.h>
#include <string.h>

namespace GL {

void Texture2D::upload_texture_data(GLenum, GLint lod, GLint internal_format, GLsizei width, GLsizei height, GLint, GLenum format, GLenum, const GLvoid* pixels)
{
    // NOTE: Some target, format, and internal formats are currently unsupported.
    // Considering we control this library, and `gl.h` itself, we don't need to add any
    // checks here to see if we support them; the program will simply fail to compile..

    // Somebody passed us in nullptr...
    // Apparently this allocates memory on the GPU (according to Khronos docs..)?
    if (pixels == nullptr) {
        dbgln("LibGL: pixels == nullptr when uploading texture data.");
        VERIFY_NOT_REACHED();
    }

    m_internal_format = internal_format;

    // Get reference to the mip
    auto& mip = m_mipmaps[lod];
    const u8* pixel_byte_array = reinterpret_cast<const u8*>(pixels);

    // Copy pixel data to storage

    // Pixels are already 32-bits wide
    if (format == GL_RGBA || format == GL_BGRA) {
        mip.pixel_data().resize(width * height * sizeof(u32));
        memcpy(mip.pixel_data().data(), pixels, width * height * sizeof(u32));
    } else {
        mip.pixel_data().resize(width * height * 3);
        // Copy RGB or BGR pixel data
        for (auto i = 0; i < width * height * 3; i += 3) {
            u32 b0 = pixel_byte_array[i];     // B or R
            u32 b1 = pixel_byte_array[i + 1]; // G
            u32 b2 = pixel_byte_array[i + 2]; // R or B

            u32 pixel = ((0xffu << 24) | (b0 << 16) | (b1 << 8) | b2);
            mip.pixel_data().append(pixel);
        }
    }

    // Now we need to swizzle the texture data from `format` to `internal_format`
    switch (format) {
    case GL_BGR: {
        if (internal_format == GL_RGB) {
            swizzle(mip.pixel_data(), [](u32 pixel) -> u32 {
                u8 r = pixel & 0xff;
                u8 g = (pixel >> 8) & 0xff;
                u8 b = (pixel >> 16) & 0xff;

                return (0xff << 24) | (r << 16) | (g << 8) | b;
            });
        } else if (internal_format == GL_RGBA) {
            swizzle(mip.pixel_data(), [](u32 pixel) -> u32 {
                u8 r = pixel & 0xff;
                u8 g = (pixel >> 8) & 0xff;
                u8 b = (pixel >> 16) & 0xff;

                return (r << 24) | (g << 16) | (b << 8) | 0xff;
            });
        }
    } break;
    case GL_BGRA: {
        if (internal_format == GL_RGB) {
            swizzle(mip.pixel_data(), [](u32 pixel) -> u32 {
                u8 r = (pixel >> 8) & 0xff;
                u8 g = (pixel >> 16) & 0xff;
                u8 b = (pixel >> 24) & 0xff;

                return (0xff << 24) | (r << 16) | (g << 8) | b;
            });
        } else if (internal_format == GL_RGBA) {
            swizzle(mip.pixel_data(), [](u32 pixel) -> u32 {
                u8 a = pixel & 0xff;
                u8 r = (pixel >> 8) & 0xff;
                u8 g = (pixel >> 16) & 0xff;
                u8 b = (pixel >> 24) & 0xff;

                return (r << 24) | (g << 16) | (b << 8) | a;
            });
        }
    } break;
    case GL_RGB: {
        if (internal_format == GL_RGBA) {
            swizzle(mip.pixel_data(), [](u32 pixel) -> u32 {
                u8 r = pixel & 0xff;
                u8 g = (pixel >> 8) & 0xff;
                u8 b = (pixel >> 16) & 0xff;

                return (r << 24) | (g << 16) | (b << 8) | 0xff;
            });
        }
    } break;
    case GL_RGBA:
        break;
    default:
        // Let's crash for now so we can implement format by format
        VERIFY_NOT_REACHED();
    }

    mip.set_width(width);
    mip.set_height(height);
}

MipMap const& Texture2D::mipmap(unsigned lod) const
{
    if (lod >= m_mipmaps.size())
        return m_mipmaps.at(m_mipmaps.size() - 1);

    return m_mipmaps.at(lod);
}

}
