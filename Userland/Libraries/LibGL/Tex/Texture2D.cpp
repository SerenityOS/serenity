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

    mip.pixel_data().clear();
    if (format == GL_RGBA) {
        for (auto i = 0; i < width * height * 4; i += 4) {
            u32 r = pixel_byte_array[i + 0];
            u32 g = pixel_byte_array[i + 1];
            u32 b = pixel_byte_array[i + 2];
            u32 a = pixel_byte_array[i + 3];

            u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
            mip.pixel_data().append(pixel);
        }
    } else if (format == GL_BGRA) {
        for (auto i = 0; i < width * height * 4; i += 4) {
            u32 b = pixel_byte_array[i + 0];
            u32 g = pixel_byte_array[i + 1];
            u32 r = pixel_byte_array[i + 2];
            u32 a = pixel_byte_array[i + 3];

            u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
            mip.pixel_data().append(pixel);
        }
    } else if (format == GL_BGR) {
        for (auto i = 0; i < width * height * 3; i += 3) {
            u32 b = pixel_byte_array[i + 0];
            u32 g = pixel_byte_array[i + 1];
            u32 r = pixel_byte_array[i + 2];
            u32 a = 255;

            u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
            mip.pixel_data().append(pixel);
        }
    } else if (format == GL_RGB) {
        for (auto i = 0; i < width * height * 3; i += 3) {
            u32 r = pixel_byte_array[i + 0];
            u32 g = pixel_byte_array[i + 1];
            u32 b = pixel_byte_array[i + 2];
            u32 a = 255;

            u32 pixel = ((a << 24) | (r << 16) | (g << 8) | b);
            mip.pixel_data().append(pixel);
        }
    } else {
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
