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

    // FIXME: We currently only support GL_UNSIGNED_BYTE pixel data
    VERIFY(type == GL_UNSIGNED_BYTE);
    VERIFY(lod < m_mipmaps.size());
    VERIFY(xoffset >= 0 && yoffset >= 0 && xoffset + width <= mip.width() && yoffset + height <= mip.height());
    VERIFY(pixels_per_row == 0 || pixels_per_row >= xoffset + width);

    // Calculate row offset at end to fit alignment
    int const physical_width = pixels_per_row > 0 ? pixels_per_row : width;
    u8 const component_size_bytes = sizeof(u8);
    u8 const component_count = (format == GL_RGBA || format == GL_BGRA) ? 4 : 3;
    size_t const physical_width_bytes = physical_width * component_count * component_size_bytes;
    size_t const row_remainder_bytes = (physical_width - width) * component_count * component_size_bytes
        + (byte_alignment - physical_width_bytes % byte_alignment) % byte_alignment;

    u8 const* pixel_byte_array = reinterpret_cast<u8 const*>(pixels);

    if (format == GL_RGBA) {
        for (auto y = yoffset; y < yoffset + height; y++) {
            for (auto x = xoffset; x < xoffset + width; x++) {
                u32 r = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 b = *pixel_byte_array++;
                u32 a = *pixel_byte_array++;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data()[y * mip.width() + x] = pixel;
            }

            pixel_byte_array += row_remainder_bytes;
        }
    } else if (format == GL_BGRA) {
        for (auto y = yoffset; y < yoffset + height; y++) {
            for (auto x = xoffset; x < xoffset + width; x++) {
                u32 b = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 r = *pixel_byte_array++;
                u32 a = *pixel_byte_array++;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data()[y * mip.width() + x] = pixel;
            }

            pixel_byte_array += row_remainder_bytes;
        }
    } else if (format == GL_BGR) {
        for (auto y = yoffset; y < yoffset + height; y++) {
            for (auto x = xoffset; x < xoffset + width; x++) {
                u32 b = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 r = *pixel_byte_array++;
                u32 a = 255;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data()[y * mip.width() + x] = pixel;
            }

            pixel_byte_array += row_remainder_bytes;
        }
    } else if (format == GL_RGB) {
        for (auto y = yoffset; y < yoffset + height; y++) {
            for (auto x = xoffset; x < xoffset + width; x++) {
                u32 r = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 b = *pixel_byte_array++;
                u32 a = 255;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data()[y * mip.width() + x] = pixel;
            }

            pixel_byte_array += row_remainder_bytes;
        }
    } else {
        VERIFY_NOT_REACHED();
    }
}

MipMap const& Texture2D::mipmap(unsigned lod) const
{
    if (lod >= m_mipmaps.size())
        return m_mipmaps.back();

    return m_mipmaps.at(lod);
}

}
