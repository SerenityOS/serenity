/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Texture.h"

#include <AK/Array.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibGL/GL/gl.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector4.h>

namespace GL {

class Texture2D final : public Texture {
public:
    // FIXME: These shouldn't really belong here, they're context specific.
    static constexpr u16 MAX_TEXTURE_SIZE = 2048;
    static constexpr u8 LOG2_MAX_TEXTURE_SIZE = 11;

    class MipMap {
    public:
        MipMap() = default;
        ~MipMap() = default;

        void set_width(GLsizei width) { m_width = width; }
        void set_height(GLsizei height) { m_height = height; }
        GLsizei width() const { return m_width; }
        GLsizei height() const { return m_height; }

        Vector<u32>& pixel_data() { return m_pixel_data; }
        const Vector<u32>& pixel_data() const { return m_pixel_data; }

    private:
        GLsizei m_width;
        GLsizei m_height;
        Vector<u32> m_pixel_data;
    };

    // To quote the Khronos documentation:
    // "You could say that a texture object contains a sampler object, which you access through the texture interface."
    // FIXME: Better name?
    struct TextureSamplerParamaters {
        GLint m_min_filter { GL_NEAREST_MIPMAP_LINEAR };
        GLint m_mag_filter { GL_LINEAR };
        GLint m_wrap_s_mode { GL_REPEAT };
        GLint m_wrap_t_mode { GL_REPEAT };
    };

public:
    Texture2D() = default;
    ~Texture2D() { }

    virtual bool is_texture_2d() const override { return true; }

    void upload_texture_data(GLenum target, GLint lod, GLint internal_format, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels);
    void replace_sub_texture_data(GLint lod, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* data);
    FloatVector4 sample_texel(const FloatVector2& uv) const;

    GLenum internal_format() const { return m_internal_format; }

private:
    template<typename TCallback>
    void swizzle(Vector<u32>& pixels, TCallback&& callback)
    {
        for (auto& pixel : pixels)
            pixel = callback(pixel);
    }

private:
    // FIXME: Mipmaps are currently unused, but we have the plumbing for it at least
    Array<MipMap, LOG2_MAX_TEXTURE_SIZE> m_mipmaps;
    GLenum m_internal_format;
    TextureSamplerParamaters m_sampler_params;
};

}
