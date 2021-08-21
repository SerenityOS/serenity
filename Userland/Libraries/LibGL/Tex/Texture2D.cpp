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

void Texture2D::upload_texture_data(GLenum, GLint lod, GLint internal_format, GLsizei width, GLsizei height, GLint, GLenum format, GLenum, const GLvoid* pixels, size_t pixels_per_row)
{
    // NOTE: Some target, format, and internal formats are currently unsupported.
    // Considering we control this library, and `gl.h` itself, we don't need to add any
    // checks here to see if we support them; the program will simply fail to compile..

    auto& mip = m_mipmaps[lod];
    mip.set_width(width);
    mip.set_height(height);

    // No pixel data was supplied. Just allocate texture memory and leave it uninitialized.
    if (pixels == nullptr) {
        mip.pixel_data().resize(width * height);
        return;
    }

    m_internal_format = internal_format;

    const u8* pixel_byte_array = reinterpret_cast<const u8*>(pixels);

    mip.pixel_data().clear();
    if (format == GL_RGBA) {
        for (auto y = 0; y < height; y++) {
            for (auto x = 0; x < width; x++) {
                u32 r = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 b = *pixel_byte_array++;
                u32 a = *pixel_byte_array++;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data().append(pixel);
            }

            if (pixels_per_row > 0) {
                pixel_byte_array += (pixels_per_row - width) * 4;
            }
        }
    } else if (format == GL_BGRA) {
        for (auto y = 0; y < height; y++) {
            for (auto x = 0; x < width; x++) {
                u32 b = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 r = *pixel_byte_array++;
                u32 a = *pixel_byte_array++;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data().append(pixel);
            }

            if (pixels_per_row > 0) {
                pixel_byte_array += (pixels_per_row - width) * 4;
            }
        }
    } else if (format == GL_BGR) {
        for (auto y = 0; y < height; y++) {
            for (auto x = 0; x < width; x++) {
                u32 b = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 r = *pixel_byte_array++;
                u32 a = 255;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data().append(pixel);
            }

            if (pixels_per_row > 0) {
                pixel_byte_array += (pixels_per_row - width) * 3;
            }
        }
    } else if (format == GL_RGB) {
        for (auto y = 0; y < height; y++) {
            for (auto x = 0; x < width; x++) {
                u32 r = *pixel_byte_array++;
                u32 g = *pixel_byte_array++;
                u32 b = *pixel_byte_array++;
                u32 a = 255;

                u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
                mip.pixel_data().append(pixel);
            }

            if (pixels_per_row > 0) {
                pixel_byte_array += (pixels_per_row - width) * 3;
            }
        }
    } else {
        VERIFY_NOT_REACHED();
    }
}

MipMap const& Texture2D::mipmap(unsigned lod) const
{
    if (lod >= m_mipmaps.size())
        return m_mipmaps.at(m_mipmaps.size() - 1);

    return m_mipmaps.at(lod);
}

}
