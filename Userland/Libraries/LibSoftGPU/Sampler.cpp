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

static f32x4 wrap(f32x4 value, GPU::TextureWrapMode mode, u32x4 num_texels)
{
    switch (mode) {
    case GPU::TextureWrapMode::Repeat:
        return wrap_repeat(value);
    case GPU::TextureWrapMode::MirroredRepeat:
        return wrap_mirrored_repeat(value, num_texels);
    case GPU::TextureWrapMode::Clamp:
        if constexpr (CLAMP_DEPRECATED_BEHAVIOR) {
            return wrap_clamp(value);
        }
        return wrap_clamp_to_edge(value, num_texels);
    case GPU::TextureWrapMode::ClampToBorder:
    case GPU::TextureWrapMode::ClampToEdge:
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

    auto const& image = *static_ptr_cast<Image>(m_config.bound_image);

    // FIXME: Make base level configurable with glTexParameteri(GL_TEXTURE_BASE_LEVEL, base_level)
    constexpr unsigned base_level = 0;

    // Determine the texture scale factor. See OpenGL 1.5 spec chapter 3.8.8.
    // FIXME: Static casting from u32 to float could silently truncate here.
    // u16 should be plenty enough for texture dimensions and would allow textures of up to 65536x65536x65536 pixels.
    auto texel_coordinates = uv;
    texel_coordinates.set_x(texel_coordinates.x() * static_cast<float>(image.level_width(base_level)));
    texel_coordinates.set_y(texel_coordinates.y() * static_cast<float>(image.level_height(base_level)));
    auto dtdx = ddx(texel_coordinates);
    auto dtdy = ddy(texel_coordinates);
    auto scale_factor = max(dtdx.dot(dtdx), dtdy.dot(dtdy));

    // FIXME: Here we simply determine the filter based on the single scale factor of the upper left pixel.
    // Actually, we could end up with different scale factors for each pixel. This however would break our
    // parallelisation as we could also end up with different filter modes per pixel.

    // Note: scale_factor approximates texels per pixel. This means a scale factor less than 1 indicates texture magnification.
    if (scale_factor[0] <= 1.f)
        return sample_2d_lod(uv, expand4(base_level), m_config.texture_mag_filter);

    if (m_config.mipmap_filter == GPU::MipMapFilter::None)
        return sample_2d_lod(uv, expand4(base_level), m_config.texture_min_filter);

    // FIXME: Instead of clamping to num_levels - 1, actually make the max mipmap level configurable with glTexParameteri(GL_TEXTURE_MAX_LEVEL, max_level)
    auto min_level = expand4(static_cast<float>(base_level));
    auto max_level = expand4(image.num_levels() - 1.0f);
    auto level = min(max(log2_approximate(scale_factor) * 0.5f, min_level), max_level);

    auto lower_level_texel = sample_2d_lod(uv, to_u32x4(level), m_config.texture_min_filter);

    if (m_config.mipmap_filter == GPU::MipMapFilter::Nearest)
        return lower_level_texel;

    auto higher_level_texel = sample_2d_lod(uv, to_u32x4(min(level + 1.f, max_level)), m_config.texture_min_filter);

    return mix(lower_level_texel, higher_level_texel, frac_int_range(level));
}

Vector4<AK::SIMD::f32x4> Sampler::sample_2d_lod(Vector2<AK::SIMD::f32x4> const& uv, AK::SIMD::u32x4 level, GPU::TextureFilter filter) const
{
    auto const& image = *static_ptr_cast<Image>(m_config.bound_image);
    u32x4 const layer = expand4(0u);

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

    if (filter == GPU::TextureFilter::Nearest) {
        u32x4 i = to_u32x4(u);
        u32x4 j = to_u32x4(v);
        u32x4 k = expand4(0u);

        i = image.width_is_power_of_two() ? i & width_mask : i % width;
        j = image.height_is_power_of_two() ? j & height_mask : j % height;

        return texel4(image, layer, level, i, j, k);
    }

    u -= 0.5f;
    v -= 0.5f;

    u32x4 i0 = to_u32x4(floor_int_range(u));
    u32x4 i1 = i0 + 1;
    u32x4 j0 = to_u32x4(floor_int_range(v));
    u32x4 j1 = j0 + 1;

    if (m_config.texture_wrap_u == GPU::TextureWrapMode::Repeat) {
        if (image.width_is_power_of_two()) {
            i0 = i0 & width_mask;
            i1 = i1 & width_mask;
        } else {
            i0 = i0 % width;
            i1 = i1 % width;
        }
    }

    if (m_config.texture_wrap_v == GPU::TextureWrapMode::Repeat) {
        if (image.height_is_power_of_two()) {
            j0 = j0 & height_mask;
            j1 = j1 & height_mask;
        } else {
            j0 = j0 % height;
            j1 = j1 % height;
        }
    }

    u32x4 k = expand4(0u);

    Vector4<f32x4> t0, t1, t2, t3;

    if (m_config.texture_wrap_u == GPU::TextureWrapMode::Repeat && m_config.texture_wrap_v == GPU::TextureWrapMode::Repeat) {
        t0 = texel4(image, layer, level, i0, j0, k);
        t1 = texel4(image, layer, level, i1, j0, k);
        t2 = texel4(image, layer, level, i0, j1, k);
        t3 = texel4(image, layer, level, i1, j1, k);
    } else {
        t1 = texel4border(image, layer, level, i1, j0, k, m_config.border_color, width, height);
        t0 = texel4border(image, layer, level, i0, j0, k, m_config.border_color, width, height);
        t2 = texel4border(image, layer, level, i0, j1, k, m_config.border_color, width, height);
        t3 = texel4border(image, layer, level, i1, j1, k, m_config.border_color, width, height);
    }

    f32x4 const alpha = frac_int_range(u);
    f32x4 const beta = frac_int_range(v);

    auto const lerp_0 = mix(t0, t1, alpha);
    auto const lerp_1 = mix(t2, t3, alpha);
    return mix(lerp_0, lerp_1, beta);
}

}
