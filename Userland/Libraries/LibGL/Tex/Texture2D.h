/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Texture.h"

#include <AK/Array.h>
#include <AK/IntegralMath.h>
#include <LibGL/GL/gl.h>
#include <LibGL/Tex/MipMap.h>
#include <LibGL/Tex/Sampler2D.h>
#include <LibGPU/ImageDataLayout.h>

namespace GL {

class Texture2D final : public Texture {
public:
    // FIXME: These shouldn't really belong here, they're context specific.
    static constexpr u16 MAX_TEXTURE_SIZE = 2048;
    static constexpr u8 LOG2_MAX_TEXTURE_SIZE = AK::log2(MAX_TEXTURE_SIZE);

    virtual bool is_texture_2d() const override { return true; }

    void download_texture_data(GLuint lod, GPU::ImageDataLayout output_layout, GLvoid* pixels);
    void upload_texture_data(GLuint lod, GLenum internal_format, GPU::ImageDataLayout input_layout, GLvoid const* pixels);
    void replace_sub_texture_data(GLuint lod, GPU::ImageDataLayout input_layout, Vector3<i32> const& output_offset, GLvoid const* pixels);

    MipMap const& mipmap(unsigned lod) const
    {
        if (lod >= m_mipmaps.size())
            return m_mipmaps.last();

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
