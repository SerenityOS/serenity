/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Sampler2D.h"

#include <LibGL/Tex/Texture2D.h>
#include <math.h>

namespace GL {

static constexpr float wrap_repeat(float value)
{
    return value - floorf(value);
}

static constexpr float wrap_clamp_to_edge(float value, int num_texels)
{
    float const clamp_limit = 1.f / (2 * num_texels);
    return clamp(value, clamp_limit, 1.0f - clamp_limit);
}

static constexpr float wrap_mirrored_repeat(float value, int num_texels)
{
    float integer = floorf(value);
    float frac = value - integer;
    bool iseven = fmodf(integer, 2.0f) == 0.0f;
    return wrap_clamp_to_edge(iseven ? frac : 1 - frac, num_texels);
}

static constexpr float wrap(float value, GLint mode, int num_texels)
{
    switch (mode) {
    case GL_REPEAT:
        return wrap_repeat(value);

    // FIXME: These clamp modes actually have slightly different behavior. Currently we use GL_CLAMP_TO_EDGE for all of them.
    case GL_CLAMP:
    case GL_CLAMP_TO_BORDER:
    case GL_CLAMP_TO_EDGE:
        return wrap_clamp_to_edge(value, num_texels);

    case GL_MIRRORED_REPEAT:
        return wrap_mirrored_repeat(value, num_texels);

    default:
        VERIFY_NOT_REACHED();
    }
}

FloatVector4 Sampler2D::sample(FloatVector4 const& uv) const
{
    // FIXME: Calculate the correct mipmap level here, need to receive uv derivatives for that
    unsigned lod = 0;

    MipMap const& mip = m_texture.mipmap(lod);

    if (mip.width() < 1 || mip.height() < 1)
        return { 1, 1, 1, 1 };

    float x = wrap(uv.x(), m_wrap_s_mode, mip.width());
    float y = wrap(uv.y(), m_wrap_t_mode, mip.height());

    x *= mip.width();
    y *= mip.height();

    // Sampling implemented according to https://www.khronos.org/registry/OpenGL/specs/gl/glspec121.pdf Chapter 3.8
    if (m_mag_filter == GL_NEAREST) {
        return mip.texel(static_cast<unsigned>(x), static_cast<unsigned>(y));
    } else if (m_mag_filter == GL_LINEAR) {
        // FIXME: Implement different sampling points for wrap modes other than GL_REPEAT

        x -= 0.5f;
        y -= 0.5f;

        unsigned i0 = static_cast<unsigned>(x) % mip.width();
        unsigned j0 = static_cast<unsigned>(y) % mip.height();

        unsigned i1 = (i0 + 1) % mip.width();
        unsigned j1 = (j0 + 1) % mip.height();

        auto t0 = mip.texel(i0, j0);
        auto t1 = mip.texel(i1, j0);
        auto t2 = mip.texel(i0, j1);
        auto t3 = mip.texel(i1, j1);

        float frac_x = x - floorf(x);
        float frac_y = y - floorf(y);
        float one_minus_frac_x = 1 - frac_x;

        auto h1 = t0 * one_minus_frac_x + t1 * frac_x;
        auto h2 = t2 * one_minus_frac_x + t3 * frac_x;
        return h1 * (1 - frac_y) + h2 * frac_y;
    } else {
        VERIFY_NOT_REACHED();
    }
}

}
