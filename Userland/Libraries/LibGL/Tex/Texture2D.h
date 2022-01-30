/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Texture.h"

#include <AK/Array.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibGL/GL/gl.h>
#include <LibGL/Tex/MipMap.h>
#include <LibGL/Tex/Sampler2D.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector4.h>

namespace GL {

class Texture2D final : public Texture {
public:
    // FIXME: These shouldn't really belong here, they're context specific.
    static constexpr u16 MAX_TEXTURE_SIZE = 2048;
    static constexpr u8 LOG2_MAX_TEXTURE_SIZE = 11;

public:
    virtual bool is_texture_2d() const override { return true; }

    void upload_texture_data(GLuint lod, GLint internal_format, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid const* pixels, GLsizei pixels_per_row, u8 byte_alignment);
    void replace_sub_texture_data(GLuint lod, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid const* pixels, GLsizei pixels_per_row, u8 byte_alignment);

    MipMap const& mipmap(unsigned lod) const
    {
        if (lod >= m_mipmaps.size())
            return m_mipmaps.back();

        return m_mipmaps.at(lod);
    }

    GLenum internal_format() const { return m_internal_format; }
    Sampler2D const& sampler() const { return m_sampler; }
    Sampler2D& sampler() { return m_sampler; }

    int width_at_lod(unsigned level) const { return (level >= m_mipmaps.size()) ? 0 : m_mipmaps.at(level).width(); }
    int height_at_lod(unsigned level) const { return (level >= m_mipmaps.size()) ? 0 : m_mipmaps.at(level).height(); }

private:
    // FIXME: Mipmaps are currently unused, but we have the plumbing for it at least
    Array<MipMap, LOG2_MAX_TEXTURE_SIZE> m_mipmaps;
    GLenum m_internal_format;
    Sampler2D m_sampler;
};

}
