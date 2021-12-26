/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/Tex/Texture2D.h>

namespace GL {

void Texture2D::upload_texture_data(GLuint lod, GLint internal_format, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* pixels, GLsizei pixels_per_row, u8 byte_alignment)
{
    // NOTE: Some target, format, and internal formats are currently unsupported.
    // Considering we control this library, and `gl.h` itself, we don't need to add any
    // checks here to see if we support them; the program will simply fail to compile..

    auto& mip = m_mipmaps[lod];
    mip.set_width(width);
    mip.set_height(height);

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

    // FIXME: We currently depend on the first glTexImage2D call to attach an image to mipmap level 0, which initializes the GPU image
    // Ideally we would create separate GPU images for each level and merge them into a final image
    // once used for rendering for the first time.
    if (device_image().is_null())
        return;

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

    SoftGPU::ImageDataLayout layout;
    layout.column_stride = pixel_size_bytes;
    layout.row_stride = physical_width_bytes + (byte_alignment - physical_width_bytes % byte_alignment) % byte_alignment;
    layout.depth_stride = 0;

    if (type == GL_UNSIGNED_SHORT_5_6_5) {
        layout.format = SoftGPU::ImageFormat::RGB565;
    } else if (type == GL_UNSIGNED_BYTE) {
        if (format == GL_RGB)
            layout.format = SoftGPU::ImageFormat::RGB888;
        else if (format == GL_BGR)
            layout.format = SoftGPU::ImageFormat::BGR888;
        else if (format == GL_RGBA)
            layout.format = SoftGPU::ImageFormat::RGBA8888;
        else if (format == GL_BGRA)
            layout.format = SoftGPU::ImageFormat::BGRA8888;
    }

    Vector3<unsigned> offset {
        static_cast<unsigned>(xoffset),
        static_cast<unsigned>(yoffset),
        0
    };

    Vector3<unsigned> size {
        static_cast<unsigned>(width),
        static_cast<unsigned>(height),
        1
    };

    device_image()->write_texels(0, lod, offset, size, pixels, layout);
}

}
