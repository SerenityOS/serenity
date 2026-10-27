/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Texture.h"

#include <AK/IntegralMath.h>
#include <LibGL/GL/gl.h>
#include <LibGL/Tex/Sampler2D.h>
#include <LibGPU/ImageDataLayout.h>

namespace GL {

class Texture2D final : public Texture {
public:
    virtual bool is_texture_2d() const override { return true; }

    void download_texture_data(GLuint lod, GPU::ImageDataLayout output_layout, GLvoid* pixels);
    void upload_texture_data(GLuint lod, GLenum internal_format, GPU::ImageDataLayout input_layout, GLvoid const* pixels);
    void replace_sub_texture_data(GLuint lod, GPU::ImageDataLayout input_layout, Vector3<i32> const& output_offset, GLvoid const* pixels);

    void set_generate_mipmaps(bool generate_mipmaps);
    GLenum internal_format() const { return m_internal_format; }
    Sampler2D const& sampler() const { return m_sampler; }
    Sampler2D& sampler() { return m_sampler; }

    // OpenGL specification version 1.5, 3.8.11 Texture State and Proxy State:
    // "Each initial texture array is null (zero width, height, and depth,
    //  zero border width, internal format 1, with the compressed flag
    //  set to FALSE, a zero compressed size, and zero-sized components)."
    // We represent null textures by having a null device_image(), so the
    // {width,height,depth}_at_lod() functions all return 0 when device_image() is null.

    int width_at_lod(unsigned level) const
    {
        if (device_image().is_null())
            return 0;
        return static_cast<int>(device_image()->width_at_level(level));
    }

    int height_at_lod(unsigned level) const
    {
        if (device_image().is_null())
            return 0;
        return static_cast<int>(device_image()->height_at_level(level));
    }

    int depth_at_lod(unsigned level) const
    {
        if (device_image().is_null())
            return 0;
        return static_cast<int>(device_image()->depth_at_level(level));
    }

private:
    bool m_generate_mipmaps { false };
    GLenum m_internal_format;
    Sampler2D m_sampler;
};

}
