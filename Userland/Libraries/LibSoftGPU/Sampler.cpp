/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Image.h>
#include <LibSoftGPU/Sampler.h>
#include <math.h>

namespace SoftGPU {

static constexpr float fracf(float value)
{
    return value - floorf(value);
}

static constexpr float wrap_repeat(float value)
{
    return fracf(value);
}

[[maybe_unused]] static constexpr float wrap_clamp(float value)
{
    return clamp(value, 0.0f, 1.0f);
}

static constexpr float wrap_clamp_to_edge(float value, unsigned num_texels)
{
    float const clamp_limit = 1.f / (2 * num_texels);
    return clamp(value, clamp_limit, 1.0f - clamp_limit);
}

static constexpr float wrap_mirrored_repeat(float value, unsigned num_texels)
{
    float integer = floorf(value);
    float frac = value - integer;
    bool iseven = fmodf(integer, 2.0f) == 0.0f;
    return wrap_clamp_to_edge(iseven ? frac : 1 - frac, num_texels);
}

static constexpr float wrap(float value, TextureWrapMode mode, unsigned num_texels)
{
    switch (mode) {
    case TextureWrapMode::Repeat:
        return wrap_repeat(value);
    case TextureWrapMode::MirroredRepeat:
        return wrap_mirrored_repeat(value, num_texels);
    case TextureWrapMode::Clamp:
        if constexpr (CLAMP_DEPRECATED_BEHAVIOR) {
            return wrap_clamp(value);
        }
        return wrap_clamp_to_edge(value, num_texels);
    case TextureWrapMode::ClampToBorder:
    case TextureWrapMode::ClampToEdge:
        return wrap_clamp_to_edge(value, num_texels);
    default:
        VERIFY_NOT_REACHED();
    }
}

FloatVector4 Sampler::sample_2d(FloatVector2 const& uv) const
{
    if (m_config.bound_image.is_null())
        return { 0, 0, 0, 1 };

    auto const& image = *m_config.bound_image;

    unsigned const layer = 0;
    // FIXME: calculate actual mipmap level  to use
    unsigned const level = 0;

    unsigned width = image.level_width(level);
    unsigned height = image.level_height(level);

    float s = wrap(uv.x(), m_config.texture_wrap_u, width);
    float t = wrap(uv.y(), m_config.texture_wrap_v, height);

    float u = s * width;
    float v = t * height;

    if (m_config.texture_mag_filter == TextureFilter::Nearest) {
        unsigned i = min(static_cast<unsigned>(u), width - 1);
        unsigned j = min(static_cast<unsigned>(v), height - 1);
        return image.texel(layer, level, i, j, 0);
    }

    int i0 = m_config.texture_wrap_u == TextureWrapMode::Repeat ? static_cast<unsigned>(floorf(u - 0.5f)) % width : floorf(u - 0.5f);
    int j0 = m_config.texture_wrap_v == TextureWrapMode::Repeat ? static_cast<unsigned>(floorf(v - 0.5f)) % height : floorf(v - 0.5f);

    int i1 = m_config.texture_wrap_u == TextureWrapMode::Repeat ? (i0 + 1) % width : i0 + 1;
    int j1 = m_config.texture_wrap_v == TextureWrapMode::Repeat ? (j0 + 1) % height : j0 + 1;

    FloatVector4 t0, t1, t2, t3;

    if (m_config.texture_wrap_u == TextureWrapMode::Repeat && m_config.texture_wrap_v == TextureWrapMode::Repeat) {
        t0 = image.texel(layer, level, i0, j0, 0);
        t1 = image.texel(layer, level, i1, j0, 0);
        t2 = image.texel(layer, level, i0, j1, 0);
        t3 = image.texel(layer, level, i1, j1, 0);
    } else {
        int w = static_cast<int>(width);
        int h = static_cast<int>(height);
        t0 = (i0 < 0 || i0 >= w || j0 < 0 || j0 >= h) ? m_config.border_color : image.texel(layer, level, i0, j0, 0);
        t1 = (i1 < 0 || i1 >= w || j0 < 0 || j0 >= h) ? m_config.border_color : image.texel(layer, level, i1, j0, 0);
        t2 = (i0 < 0 || i0 >= w || j1 < 0 || j1 >= h) ? m_config.border_color : image.texel(layer, level, i0, j1, 0);
        t3 = (i1 < 0 || i1 >= w || j1 < 0 || j1 >= h) ? m_config.border_color : image.texel(layer, level, i1, j1, 0);
    }

    float const alpha = fracf(u - 0.5f);
    float const beta = fracf(v - 0.5f);

    auto const lerp_0 = mix(t0, t1, alpha);
    auto const lerp_1 = mix(t2, t3, alpha);
    return mix(lerp_0, lerp_1, beta);
}

}
