/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/Tex/Texture2D.h>

namespace GL {

void Texture2D::upload_texture_data(GLuint lod, GLint internal_format, GLsizei width, GLsizei height, GLint, GLenum format, GLenum type, const GLvoid* pixels, GLsizei pixels_per_row, u8 byte_alignment)
{
    // NOTE: Some target, format, and internal formats are currently unsupported.
    // Considering we control this library, and `gl.h` itself, we don't need to add any
    // checks here to see if we support them; the program will simply fail to compile..

    auto& mip = m_mipmaps[lod];
    mip.set_width(width);
    mip.set_height(height);
    mip.pixel_data().resize(width * height);

    // No pixel data was supplied leave the texture memory uninitialized.
    if (pixels == nullptr)
        return;

    m_internal_format = internal_format;

    replace_sub_texture_data(lod, 0, 0, width, height, format, type, pixels, pixels_per_row, byte_alignment);
}

void Texture2D::replace_sub_texture_data(GLuint lod, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* pixels, GLsizei pixels_per_row, u8 byte_alignment)
{
    auto& mip = m_mipmaps[lod];

    // FIXME: We currently only support GL_UNSIGNED_BYTE and GL_UNSIGNED_SHORT_5_6_5 pixel data
    VERIFY(type == GL_UNSIGNED_BYTE || type == GL_UNSIGNED_SHORT_5_6_5);
    VERIFY(xoffset >= 0 && yoffset >= 0 && xoffset + width <= mip.width() && yoffset + height <= mip.height());
    VERIFY(pixels_per_row == 0 || pixels_per_row >= xoffset + width);

    u8 pixel_size_bytes;
    switch (type) {
    case GL_UNSIGNED_BYTE:
        pixel_size_bytes = (format == GL_RGBA || format == GL_BGRA) ? 4 : 3;
        break;
    case GL_UNSIGNED_SHORT_5_6_5:
        pixel_size_bytes = sizeof(u16);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // Calculate row offset at end to fit alignment
    int const physical_width = pixels_per_row > 0 ? pixels_per_row : width;
    size_t const physical_width_bytes = physical_width * pixel_size_bytes;
    size_t const row_remainder_bytes = (physical_width - width) * pixel_size_bytes
        + (byte_alignment - physical_width_bytes % byte_alignment) % byte_alignment;

    u8 const* pixel_byte_array = reinterpret_cast<u8 const*>(pixels);

    auto get_next_pixel = [type, format](u8 const** pixels) -> u32 {
        // Split bytes up into RGBA components
        u8 c1, c2, c3, c4;
        switch (type) {
        case GL_UNSIGNED_BYTE:
            c1 = *((*pixels)++);
            c2 = *((*pixels)++);
            c3 = *((*pixels)++);
            if (format == GL_RGBA || format == GL_BGRA) {
                c4 = *((*pixels)++);
            } else {
                c4 = 255;
            }
            break;
        case GL_UNSIGNED_SHORT_5_6_5: {
            u16 const s = *reinterpret_cast<u16 const*>((*pixels) += 2);
            c1 = (s & 0xf800) >> 8;
            c2 = (s & 0x7e0) >> 3;
            c3 = (s & 0x1f) << 3;
            c4 = 255;
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }

        // Reorder components into BGRA pixel
        switch (format) {
        case GL_BGR:
        case GL_BGRA:
            return ((c4 << 24) | (c3 << 16) | (c2 << 8) | c1);
        case GL_RGB:
        case GL_RGBA:
            return ((c4 << 24) | (c1 << 16) | (c2 << 8) | c3);
        default:
            VERIFY_NOT_REACHED();
        }
    };

    for (auto y = yoffset; y < yoffset + height; y++) {
        for (auto x = xoffset; x < xoffset + width; x++) {
            u32 pixel = get_next_pixel(&pixel_byte_array);
            mip.pixel_data()[y * mip.width() + x] = pixel;
        }

        pixel_byte_array += row_remainder_bytes;
    }
}

}
