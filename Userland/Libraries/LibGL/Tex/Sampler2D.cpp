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

static constexpr float wrap_clamp(float value)
{
    return clamp(value, 0.0f, 1.0f);
}

FloatVector4 Sampler2D::sample(FloatVector2 const& uv) const
{
    // FIXME: Calculate the correct mipmap level here, need to receive uv derivatives for that
    unsigned lod = 0;

    MipMap const& mip = m_texture.mipmap(lod);

    float x = uv.x();
    float y = uv.y();

    switch (m_wrap_s_mode) {
    case GL_REPEAT:
        x = wrap_repeat(x);
        break;

    // FIXME: These clamp modes actually have slightly different behaviour
    case GL_CLAMP:
    case GL_CLAMP_TO_BORDER:
    case GL_CLAMP_TO_EDGE:
        x = wrap_clamp(x);
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    switch (m_wrap_t_mode) {
    case GL_REPEAT:
        y = wrap_repeat(y);
        break;

    // FIXME: These clamp modes actually have slightly different behaviour
    case GL_CLAMP:
    case GL_CLAMP_TO_BORDER:
    case GL_CLAMP_TO_EDGE:
        y = wrap_clamp(y);
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    x *= mip.width() - 1;
    y *= mip.height() - 1;

    return mip.texel(static_cast<unsigned>(x), static_cast<unsigned>(y));
}

}
