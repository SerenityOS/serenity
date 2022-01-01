/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SIMDExtras.h>
#include <AK/SIMDMath.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Image.h>
#include <LibSoftGPU/SIMD.h>
#include <LibSoftGPU/Sampler.h>
#include <math.h>

namespace SoftGPU {

using AK::SIMD::f32x4;
using AK::SIMD::i32x4;
using AK::SIMD::u32x4;

using AK::SIMD::clamp;
using AK::SIMD::expand4;
using AK::SIMD::floor_int_range;
using AK::SIMD::frac_int_range;
using AK::SIMD::maskbits;
using AK::SIMD::to_f32x4;
using AK::SIMD::to_i32x4;
using AK::SIMD::to_u32x4;
using AK::SIMD::truncate_int_range;

static f32x4 wrap_repeat(f32x4 value)
{
    return frac_int_range(value);
}

[[maybe_unused]] static f32x4 wrap_clamp(f32x4 value)
{
    return clamp(value, expand4(0.0f), expand4(1.0f));
}

static f32x4 wrap_clamp_to_edge(f32x4 value, u32x4 num_texels)
{
    f32x4 const clamp_limit = 1.f / to_f32x4(2 * num_texels);
    return clamp(value, clamp_limit, 1.0f - clamp_limit);
}

static f32x4 wrap_mirrored_repeat(f32x4 value, u32x4 num_texels)
{
    f32x4 integer = floor_int_range(value);
    f32x4 frac = value - integer;
    auto is_odd = to_i32x4(integer) & 1;
    return wrap_clamp_to_edge(is_odd ? 1 - frac : frac, num_texels);
}

static f32x4 wrap(f32x4 value, TextureWrapMode mode, u32x4 num_texels)
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

ALWAYS_INLINE static Vector4<f32x4> texel4(Image const& image, u32x4 layer, u32x4 level, u32x4 x, u32x4 y, u32x4 z)
{
    auto t0 = image.texel(layer[0], level[0], x[0], y[0], z[0]);
    auto t1 = image.texel(layer[1], level[1], x[1], y[1], z[1]);
    auto t2 = image.texel(layer[2], level[2], x[2], y[2], z[2]);
    auto t3 = image.texel(layer[3], level[3], x[3], y[3], z[3]);

    return Vector4<f32x4> {
        f32x4 { t0.x(), t1.x(), t2.x(), t3.x() },
        f32x4 { t0.y(), t1.y(), t2.y(), t3.y() },
        f32x4 { t0.z(), t1.z(), t2.z(), t3.z() },
        f32x4 { t0.w(), t1.w(), t2.w(), t3.w() },
    };
}

ALWAYS_INLINE static Vector4<f32x4> texel4border(Image const& image, u32x4 layer, u32x4 level, u32x4 x, u32x4 y, u32x4 z, FloatVector4 const& border, u32x4 w, u32x4 h)
{
    auto border_mask = maskbits(x < 0 || x >= w || y < 0 || y >= h);

    auto t0 = border_mask & 1 ? border : image.texel(layer[0], level[0], x[0], y[0], z[0]);
    auto t1 = border_mask & 2 ? border : image.texel(layer[1], level[1], x[1], y[1], z[1]);
    auto t2 = border_mask & 4 ? border : image.texel(layer[2], level[2], x[2], y[2], z[2]);
    auto t3 = border_mask & 8 ? border : image.texel(layer[3], level[3], x[3], y[3], z[3]);

    return Vector4<f32x4> {
        f32x4 { t0.x(), t1.x(), t2.x(), t3.x() },
        f32x4 { t0.y(), t1.y(), t2.y(), t3.y() },
        f32x4 { t0.z(), t1.z(), t2.z(), t3.z() },
        f32x4 { t0.w(), t1.w(), t2.w(), t3.w() },
    };
}

Vector4<AK::SIMD::f32x4> Sampler::sample_2d(Vector2<AK::SIMD::f32x4> const& uv) const
{
    if (m_config.bound_image.is_null())
        return expand4(FloatVector4 { 1, 0, 0, 1 });

    auto const& image = *m_config.bound_image;

    u32x4 const layer = expand4(0u);
    // FIXME: calculate actual mipmap level  to use
    u32x4 const level = expand4(0u);

    u32x4 const width = {
        image.level_width(level[0]),
        image.level_width(level[1]),
        image.level_width(level[2]),
        image.level_width(level[3]),
    };
    u32x4 const height = {
        image.level_height(level[0]),
        image.level_height(level[1]),
        image.level_height(level[2]),
        image.level_height(level[3]),
    };

    u32x4 width_mask = width - 1;
    u32x4 height_mask = height - 1;

    f32x4 s = wrap(uv.x(), m_config.texture_wrap_u, width);
    f32x4 t = wrap(uv.y(), m_config.texture_wrap_v, height);

    f32x4 u = s * to_f32x4(width);
    f32x4 v = t * to_f32x4(height);

    if (m_config.texture_mag_filter == TextureFilter::Nearest) {
        u32x4 i = to_u32x4(u);
        u32x4 j = to_u32x4(v);
        u32x4 k = expand4(0u);

        i = image.width_is_power_of_two() ? i & width_mask : i % width;
        j = image.height_is_power_of_two() ? j & height_mask : j % height;

        return texel4(image, layer, level, i, j, k);
    }

    u -= 0.5f;
    v -= 0.5f;

    i32x4 i0 = to_i32x4(floor_int_range(u));
    i32x4 i1 = i0 + 1;
    i32x4 j0 = to_i32x4(floor_int_range(v));
    i32x4 j1 = j0 + 1;

    if (m_config.texture_wrap_u == TextureWrapMode::Repeat) {
        if (image.width_is_power_of_two()) {
            i0 = (i32x4)(i0 & width_mask);
            i1 = (i32x4)(i1 & width_mask);
        } else {
            i0 = (i32x4)(i0 % width);
            i1 = (i32x4)(i1 % width);
        }
    }

    if (m_config.texture_wrap_v == TextureWrapMode::Repeat) {
        if (image.height_is_power_of_two()) {
            j0 = (i32x4)(j0 & height_mask);
            j1 = (i32x4)(j1 & height_mask);
        } else {
            j0 = (i32x4)(j0 % height);
            j1 = (i32x4)(j1 % height);
        }
    }

    u32x4 k = expand4(0u);

    Vector4<f32x4> t0, t1, t2, t3;

    if (m_config.texture_wrap_u == TextureWrapMode::Repeat && m_config.texture_wrap_v == TextureWrapMode::Repeat) {
        t0 = texel4(image, layer, level, to_u32x4(i0), to_u32x4(j0), k);
        t1 = texel4(image, layer, level, to_u32x4(i1), to_u32x4(j0), k);
        t2 = texel4(image, layer, level, to_u32x4(i0), to_u32x4(j1), k);
        t3 = texel4(image, layer, level, to_u32x4(i1), to_u32x4(j1), k);
    } else {
        t0 = texel4border(image, layer, level, to_u32x4(i0), to_u32x4(j0), k, m_config.border_color, width, height);
        t1 = texel4border(image, layer, level, to_u32x4(i1), to_u32x4(j0), k, m_config.border_color, width, height);
        t2 = texel4border(image, layer, level, to_u32x4(i0), to_u32x4(j1), k, m_config.border_color, width, height);
        t3 = texel4border(image, layer, level, to_u32x4(i1), to_u32x4(j1), k, m_config.border_color, width, height);
    }

    f32x4 const alpha = frac_int_range(u);
    f32x4 const beta = frac_int_range(v);

    auto const lerp_0 = mix(t0, t1, alpha);
    auto const lerp_1 = mix(t2, t3, alpha);
    return mix(lerp_0, lerp_1, beta);
}

}
