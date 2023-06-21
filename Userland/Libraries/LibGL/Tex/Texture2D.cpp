/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/Tex/Texture2D.h>

namespace GL {

void Texture2D::download_texture_data(GLuint lod, GPU::ImageDataLayout output_layout, GLvoid* pixels)
{
    VERIFY(!device_image().is_null());
    device_image()->read_texels(lod, { 0, 0, 0 }, pixels, output_layout);
}

void Texture2D::upload_texture_data(GLuint lod, GLenum internal_format, GPU::ImageDataLayout input_layout, GLvoid const* pixels)
{
    m_internal_format = internal_format;

    // No pixel data was supplied; leave the texture memory uninitialized.
    if (pixels == nullptr)
        return;

    replace_sub_texture_data(lod, input_layout, { 0, 0, 0 }, pixels);
    if (lod == 0 && m_generate_mipmaps)
        device_image()->regenerate_mipmaps();
}

void Texture2D::replace_sub_texture_data(GLuint lod, GPU::ImageDataLayout input_layout, Vector3<i32> const& output_offset, GLvoid const* pixels)
{
    // FIXME: We currently depend on the first glTexImage2D call to attach an image to mipmap level 0, which initializes the GPU image
    // Ideally we would create separate GPU images for each level and merge them into a final image
    // once used for rendering for the first time.
    VERIFY(!device_image().is_null());

    device_image()->write_texels(lod, output_offset, pixels, input_layout);
    if (lod == 0 && m_generate_mipmaps)
        device_image()->regenerate_mipmaps();
}

void Texture2D::set_generate_mipmaps(bool generate_mipmaps)
{
    if (m_generate_mipmaps == generate_mipmaps)
        return;
    m_generate_mipmaps = generate_mipmaps;
    if (generate_mipmaps && !device_image().is_null())
        device_image()->regenerate_mipmaps();
}

}
