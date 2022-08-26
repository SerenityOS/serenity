/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <AK/Error.h>
#include <AK/Math.h>
#include <AK/NumericLimits.h>
#include <AK/SIMDExtras.h>
#include <AK/SIMDMath.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector3.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Device.h>
#include <LibSoftGPU/PixelConverter.h>
#include <LibSoftGPU/PixelQuad.h>
#include <LibSoftGPU/SIMD.h>
#include <math.h>

namespace SoftGPU {

static i64 g_num_rasterized_triangles;
static i64 g_num_pixels;
static i64 g_num_pixels_shaded;
static i64 g_num_pixels_blended;
static i64 g_num_sampler_calls;
static i64 g_num_stencil_writes;
static i64 g_num_quads;

using AK::abs;
using AK::SIMD::any;
using AK::SIMD::exp;
using AK::SIMD::expand4;
using AK::SIMD::f32x4;
using AK::SIMD::i32x4;
using AK::SIMD::load4_masked;
using AK::SIMD::maskbits;
using AK::SIMD::maskcount;
using AK::SIMD::store4_masked;
using AK::SIMD::to_f32x4;
using AK::SIMD::to_u32x4;
using AK::SIMD::u32x4;

static constexpr int subpixel_factor = 1 << SUBPIXEL_BITS;

// Returns positive values for counter-clockwise rotation of vertices. Note that it returns the
// area of a parallelogram with sides {a, b} and {b, c}, so _double_ the area of the triangle {a, b, c}.
constexpr static i32 edge_function(IntVector2 const& a, IntVector2 const& b, IntVector2 const& c)
{
    return (c.y() - a.y()) * (b.x() - a.x()) - (c.x() - a.x()) * (b.y() - a.y());
}

constexpr static i32x4 edge_function4(IntVector2 const& a, IntVector2 const& b, Vector2<i32x4> const& c)
{
    return (c.y() - a.y()) * (b.x() - a.x()) - (c.x() - a.x()) * (b.y() - a.y());
}

template<typename T, typename U>
constexpr static auto interpolate(T const& v0, T const& v1, T const& v2, Vector3<U> const& barycentric_coords)
{
    return v0 * barycentric_coords.x() + v1 * barycentric_coords.y() + v2 * barycentric_coords.z();
}

static GPU::ColorType to_argb32(FloatVector4 const& color)
{
    auto clamped = color.clamped(0.0f, 1.0f);
    auto r = static_cast<u8>(clamped.x() * 255);
    auto g = static_cast<u8>(clamped.y() * 255);
    auto b = static_cast<u8>(clamped.z() * 255);
    auto a = static_cast<u8>(clamped.w() * 255);
    return a << 24 | r << 16 | g << 8 | b;
}

ALWAYS_INLINE static u32x4 to_argb32(Vector4<f32x4> const& color)
{
    auto clamped = color.clamped(expand4(0.0f), expand4(1.0f));
    auto r = to_u32x4(clamped.x() * 255);
    auto g = to_u32x4(clamped.y() * 255);
    auto b = to_u32x4(clamped.z() * 255);
    auto a = to_u32x4(clamped.w() * 255);

    return a << 24 | r << 16 | g << 8 | b;
}

static Vector4<f32x4> to_vec4(u32x4 bgra)
{
    auto constexpr one_over_255 = expand4(1.0f / 255);
    return {
        to_f32x4((bgra >> 16) & 0xff) * one_over_255,
        to_f32x4((bgra >> 8) & 0xff) * one_over_255,
        to_f32x4(bgra & 0xff) * one_over_255,
        to_f32x4((bgra >> 24) & 0xff) * one_over_255,
    };
}

void Device::setup_blend_factors()
{
    m_alpha_blend_factors = {};

    switch (m_options.blend_source_factor) {
    case GPU::BlendFactor::Zero:
        break;
    case GPU::BlendFactor::One:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        break;
    case GPU::BlendFactor::SrcColor:
        m_alpha_blend_factors.src_factor_src_color = 1;
        break;
    case GPU::BlendFactor::OneMinusSrcColor:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_src_color = -1;
        break;
    case GPU::BlendFactor::SrcAlpha:
        m_alpha_blend_factors.src_factor_src_alpha = 1;
        break;
    case GPU::BlendFactor::OneMinusSrcAlpha:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_src_alpha = -1;
        break;
    case GPU::BlendFactor::DstAlpha:
        m_alpha_blend_factors.src_factor_dst_alpha = 1;
        break;
    case GPU::BlendFactor::OneMinusDstAlpha:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_dst_alpha = -1;
        break;
    case GPU::BlendFactor::DstColor:
        m_alpha_blend_factors.src_factor_dst_color = 1;
        break;
    case GPU::BlendFactor::OneMinusDstColor:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_dst_color = -1;
        break;
    case GPU::BlendFactor::SrcAlphaSaturate:
    default:
        VERIFY_NOT_REACHED();
    }

    switch (m_options.blend_destination_factor) {
    case GPU::BlendFactor::Zero:
        break;
    case GPU::BlendFactor::One:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        break;
    case GPU::BlendFactor::SrcColor:
        m_alpha_blend_factors.dst_factor_src_color = 1;
        break;
    case GPU::BlendFactor::OneMinusSrcColor:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_src_color = -1;
        break;
    case GPU::BlendFactor::SrcAlpha:
        m_alpha_blend_factors.dst_factor_src_alpha = 1;
        break;
    case GPU::BlendFactor::OneMinusSrcAlpha:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_src_alpha = -1;
        break;
    case GPU::BlendFactor::DstAlpha:
        m_alpha_blend_factors.dst_factor_dst_alpha = 1;
        break;
    case GPU::BlendFactor::OneMinusDstAlpha:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_dst_alpha = -1;
        break;
    case GPU::BlendFactor::DstColor:
        m_alpha_blend_factors.dst_factor_dst_color = 1;
        break;
    case GPU::BlendFactor::OneMinusDstColor:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_dst_color = -1;
        break;
    case GPU::BlendFactor::SrcAlphaSaturate:
    default:
        VERIFY_NOT_REACHED();
    }
}

template<typename CB1, typename CB2, typename CB3>
ALWAYS_INLINE void Device::rasterize(Gfx::IntRect& render_bounds, CB1 set_coverage_mask, CB2 set_quad_depth, CB3 set_quad_attributes)
{
    // Return if alpha testing is a no-op
    if (m_options.enable_alpha_test && m_options.alpha_test_func == GPU::AlphaTestFunction::Never)
        return;

    // Buffers
    auto color_buffer = m_frame_buffer->color_buffer();
    auto depth_buffer = m_frame_buffer->depth_buffer();
    auto stencil_buffer = m_frame_buffer->stencil_buffer();

    // Stencil configuration and writing
    auto const& stencil_configuration = m_stencil_configuration[GPU::Face::Front];
    auto const stencil_reference_value = stencil_configuration.reference_value & stencil_configuration.test_mask;

    auto write_to_stencil = [](GPU::StencilType* stencil_ptrs[4], i32x4 stencil_value, GPU::StencilOperation op, GPU::StencilType reference_value, GPU::StencilType write_mask, i32x4 pixel_mask) {
        if (write_mask == 0 || op == GPU::StencilOperation::Keep)
            return;

        switch (op) {
        case GPU::StencilOperation::Decrement:
            stencil_value = (stencil_value & ~write_mask) | (max(stencil_value - 1, expand4(0)) & write_mask);
            break;
        case GPU::StencilOperation::DecrementWrap:
            stencil_value = (stencil_value & ~write_mask) | (((stencil_value - 1) & 0xFF) & write_mask);
            break;
        case GPU::StencilOperation::Increment:
            stencil_value = (stencil_value & ~write_mask) | (min(stencil_value + 1, expand4(0xFF)) & write_mask);
            break;
        case GPU::StencilOperation::IncrementWrap:
            stencil_value = (stencil_value & ~write_mask) | (((stencil_value + 1) & 0xFF) & write_mask);
            break;
        case GPU::StencilOperation::Invert:
            stencil_value ^= write_mask;
            break;
        case GPU::StencilOperation::Replace:
            stencil_value = (stencil_value & ~write_mask) | (reference_value & write_mask);
            break;
        case GPU::StencilOperation::Zero:
            stencil_value &= ~write_mask;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        INCREASE_STATISTICS_COUNTER(g_num_stencil_writes, maskcount(pixel_mask));
        store4_masked(stencil_value, stencil_ptrs[0], stencil_ptrs[1], stencil_ptrs[2], stencil_ptrs[3], pixel_mask);
    };

    // Limit rendering to framebuffer and scissor rects
    render_bounds.intersect(m_frame_buffer->rect());
    if (m_options.scissor_enabled)
        render_bounds.intersect(m_options.scissor_box);

    // Quad bounds
    auto const render_bounds_left = render_bounds.left();
    auto const render_bounds_right = render_bounds.right();
    auto const render_bounds_top = render_bounds.top();
    auto const render_bounds_bottom = render_bounds.bottom();
    auto const qx0 = render_bounds_left & ~1;
    auto const qx1 = render_bounds_right & ~1;
    auto const qy0 = render_bounds_top & ~1;
    auto const qy1 = render_bounds_bottom & ~1;

    // Rasterize all quads
    // FIXME: this could be embarrassingly parallel
    for (int qy = qy0; qy <= qy1; qy += 2) {
        for (int qx = qx0; qx <= qx1; qx += 2) {
            PixelQuad quad;
            quad.screen_coordinates = {
                i32x4 { qx, qx + 1, qx, qx + 1 },
                i32x4 { qy, qy, qy + 1, qy + 1 },
            };

            // Set coverage mask and test against render bounds
            set_coverage_mask(quad);
            quad.mask &= quad.screen_coordinates.x() >= render_bounds_left
                && quad.screen_coordinates.x() <= render_bounds_right
                && quad.screen_coordinates.y() >= render_bounds_top
                && quad.screen_coordinates.y() <= render_bounds_bottom;
            auto coverage_bits = maskbits(quad.mask);
            if (coverage_bits == 0)
                continue;

            INCREASE_STATISTICS_COUNTER(g_num_quads, 1);
            INCREASE_STATISTICS_COUNTER(g_num_pixels, maskcount(quad.mask));

            // Stencil testing
            GPU::StencilType* stencil_ptrs[4];
            i32x4 stencil_value;
            if (m_options.enable_stencil_test) {
                stencil_ptrs[0] = coverage_bits & 1 ? &stencil_buffer->scanline(qy)[qx] : nullptr;
                stencil_ptrs[1] = coverage_bits & 2 ? &stencil_buffer->scanline(qy)[qx + 1] : nullptr;
                stencil_ptrs[2] = coverage_bits & 4 ? &stencil_buffer->scanline(qy + 1)[qx] : nullptr;
                stencil_ptrs[3] = coverage_bits & 8 ? &stencil_buffer->scanline(qy + 1)[qx + 1] : nullptr;

                stencil_value = load4_masked(stencil_ptrs[0], stencil_ptrs[1], stencil_ptrs[2], stencil_ptrs[3], quad.mask);
                stencil_value &= stencil_configuration.test_mask;

                i32x4 stencil_test_passed;
                switch (stencil_configuration.test_function) {
                case GPU::StencilTestFunction::Always:
                    stencil_test_passed = expand4(~0);
                    break;
                case GPU::StencilTestFunction::Equal:
                    stencil_test_passed = stencil_value == stencil_reference_value;
                    break;
                case GPU::StencilTestFunction::Greater:
                    stencil_test_passed = stencil_value > stencil_reference_value;
                    break;
                case GPU::StencilTestFunction::GreaterOrEqual:
                    stencil_test_passed = stencil_value >= stencil_reference_value;
                    break;
                case GPU::StencilTestFunction::Less:
                    stencil_test_passed = stencil_value < stencil_reference_value;
                    break;
                case GPU::StencilTestFunction::LessOrEqual:
                    stencil_test_passed = stencil_value <= stencil_reference_value;
                    break;
                case GPU::StencilTestFunction::Never:
                    stencil_test_passed = expand4(0);
                    break;
                case GPU::StencilTestFunction::NotEqual:
                    stencil_test_passed = stencil_value != stencil_reference_value;
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }

                // Update stencil buffer for pixels that failed the stencil test
                write_to_stencil(
                    stencil_ptrs,
                    stencil_value,
                    stencil_configuration.on_stencil_test_fail,
                    stencil_reference_value,
                    stencil_configuration.write_mask,
                    quad.mask & ~stencil_test_passed);

                // Update coverage mask + early quad rejection
                quad.mask &= stencil_test_passed;
                coverage_bits = maskbits(quad.mask);
                if (coverage_bits == 0)
                    continue;
            }

            // Depth testing
            GPU::DepthType* depth_ptrs[4] = {
                coverage_bits & 1 ? &depth_buffer->scanline(qy)[qx] : nullptr,
                coverage_bits & 2 ? &depth_buffer->scanline(qy)[qx + 1] : nullptr,
                coverage_bits & 4 ? &depth_buffer->scanline(qy + 1)[qx] : nullptr,
                coverage_bits & 8 ? &depth_buffer->scanline(qy + 1)[qx + 1] : nullptr,
            };
            if (m_options.enable_depth_test) {
                set_quad_depth(quad);

                auto depth = load4_masked(depth_ptrs[0], depth_ptrs[1], depth_ptrs[2], depth_ptrs[3], quad.mask);
                i32x4 depth_test_passed;
                switch (m_options.depth_func) {
                case GPU::DepthTestFunction::Always:
                    depth_test_passed = expand4(~0);
                    break;
                case GPU::DepthTestFunction::Never:
                    depth_test_passed = expand4(0);
                    break;
                case GPU::DepthTestFunction::Greater:
                    depth_test_passed = quad.depth > depth;
                    break;
                case GPU::DepthTestFunction::GreaterOrEqual:
                    depth_test_passed = quad.depth >= depth;
                    break;
                case GPU::DepthTestFunction::NotEqual:
#ifdef __SSE__
                    depth_test_passed = quad.depth != depth;
#else
                    depth_test_passed = i32x4 {
                        bit_cast<u32>(quad.depth[0]) != bit_cast<u32>(depth[0]) ? -1 : 0,
                        bit_cast<u32>(quad.depth[1]) != bit_cast<u32>(depth[1]) ? -1 : 0,
                        bit_cast<u32>(quad.depth[2]) != bit_cast<u32>(depth[2]) ? -1 : 0,
                        bit_cast<u32>(quad.depth[3]) != bit_cast<u32>(depth[3]) ? -1 : 0,
                    };
#endif
                    break;
                case GPU::DepthTestFunction::Equal:
#ifdef __SSE__
                    depth_test_passed = quad.depth == depth;
#else
                    //
                    // This is an interesting quirk that occurs due to us using the x87 FPU when Serenity is
                    // compiled for the i686 target. When we calculate our depth value to be stored in the buffer,
                    // it is an 80-bit x87 floating point number, however, when stored into the depth buffer, this is
                    // truncated to 32 bits. This 38 bit loss of precision means that when x87 `FCOMP` is eventually
                    // used here the comparison fails.
                    // This could be solved by using a `long double` for the depth buffer, however this would take
                    // up significantly more space and is completely overkill for a depth buffer. As such, comparing
                    // the first 32-bits of this depth value is "good enough" that if we get a hit on it being
                    // equal, we can pretty much guarantee that it's actually equal.
                    //
                    depth_test_passed = i32x4 {
                        bit_cast<u32>(quad.depth[0]) == bit_cast<u32>(depth[0]) ? -1 : 0,
                        bit_cast<u32>(quad.depth[1]) == bit_cast<u32>(depth[1]) ? -1 : 0,
                        bit_cast<u32>(quad.depth[2]) == bit_cast<u32>(depth[2]) ? -1 : 0,
                        bit_cast<u32>(quad.depth[3]) == bit_cast<u32>(depth[3]) ? -1 : 0,
                    };
#endif
                    break;
                case GPU::DepthTestFunction::LessOrEqual:
                    depth_test_passed = quad.depth <= depth;
                    break;
                case GPU::DepthTestFunction::Less:
                    depth_test_passed = quad.depth < depth;
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }

                // Update stencil buffer for pixels that failed the depth test
                if (m_options.enable_stencil_test) {
                    write_to_stencil(
                        stencil_ptrs,
                        stencil_value,
                        stencil_configuration.on_depth_test_fail,
                        stencil_reference_value,
                        stencil_configuration.write_mask,
                        quad.mask & ~depth_test_passed);
                }

                // Update coverage mask + early quad rejection
                quad.mask &= depth_test_passed;
                coverage_bits = maskbits(quad.mask);
                if (coverage_bits == 0)
                    continue;
            }

            // Update stencil buffer for passed pixels
            if (m_options.enable_stencil_test) {
                write_to_stencil(
                    stencil_ptrs,
                    stencil_value,
                    stencil_configuration.on_pass,
                    stencil_reference_value,
                    stencil_configuration.write_mask,
                    quad.mask);
            }

            INCREASE_STATISTICS_COUNTER(g_num_pixels_shaded, maskcount(quad.mask));

            set_quad_attributes(quad);
            shade_fragments(quad);

            // Alpha testing
            if (m_options.enable_alpha_test) {
                test_alpha(quad);
                coverage_bits = maskbits(quad.mask);
                if (coverage_bits == 0)
                    continue;
            }

            // Write to depth buffer
            if (m_options.enable_depth_test && m_options.enable_depth_write)
                store4_masked(quad.depth, depth_ptrs[0], depth_ptrs[1], depth_ptrs[2], depth_ptrs[3], quad.mask);

            // We will not update the color buffer at all
            if ((m_options.color_mask == 0) || !m_options.enable_color_write)
                continue;

            GPU::ColorType* color_ptrs[4] = {
                coverage_bits & 1 ? &color_buffer->scanline(qy)[qx] : nullptr,
                coverage_bits & 2 ? &color_buffer->scanline(qy)[qx + 1] : nullptr,
                coverage_bits & 4 ? &color_buffer->scanline(qy + 1)[qx] : nullptr,
                coverage_bits & 8 ? &color_buffer->scanline(qy + 1)[qx + 1] : nullptr,
            };

            u32x4 dst_u32;
            if (m_options.enable_blending || m_options.color_mask != 0xffffffff)
                dst_u32 = load4_masked(color_ptrs[0], color_ptrs[1], color_ptrs[2], color_ptrs[3], quad.mask);

            if (m_options.enable_blending) {
                INCREASE_STATISTICS_COUNTER(g_num_pixels_blended, maskcount(quad.mask));

                // Blend color values from pixel_staging into color_buffer
                auto const& src = quad.out_color;
                auto dst = to_vec4(dst_u32);

                auto src_factor = expand4(m_alpha_blend_factors.src_constant)
                    + src * m_alpha_blend_factors.src_factor_src_color
                    + Vector4<f32x4> { src.w(), src.w(), src.w(), src.w() } * m_alpha_blend_factors.src_factor_src_alpha
                    + dst * m_alpha_blend_factors.src_factor_dst_color
                    + Vector4<f32x4> { dst.w(), dst.w(), dst.w(), dst.w() } * m_alpha_blend_factors.src_factor_dst_alpha;

                auto dst_factor = expand4(m_alpha_blend_factors.dst_constant)
                    + src * m_alpha_blend_factors.dst_factor_src_color
                    + Vector4<f32x4> { src.w(), src.w(), src.w(), src.w() } * m_alpha_blend_factors.dst_factor_src_alpha
                    + dst * m_alpha_blend_factors.dst_factor_dst_color
                    + Vector4<f32x4> { dst.w(), dst.w(), dst.w(), dst.w() } * m_alpha_blend_factors.dst_factor_dst_alpha;

                quad.out_color = src * src_factor + dst * dst_factor;
            }

            if (m_options.color_mask == 0xffffffff)
                store4_masked(to_argb32(quad.out_color), color_ptrs[0], color_ptrs[1], color_ptrs[2], color_ptrs[3], quad.mask);
            else
                store4_masked((to_argb32(quad.out_color) & m_options.color_mask) | (dst_u32 & ~m_options.color_mask), color_ptrs[0], color_ptrs[1], color_ptrs[2], color_ptrs[3], quad.mask);
        }
    }
}

void Device::rasterize_line_aliased(GPU::Vertex& from, GPU::Vertex& to)
{
    // FIXME: implement aliased lines; for now we fall back to anti-aliased logic
    rasterize_line_antialiased(from, to);
}

void Device::rasterize_line_antialiased(GPU::Vertex& from, GPU::Vertex& to)
{
    auto const from_coords = from.window_coordinates.xy();
    auto const to_coords = to.window_coordinates.xy();
    auto const line_width = ceilf(m_options.line_width);
    auto const line_radius = line_width / 2;

    auto render_bounds = Gfx::IntRect {
        min(from_coords.x(), to_coords.x()),
        min(from_coords.y(), to_coords.y()),
        abs(from_coords.x() - to_coords.x()) + 1,
        abs(from_coords.y() - to_coords.y()) + 1,
    };
    render_bounds.inflate(line_width, line_width);

    auto const from_coords4 = expand4(from_coords);
    auto const line_vector = to_coords - from_coords;
    auto const line_vector4 = expand4(line_vector);
    auto const line_dot4 = expand4(line_vector.dot(line_vector));

    auto const from_depth4 = expand4(from.window_coordinates.z());
    auto const to_depth4 = expand4(to.window_coordinates.z());

    auto const from_color4 = expand4(from.color);
    auto const from_fog_depth4 = expand4(abs(from.eye_coordinates.z()));

    // Rasterize using a 2D signed distance field for a line segment
    // FIXME: performance-wise, this might be the absolute worst way to draw an anti-aliased line
    f32x4 distance_along_line;
    rasterize(
        render_bounds,
        [&from_coords4, &distance_along_line, &line_vector4, &line_dot4, &line_radius](auto& quad) {
            auto const screen_coordinates4 = to_vec2_f32x4(quad.screen_coordinates);
            auto const pixel_vector = screen_coordinates4 - from_coords4;
            distance_along_line = AK::SIMD::clamp(pixel_vector.dot(line_vector4) / line_dot4, 0.f, 1.f);
            auto distance_to_line = length(pixel_vector - line_vector4 * distance_along_line) - line_radius;

            // Add .5f to the distance so coverage transitions half a pixel before the actual border
            quad.coverage = 1.f - AK::SIMD::clamp(distance_to_line + 0.5f, 0.f, 1.f);
            quad.mask = quad.coverage > 0.f;
        },
        [&from_depth4, &to_depth4, &distance_along_line](auto& quad) {
            quad.depth = mix(from_depth4, to_depth4, distance_along_line);
        },
        [&from_color4, &from, &from_fog_depth4](auto& quad) {
            // FIXME: interpolate color, tex coords and fog depth along the distance of the line
            //        in clip space (i.e. NOT distance_from_line)
            quad.vertex_color = from_color4;
            for (size_t i = 0; i < GPU::NUM_SAMPLERS; ++i)
                quad.texture_coordinates[i] = expand4(from.tex_coords[i]);
            quad.fog_depth = from_fog_depth4;
        });
}

void Device::rasterize_line(GPU::Vertex& from, GPU::Vertex& to)
{
    if (m_options.line_smooth)
        rasterize_line_antialiased(from, to);
    else
        rasterize_line_aliased(from, to);
}

void Device::rasterize_point_aliased(GPU::Vertex& point)
{
    // Determine aliased point width
    constexpr size_t maximum_aliased_point_size = 64;
    auto point_width = clamp(round_to<int>(m_options.point_size), 1, maximum_aliased_point_size);

    // Determine aliased center coordinates
    IntVector2 point_center;
    if (point_width % 2 == 1)
        point_center = point.window_coordinates.xy().to_type<int>();
    else
        point_center = (point.window_coordinates.xy() + FloatVector2 { .5f, .5f }).to_type<int>();

    // Aliased points are rects; calculate boundaries around center
    auto point_rect = Gfx::IntRect {
        point_center.x() - point_width / 2,
        point_center.y() - point_width / 2,
        point_width,
        point_width,
    };

    // Rasterize the point as a rect
    rasterize(
        point_rect,
        [](auto& quad) {
            // We already passed in point_rect, so this doesn't matter
            quad.mask = expand4(~0);
        },
        [&point](auto& quad) {
            quad.depth = expand4(point.window_coordinates.z());
        },
        [&point](auto& quad) {
            quad.vertex_color = expand4(point.color);
            for (size_t i = 0; i < GPU::NUM_SAMPLERS; ++i)
                quad.texture_coordinates[i] = expand4(point.tex_coords[i]);
            quad.fog_depth = expand4(abs(point.eye_coordinates.z()));
        });
}

void Device::rasterize_point_antialiased(GPU::Vertex& point)
{
    auto const center = point.window_coordinates.xy();
    auto const center4 = expand4(center);
    auto const radius = m_options.point_size / 2;

    auto render_bounds = Gfx::IntRect {
        center.x() - radius,
        center.y() - radius,
        radius * 2 + 1,
        radius * 2 + 1,
    };

    // Rasterize using a 2D signed distance field for a circle
    rasterize(
        render_bounds,
        [&center4, &radius](auto& quad) {
            auto screen_coords = to_vec2_f32x4(quad.screen_coordinates);
            auto distance_to_point = length(center4 - screen_coords) - radius;

            // Add .5f to the distance so coverage transitions half a pixel before the actual border
            quad.coverage = 1.f - AK::SIMD::clamp(distance_to_point + .5f, 0.f, 1.f);
            quad.mask = quad.coverage > 0.f;
        },
        [&point](auto& quad) {
            quad.depth = expand4(point.window_coordinates.z());
        },
        [&point](auto& quad) {
            quad.vertex_color = expand4(point.color);
            for (size_t i = 0; i < GPU::NUM_SAMPLERS; ++i)
                quad.texture_coordinates[i] = expand4(point.tex_coords[i]);
            quad.fog_depth = expand4(abs(point.eye_coordinates.z()));
        });
}

void Device::rasterize_point(GPU::Vertex& point)
{
    // Divide texture coordinates R, S and T by Q
    for (size_t i = 0; i < GPU::NUM_SAMPLERS; ++i) {
        auto& tex_coord = point.tex_coords[i];
        auto one_over_w = 1 / tex_coord.w();
        tex_coord = {
            tex_coord.x() * one_over_w,
            tex_coord.y() * one_over_w,
            tex_coord.z() * one_over_w,
            tex_coord.w(),
        };
    }

    if (m_options.point_smooth)
        rasterize_point_antialiased(point);
    else
        rasterize_point_aliased(point);
}

void Device::rasterize_triangle(Triangle& triangle)
{
    INCREASE_STATISTICS_COUNTER(g_num_rasterized_triangles, 1);

    auto v0 = (triangle.vertices[0].window_coordinates.xy() * subpixel_factor).to_rounded<int>();
    auto v1 = (triangle.vertices[1].window_coordinates.xy() * subpixel_factor).to_rounded<int>();
    auto v2 = (triangle.vertices[2].window_coordinates.xy() * subpixel_factor).to_rounded<int>();

    auto triangle_area = edge_function(v0, v1, v2);
    if (triangle_area == 0)
        return;

    // Perform face culling
    if (m_options.enable_culling) {
        bool is_front = (m_options.front_face == GPU::WindingOrder::CounterClockwise ? triangle_area > 0 : triangle_area < 0);

        if (!is_front && m_options.cull_back)
            return;

        if (is_front && m_options.cull_front)
            return;
    }

    // Force counter-clockwise ordering of vertices
    if (triangle_area < 0) {
        swap(triangle.vertices[0], triangle.vertices[1]);
        swap(v0, v1);
        triangle_area *= -1;
    }

    auto const& vertex0 = triangle.vertices[0];
    auto const& vertex1 = triangle.vertices[1];
    auto const& vertex2 = triangle.vertices[2];

    auto const one_over_area = 1.0f / triangle_area;

    // This function calculates the 3 edge values for the pixel relative to the triangle.
    auto calculate_edge_values4 = [v0, v1, v2](Vector2<i32x4> const& p) -> Vector3<i32x4> {
        return {
            edge_function4(v1, v2, p),
            edge_function4(v2, v0, p),
            edge_function4(v0, v1, p),
        };
    };

    // Zero is used in testing against edge values below, applying the "top-left rule". If a pixel
    // lies exactly on an edge shared by two triangles, we only render that pixel if the edge in
    // question is a "top" or "left" edge. By setting either a 1 or 0, we effectively change the
    // comparisons against the edge values below from "> 0" into ">= 0".
    IntVector3 const zero {
        (v2.y() < v1.y() || (v2.y() == v1.y() && v2.x() < v1.x())) ? 0 : 1,
        (v0.y() < v2.y() || (v0.y() == v2.y() && v0.x() < v2.x())) ? 0 : 1,
        (v1.y() < v0.y() || (v1.y() == v0.y() && v1.x() < v0.x())) ? 0 : 1,
    };

    // This function tests whether a point as identified by its 3 edge values lies within the triangle
    auto test_point4 = [zero](Vector3<i32x4> const& edges) -> i32x4 {
        return edges.x() >= zero.x()
            && edges.y() >= zero.y()
            && edges.z() >= zero.z();
    };

    // Calculate render bounds based on the triangle's vertices
    Gfx::IntRect render_bounds;
    render_bounds.set_left(min(min(v0.x(), v1.x()), v2.x()) / subpixel_factor);
    render_bounds.set_right(max(max(v0.x(), v1.x()), v2.x()) / subpixel_factor);
    render_bounds.set_top(min(min(v0.y(), v1.y()), v2.y()) / subpixel_factor);
    render_bounds.set_bottom(max(max(v0.y(), v1.y()), v2.y()) / subpixel_factor);

    // Calculate depth of fragment for fog;
    // OpenGL 1.5 chapter 3.10: "An implementation may choose to approximate the
    // eye-coordinate distance from the eye to each fragment center by |Ze|."
    Vector3<f32x4> fog_depth;
    if (m_options.fog_enabled) {
        fog_depth = {
            expand4(abs(vertex0.eye_coordinates.z())),
            expand4(abs(vertex1.eye_coordinates.z())),
            expand4(abs(vertex2.eye_coordinates.z())),
        };
    }

    auto const half_pixel_offset = Vector2<i32x4> { expand4(subpixel_factor / 2), expand4(subpixel_factor / 2) };

    auto const window_w_coordinates = Vector3<f32x4> {
        expand4(vertex0.window_coordinates.w()),
        expand4(vertex1.window_coordinates.w()),
        expand4(vertex2.window_coordinates.w()),
    };

    // Calculate depth offset to apply
    float depth_offset = 0.f;
    if (m_options.depth_offset_enabled) {
        // Edge value deltas
        auto edge_value_step_x = FloatVector3 {
            static_cast<float>(v1.y() - v2.y()),
            static_cast<float>(v2.y() - v0.y()),
            static_cast<float>(v0.y() - v1.y()),
        };
        auto edge_value_step_y = FloatVector3 {
            static_cast<float>(v2.x() - v1.x()),
            static_cast<float>(v0.x() - v2.x()),
            static_cast<float>(v1.x() - v0.x()),
        };

        // Barycentric deltas
        auto barycentric_step_x = edge_value_step_x * one_over_area;
        auto barycentric_step_y = edge_value_step_y * one_over_area;

        // Depth delta vector and slope (magnitude)
        auto depth_coordinates = FloatVector3 {
            vertex0.window_coordinates.z(),
            vertex1.window_coordinates.z(),
            vertex2.window_coordinates.z(),
        };
        auto depth_step = FloatVector2 {
            depth_coordinates.dot(barycentric_step_x),
            depth_coordinates.dot(barycentric_step_y),
        };
        auto depth_max_slope = depth_step.length();

        // Calculate total depth offset
        depth_offset = depth_max_slope * m_options.depth_offset_factor + NumericLimits<float>::epsilon() * m_options.depth_offset_constant;
    }

    auto const window_z_coordinates = Vector3<f32x4> {
        expand4(vertex0.window_coordinates.z() + depth_offset),
        expand4(vertex1.window_coordinates.z() + depth_offset),
        expand4(vertex2.window_coordinates.z() + depth_offset),
    };

    rasterize(
        render_bounds,
        [&](auto& quad) {
            auto edge_values = calculate_edge_values4(quad.screen_coordinates * subpixel_factor + half_pixel_offset);
            quad.mask = test_point4(edge_values);

            quad.barycentrics = {
                to_f32x4(edge_values.x()),
                to_f32x4(edge_values.y()),
                to_f32x4(edge_values.z()),
            };
        },
        [&](auto& quad) {
            // Determine each edge's ratio to the total area
            quad.barycentrics = quad.barycentrics * one_over_area;

            // Because the Z coordinates were divided by W, we can interpolate between them
            quad.depth = AK::SIMD::clamp(window_z_coordinates.dot(quad.barycentrics), 0.f, 1.f);
        },
        [&](auto& quad) {
            auto const interpolated_reciprocal_w = window_w_coordinates.dot(quad.barycentrics);
            quad.barycentrics = quad.barycentrics * window_w_coordinates / interpolated_reciprocal_w;

            // FIXME: make this more generic. We want to interpolate more than just color and uv
            if (m_options.shade_smooth)
                quad.vertex_color = interpolate(expand4(vertex0.color), expand4(vertex1.color), expand4(vertex2.color), quad.barycentrics);
            else
                quad.vertex_color = expand4(vertex0.color);

            for (size_t i = 0; i < GPU::NUM_SAMPLERS; ++i)
                quad.texture_coordinates[i] = interpolate(expand4(vertex0.tex_coords[i]), expand4(vertex1.tex_coords[i]), expand4(vertex2.tex_coords[i]), quad.barycentrics);

            if (m_options.fog_enabled)
                quad.fog_depth = fog_depth.dot(quad.barycentrics);
        });
}

Device::Device(Gfx::IntSize const& size)
    : m_frame_buffer(FrameBuffer<GPU::ColorType, GPU::DepthType, GPU::StencilType>::try_create(size).release_value_but_fixme_should_propagate_errors())
{
    m_options.scissor_box = m_frame_buffer->rect();
    m_options.viewport = m_frame_buffer->rect();
}

GPU::DeviceInfo Device::info() const
{
    return {
        .vendor_name = "SerenityOS",
        .device_name = "SoftGPU",
        .num_texture_units = GPU::NUM_SAMPLERS,
        .num_lights = NUM_LIGHTS,
        .max_clip_planes = MAX_CLIP_PLANES,
        .stencil_bits = sizeof(GPU::StencilType) * 8,
        .supports_npot_textures = true,
    };
}

static void generate_texture_coordinates(GPU::Vertex& vertex, GPU::RasterizerOptions const& options)
{
    auto generate_coordinate = [&](size_t texcoord_index, size_t config_index) -> float {
        auto mode = options.texcoord_generation_config[texcoord_index][config_index].mode;

        switch (mode) {
        case GPU::TexCoordGenerationMode::ObjectLinear: {
            auto coefficients = options.texcoord_generation_config[texcoord_index][config_index].coefficients;
            return coefficients.dot(vertex.position);
        }
        case GPU::TexCoordGenerationMode::EyeLinear: {
            auto coefficients = options.texcoord_generation_config[texcoord_index][config_index].coefficients;
            return coefficients.dot(vertex.eye_coordinates);
        }
        case GPU::TexCoordGenerationMode::SphereMap: {
            auto const eye_unit = vertex.eye_coordinates.normalized();
            FloatVector3 const eye_unit_xyz = eye_unit.xyz();
            auto const normal = vertex.normal;
            auto reflection = eye_unit_xyz - normal * 2 * normal.dot(eye_unit_xyz);
            reflection.set_z(reflection.z() + 1);
            auto const reflection_value = reflection[config_index];
            return reflection_value / (2 * reflection.length()) + 0.5f;
        }
        case GPU::TexCoordGenerationMode::ReflectionMap: {
            auto const eye_unit = vertex.eye_coordinates.normalized();
            FloatVector3 const eye_unit_xyz = eye_unit.xyz();
            auto const normal = vertex.normal;
            auto reflection = eye_unit_xyz - normal * 2 * normal.dot(eye_unit_xyz);
            return reflection[config_index];
        }
        case GPU::TexCoordGenerationMode::NormalMap: {
            return vertex.normal[config_index];
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    for (size_t i = 0; i < vertex.tex_coords.size(); ++i) {
        auto& tex_coord = vertex.tex_coords[i];
        auto const enabled_coords = options.texcoord_generation_enabled_coordinates[i];
        tex_coord = {
            ((enabled_coords & GPU::TexCoordGenerationCoordinate::S) > 0) ? generate_coordinate(i, 0) : tex_coord.x(),
            ((enabled_coords & GPU::TexCoordGenerationCoordinate::T) > 0) ? generate_coordinate(i, 1) : tex_coord.y(),
            ((enabled_coords & GPU::TexCoordGenerationCoordinate::R) > 0) ? generate_coordinate(i, 2) : tex_coord.z(),
            ((enabled_coords & GPU::TexCoordGenerationCoordinate::Q) > 0) ? generate_coordinate(i, 3) : tex_coord.w(),
        };
    }
}

void Device::calculate_vertex_lighting(GPU::Vertex& vertex) const
{
    if (!m_options.lighting_enabled)
        return;

    auto const& material = m_materials.at(0);
    auto ambient = material.ambient;
    auto diffuse = material.diffuse;
    auto emissive = material.emissive;
    auto specular = material.specular;

    if (m_options.color_material_enabled
        && (m_options.color_material_face == GPU::ColorMaterialFace::Front || m_options.color_material_face == GPU::ColorMaterialFace::FrontAndBack)) {
        switch (m_options.color_material_mode) {
        case GPU::ColorMaterialMode::Ambient:
            ambient = vertex.color;
            break;
        case GPU::ColorMaterialMode::AmbientAndDiffuse:
            ambient = vertex.color;
            diffuse = vertex.color;
            break;
        case GPU::ColorMaterialMode::Diffuse:
            diffuse = vertex.color;
            break;
        case GPU::ColorMaterialMode::Emissive:
            emissive = vertex.color;
            break;
        case GPU::ColorMaterialMode::Specular:
            specular = vertex.color;
            break;
        }
    }

    FloatVector4 result_color = emissive + ambient * m_lighting_model.scene_ambient_color;

    for (auto const& light : m_lights) {
        if (!light.is_enabled)
            continue;

        // We need to save the length here because the attenuation factor requires a non-normalized vector!
        auto sgi_arrow_operator = [](FloatVector4 const& p1, FloatVector4 const& p2, float& output_length) {
            FloatVector3 light_vector;
            if ((p1.w() != 0.f) && (p2.w() == 0.f))
                light_vector = p2.xyz();
            else if ((p1.w() == 0.f) && (p2.w() != 0.f))
                light_vector = -p1.xyz();
            else
                light_vector = p2.xyz() - p1.xyz();

            output_length = light_vector.length();
            if (output_length == 0.f)
                return light_vector;
            return light_vector / output_length;
        };

        auto sgi_dot_operator = [](FloatVector3 const& d1, FloatVector3 const& d2) {
            return AK::max(d1.dot(d2), 0.0f);
        };

        float vertex_to_light_length = 0.f;
        FloatVector3 vertex_to_light = sgi_arrow_operator(vertex.eye_coordinates, light.position, vertex_to_light_length);

        // Light attenuation value.
        float light_attenuation_factor = 1.0f;
        if (light.position.w() != 0.0f)
            light_attenuation_factor = 1.0f / (light.constant_attenuation + (light.linear_attenuation * vertex_to_light_length) + (light.quadratic_attenuation * vertex_to_light_length * vertex_to_light_length));

        // Spotlight factor
        float spotlight_factor = 1.0f;
        if (light.spotlight_cutoff_angle != 180.0f) {
            auto const vertex_to_light_dot_spotlight_direction = sgi_dot_operator(vertex_to_light, light.spotlight_direction.normalized());
            auto const cos_spotlight_cutoff = AK::cos<float>(light.spotlight_cutoff_angle * AK::Pi<float> / 180.f);

            if (vertex_to_light_dot_spotlight_direction >= cos_spotlight_cutoff)
                spotlight_factor = AK::pow<float>(vertex_to_light_dot_spotlight_direction, light.spotlight_exponent);
            else
                spotlight_factor = 0.0f;
        }

        // FIXME: The spec allows for splitting the colors calculated here into multiple different colors (primary/secondary color). Investigate what this means.
        (void)m_lighting_model.color_control;

        // FIXME: Two sided lighting should be implemented eventually (I believe this is where the normals are -ve and then lighting is calculated with the BACK material)
        (void)m_lighting_model.two_sided_lighting;

        // Ambient
        auto const ambient_component = ambient * light.ambient_intensity;

        // Diffuse
        auto const normal_dot_vertex_to_light = sgi_dot_operator(vertex.normal, vertex_to_light);
        auto const diffuse_component = diffuse * light.diffuse_intensity * normal_dot_vertex_to_light;

        // Specular
        FloatVector4 specular_component = { 0.0f, 0.0f, 0.0f, 0.0f };
        if (normal_dot_vertex_to_light > 0.0f) {
            FloatVector3 half_vector_normalized;
            if (!m_lighting_model.viewer_at_infinity) {
                half_vector_normalized = vertex_to_light + FloatVector3(0.0f, 0.0f, 1.0f);
            } else {
                auto const vertex_to_eye_point = sgi_arrow_operator(vertex.eye_coordinates, { 0.f, 0.f, 0.f, 1.f }, vertex_to_light_length);
                half_vector_normalized = vertex_to_light + vertex_to_eye_point;
            }
            half_vector_normalized.normalize();

            auto const normal_dot_half_vector = sgi_dot_operator(vertex.normal, half_vector_normalized);
            auto const specular_coefficient = AK::pow(normal_dot_half_vector, material.shininess);
            specular_component = specular * light.specular_intensity * specular_coefficient;
        }

        auto color = ambient_component + diffuse_component + specular_component;
        color = color * light_attenuation_factor * spotlight_factor;
        result_color += color;
    }

    vertex.color = result_color;
    vertex.color.set_w(diffuse.w()); // OpenGL 1.5 spec, page 59: "The A produced by lighting is the alpha value associated with diffuse color material"
    vertex.color.clamp(0.0f, 1.0f);
}

void Device::draw_primitives(GPU::PrimitiveType primitive_type, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform,
    FloatMatrix4x4 const& texture_transform, Vector<GPU::Vertex>& vertices, Vector<size_t> const& enabled_texture_units)
{
    // At this point, the user has effectively specified that they are done with defining the geometry
    // of what they want to draw. We now need to do a few things (https://www.khronos.org/opengl/wiki/Rendering_Pipeline_Overview):
    //
    // 1.   Transform all of the vertices in the current vertex list into eye space by multiplying the model-view matrix
    // 2.   Transform all of the vertices from eye space into clip space by multiplying by the projection matrix
    // 3.   If culling is enabled, we cull the desired faces (https://learnopengl.com/Advanced-OpenGL/Face-culling)
    // 4.   Each element of the vertex is then divided by w to bring the positions into NDC (Normalized Device Coordinates)
    // 5.   The triangle's vertices are sorted in a counter-clockwise orientation
    // 6.   The triangles are then sent off to the rasterizer and drawn to the screen

    if (vertices.is_empty())
        return;

    m_enabled_texture_units = enabled_texture_units;

    // Set up normals transform by taking the upper left 3x3 elements from the model view matrix
    // See section 2.11.3 of the OpenGL 1.5 spec
    auto const normal_transform = model_view_transform.submatrix_from_topleft<3>().transpose().inverse();

    // Generate texture coordinates if at least one coordinate is enabled
    bool texture_coordinate_generation_enabled = any_of(
        m_options.texcoord_generation_enabled_coordinates,
        [](auto coordinates_enabled) { return coordinates_enabled != GPU::TexCoordGenerationCoordinate::None; });

    // First, transform all vertices
    for (auto& vertex : vertices) {
        vertex.eye_coordinates = model_view_transform * vertex.position;

        vertex.normal = normal_transform * vertex.normal;
        if (m_options.normalization_enabled)
            vertex.normal.normalize();

        calculate_vertex_lighting(vertex);

        vertex.clip_coordinates = projection_transform * vertex.eye_coordinates;

        if (texture_coordinate_generation_enabled)
            generate_texture_coordinates(vertex, m_options);

        for (size_t i = 0; i < GPU::NUM_SAMPLERS; ++i)
            vertex.tex_coords[i] = texture_transform * vertex.tex_coords[i];
    }

    // Window coordinate calculation
    auto const viewport = m_options.viewport;
    auto const viewport_half_width = viewport.width() / 2.f;
    auto const viewport_half_height = viewport.height() / 2.f;
    auto const viewport_center_x = viewport.x() + viewport_half_width;
    auto const viewport_center_y = viewport.y() + viewport_half_height;
    auto const depth_half_range = (m_options.depth_max - m_options.depth_min) / 2;
    auto const depth_halfway = (m_options.depth_min + m_options.depth_max) / 2;

    auto calculate_vertex_window_coordinates = [&](GPU::Vertex& vertex) {
        auto const one_over_w = 1 / vertex.clip_coordinates.w();
        auto const ndc_coordinates = vertex.clip_coordinates.xyz() * one_over_w;

        vertex.window_coordinates = {
            viewport_center_x + ndc_coordinates.x() * viewport_half_width,
            viewport_center_y + ndc_coordinates.y() * viewport_half_height,
            depth_halfway + ndc_coordinates.z() * depth_half_range,
            one_over_w,
        };
    };

    // Process points
    if (primitive_type == GPU::PrimitiveType::Points) {
        m_clipper.clip_points_against_frustum(vertices);
        for (auto& vertex : vertices) {
            calculate_vertex_window_coordinates(vertex);
            rasterize_point(vertex);
        }
        return;
    }

    // Process lines, line loop and line strips
    auto rasterize_line_segment = [&](GPU::Vertex& from, GPU::Vertex& to) {
        if (!m_clipper.clip_line_against_frustum(from, to))
            return;

        calculate_vertex_window_coordinates(from);
        calculate_vertex_window_coordinates(to);

        rasterize_line(from, to);
    };
    if (primitive_type == GPU::PrimitiveType::Lines) {
        if (vertices.size() < 2)
            return;
        for (size_t i = 0; i < vertices.size() - 1; i += 2)
            rasterize_line_segment(vertices[i], vertices[i + 1]);
        return;
    } else if (primitive_type == GPU::PrimitiveType::LineLoop) {
        if (vertices.size() < 2)
            return;
        for (size_t i = 0; i < vertices.size(); ++i)
            rasterize_line_segment(vertices[i], vertices[(i + 1) % vertices.size()]);
        return;
    } else if (primitive_type == GPU::PrimitiveType::LineStrip) {
        if (vertices.size() < 2)
            return;
        for (size_t i = 0; i < vertices.size() - 1; ++i)
            rasterize_line_segment(vertices[i], vertices[i + 1]);
        return;
    }

    // Let's construct some triangles
    m_triangle_list.clear_with_capacity();
    m_processed_triangles.clear_with_capacity();
    if (primitive_type == GPU::PrimitiveType::Triangles) {
        Triangle triangle;
        if (vertices.size() < 3)
            return;
        for (size_t i = 0; i < vertices.size() - 2; i += 3) {
            triangle.vertices[0] = vertices.at(i);
            triangle.vertices[1] = vertices.at(i + 1);
            triangle.vertices[2] = vertices.at(i + 2);

            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == GPU::PrimitiveType::Quads) {
        // We need to construct two triangles to form the quad
        Triangle triangle;
        if (vertices.size() < 4)
            return;
        for (size_t i = 0; i < vertices.size() - 3; i += 4) {
            // Triangle 1
            triangle.vertices[0] = vertices.at(i);
            triangle.vertices[1] = vertices.at(i + 1);
            triangle.vertices[2] = vertices.at(i + 2);
            m_triangle_list.append(triangle);

            // Triangle 2
            triangle.vertices[0] = vertices.at(i + 2);
            triangle.vertices[1] = vertices.at(i + 3);
            triangle.vertices[2] = vertices.at(i);
            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == GPU::PrimitiveType::TriangleFan) {
        Triangle triangle;
        triangle.vertices[0] = vertices.at(0); // Root vertex is always the vertex defined first

        // This is technically `n-2` triangles. We start at index 1
        for (size_t i = 1; i < vertices.size() - 1; i++) {
            triangle.vertices[1] = vertices.at(i);
            triangle.vertices[2] = vertices.at(i + 1);
            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == GPU::PrimitiveType::TriangleStrip) {
        Triangle triangle;
        if (vertices.size() < 3)
            return;
        for (size_t i = 0; i < vertices.size() - 2; i++) {
            if (i % 2 == 0) {
                triangle.vertices[0] = vertices.at(i);
                triangle.vertices[1] = vertices.at(i + 1);
                triangle.vertices[2] = vertices.at(i + 2);
            } else {
                triangle.vertices[0] = vertices.at(i + 1);
                triangle.vertices[1] = vertices.at(i);
                triangle.vertices[2] = vertices.at(i + 2);
            }
            m_triangle_list.append(triangle);
        }
    }

    // Clip triangles
    for (auto& triangle : m_triangle_list) {
        m_clipped_vertices.clear_with_capacity();
        m_clipped_vertices.append(triangle.vertices[0]);
        m_clipped_vertices.append(triangle.vertices[1]);
        m_clipped_vertices.append(triangle.vertices[2]);
        m_clipper.clip_triangle_against_frustum(m_clipped_vertices);

        if (m_clip_planes.size() > 0)
            m_clipper.clip_triangle_against_user_defined(m_clipped_vertices, m_clip_planes);

        if (m_clipped_vertices.size() < 3)
            continue;

        for (auto& vertex : m_clipped_vertices)
            calculate_vertex_window_coordinates(vertex);

        Triangle tri;
        tri.vertices[0] = m_clipped_vertices[0];
        for (size_t i = 1; i < m_clipped_vertices.size() - 1; i++) {
            tri.vertices[1] = m_clipped_vertices[i];
            tri.vertices[2] = m_clipped_vertices[i + 1];
            m_processed_triangles.append(tri);
        }
    }

    for (auto& triangle : m_processed_triangles)
        rasterize_triangle(triangle);
}

ALWAYS_INLINE void Device::shade_fragments(PixelQuad& quad)
{
    quad.out_color = quad.vertex_color;

    for (size_t i : m_enabled_texture_units) {
        // FIXME: implement GL_TEXTURE_1D, GL_TEXTURE_3D and GL_TEXTURE_CUBE_MAP
        auto const& sampler = m_samplers[i];

        auto texel = sampler.sample_2d(quad.texture_coordinates[i].xy());
        INCREASE_STATISTICS_COUNTER(g_num_sampler_calls, 1);

        // FIXME: Implement more blend modes
        switch (sampler.config().fixed_function_texture_env_mode) {
        case GPU::TextureEnvMode::Modulate:
            quad.out_color = quad.out_color * texel;
            break;
        case GPU::TextureEnvMode::Replace:
            quad.out_color = texel;
            break;
        case GPU::TextureEnvMode::Decal: {
            auto dst_alpha = texel.w();
            quad.out_color.set_x(mix(quad.out_color.x(), texel.x(), dst_alpha));
            quad.out_color.set_y(mix(quad.out_color.y(), texel.y(), dst_alpha));
            quad.out_color.set_z(mix(quad.out_color.z(), texel.z(), dst_alpha));
            break;
        }
        case GPU::TextureEnvMode::Add:
            quad.out_color.set_x(quad.out_color.x() + texel.x());
            quad.out_color.set_y(quad.out_color.y() + texel.y());
            quad.out_color.set_z(quad.out_color.z() + texel.z());
            quad.out_color.set_w(quad.out_color.w() * texel.w()); // FIXME: If texture format is `GL_INTENSITY` alpha components must be added (https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glTexEnv.xml)
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    // Calculate fog
    // Math from here: https://opengl-notes.readthedocs.io/en/latest/topics/texturing/aliasing.html

    // FIXME: exponential fog is not vectorized, we should add a SIMD exp function that calculates an approximation.
    if (m_options.fog_enabled) {
        f32x4 factor;
        switch (m_options.fog_mode) {
        case GPU::FogMode::Linear:
            factor = (m_options.fog_end - quad.fog_depth) / (m_options.fog_end - m_options.fog_start);
            break;
        case GPU::FogMode::Exp: {
            auto argument = -m_options.fog_density * quad.fog_depth;
            factor = exp(argument);
        } break;
        case GPU::FogMode::Exp2: {
            auto argument = m_options.fog_density * quad.fog_depth;
            argument *= -argument;
            factor = exp(argument);
        } break;
        default:
            VERIFY_NOT_REACHED();
        }

        // Mix texel's RGB with fog's RBG - leave alpha alone
        auto fog_color = expand4(m_options.fog_color);
        quad.out_color.set_x(mix(fog_color.x(), quad.out_color.x(), factor));
        quad.out_color.set_y(mix(fog_color.y(), quad.out_color.y(), factor));
        quad.out_color.set_z(mix(fog_color.z(), quad.out_color.z(), factor));
    }

    // Multiply coverage with the fragment's alpha to obtain the final alpha value
    quad.out_color.set_w(quad.out_color.w() * quad.coverage);
}

ALWAYS_INLINE void Device::test_alpha(PixelQuad& quad)
{
    auto const alpha = quad.out_color.w();
    auto const ref_value = expand4(m_options.alpha_test_ref_value);

    switch (m_options.alpha_test_func) {
    case GPU::AlphaTestFunction::Always:
        quad.mask &= expand4(~0);
        break;
    case GPU::AlphaTestFunction::Equal:
        quad.mask &= alpha == ref_value;
        break;
    case GPU::AlphaTestFunction::Greater:
        quad.mask &= alpha > ref_value;
        break;
    case GPU::AlphaTestFunction::GreaterOrEqual:
        quad.mask &= alpha >= ref_value;
        break;
    case GPU::AlphaTestFunction::Less:
        quad.mask &= alpha < ref_value;
        break;
    case GPU::AlphaTestFunction::LessOrEqual:
        quad.mask &= alpha <= ref_value;
        break;
    case GPU::AlphaTestFunction::NotEqual:
        quad.mask &= alpha != ref_value;
        break;
    case GPU::AlphaTestFunction::Never:
    default:
        VERIFY_NOT_REACHED();
    }
}

void Device::resize(Gfx::IntSize const& size)
{
    auto frame_buffer_or_error = FrameBuffer<GPU::ColorType, GPU::DepthType, GPU::StencilType>::try_create(size);
    m_frame_buffer = MUST(frame_buffer_or_error);
}

void Device::clear_color(FloatVector4 const& color)
{
    auto const fill_color = to_argb32(color);

    auto clear_rect = m_frame_buffer->rect();
    if (m_options.scissor_enabled)
        clear_rect.intersect(m_options.scissor_box);

    m_frame_buffer->color_buffer()->fill(fill_color, clear_rect);
}

void Device::clear_depth(GPU::DepthType depth)
{
    auto clear_rect = m_frame_buffer->rect();
    if (m_options.scissor_enabled)
        clear_rect.intersect(m_options.scissor_box);

    m_frame_buffer->depth_buffer()->fill(depth, clear_rect);
}

void Device::clear_stencil(GPU::StencilType value)
{
    auto clear_rect = m_frame_buffer->rect();
    if (m_options.scissor_enabled)
        clear_rect.intersect(m_options.scissor_box);

    m_frame_buffer->stencil_buffer()->fill(value, clear_rect);
}

GPU::ImageDataLayout Device::color_buffer_data_layout(Vector2<u32> size, Vector2<i32> offset)
{
    return {
        .pixel_type = {
            .format = GPU::PixelFormat::BGRA,
            .bits = GPU::PixelComponentBits::B8_8_8_8,
            .data_type = GPU::PixelDataType::UnsignedInt,
            .components_order = GPU::ComponentsOrder::Reversed,
        },
        .dimensions = {
            .width = static_cast<u32>(m_frame_buffer->rect().width()),
            .height = static_cast<u32>(m_frame_buffer->rect().height()),
            .depth = 1,
        },
        .selection = {
            .offset_x = offset.x(),
            .offset_y = offset.y(),
            .offset_z = 0,
            .width = size.x(),
            .height = size.y(),
            .depth = 1,
        },
    };
}

GPU::ImageDataLayout Device::depth_buffer_data_layout(Vector2<u32> size, Vector2<i32> offset)
{
    return {
        .pixel_type = {
            .format = GPU::PixelFormat::DepthComponent,
            .bits = GPU::PixelComponentBits::AllBits,
            .data_type = GPU::PixelDataType::Float,
        },
        .dimensions = {
            .width = static_cast<u32>(m_frame_buffer->rect().width()),
            .height = static_cast<u32>(m_frame_buffer->rect().height()),
            .depth = 1,
        },
        .selection = {
            .offset_x = offset.x(),
            .offset_y = offset.y(),
            .offset_z = 0,
            .width = size.x(),
            .height = size.y(),
            .depth = 1,
        },
    };
}

void Device::blit_from_color_buffer(void* output_data, Vector2<i32> input_offset, GPU::ImageDataLayout const& output_layout)
{
    auto const& output_selection = output_layout.selection;
    auto input_layout = color_buffer_data_layout({ output_selection.width, output_selection.height }, input_offset);

    PixelConverter converter { input_layout, output_layout };
    auto const* input_data = m_frame_buffer->color_buffer()->scanline(0);
    auto conversion_result = converter.convert(input_data, output_data, {});
    if (conversion_result.is_error())
        dbgln("Pixel conversion failed: {}", conversion_result.error().string_literal());
}

void Device::blit_from_depth_buffer(void* output_data, Vector2<i32> input_offset, GPU::ImageDataLayout const& output_layout)
{
    auto const& output_selection = output_layout.selection;
    auto input_layout = depth_buffer_data_layout({ output_selection.width, output_selection.height }, input_offset);

    PixelConverter converter { input_layout, output_layout };
    auto const* input_data = m_frame_buffer->depth_buffer()->scanline(0);
    auto conversion_result = converter.convert(input_data, output_data, {});
    if (conversion_result.is_error())
        dbgln("Pixel conversion failed: {}", conversion_result.error().string_literal());
}

void Device::blit_to_color_buffer_at_raster_position(void const* input_data, GPU::ImageDataLayout const& input_layout)
{
    if (!m_raster_position.valid)
        return;

    auto input_selection = input_layout.selection;
    INCREASE_STATISTICS_COUNTER(g_num_pixels, input_selection.width * input_selection.height);
    INCREASE_STATISTICS_COUNTER(g_num_pixels_shaded, input_selection.width * input_selection.height);

    auto const rasterization_rect = get_rasterization_rect_of_size({ input_selection.width, input_selection.height });
    auto output_layout = color_buffer_data_layout(
        { static_cast<u32>(rasterization_rect.width()), static_cast<u32>(rasterization_rect.height()) },
        { rasterization_rect.x(), rasterization_rect.y() });

    PixelConverter converter { input_layout, output_layout };
    auto* output_data = m_frame_buffer->color_buffer()->scanline(0);
    auto conversion_result = converter.convert(input_data, output_data, {});
    if (conversion_result.is_error())
        dbgln("Pixel conversion failed: {}", conversion_result.error().string_literal());
}

void Device::blit_to_depth_buffer_at_raster_position(void const* input_data, GPU::ImageDataLayout const& input_layout)
{
    if (!m_raster_position.valid)
        return;

    auto input_selection = input_layout.selection;
    auto const rasterization_rect = get_rasterization_rect_of_size({ input_selection.width, input_selection.height });
    auto output_layout = depth_buffer_data_layout(
        { static_cast<u32>(rasterization_rect.width()), static_cast<u32>(rasterization_rect.height()) },
        { rasterization_rect.x(), rasterization_rect.y() });

    PixelConverter converter { input_layout, output_layout };
    auto* output_data = m_frame_buffer->depth_buffer()->scanline(0);
    auto conversion_result = converter.convert(input_data, output_data, {});
    if (conversion_result.is_error())
        dbgln("Pixel conversion failed: {}", conversion_result.error().string_literal());
}

void Device::blit_color_buffer_to(Gfx::Bitmap& target)
{
    m_frame_buffer->color_buffer()->blit_flipped_to_bitmap(target, m_frame_buffer->rect());

    if constexpr (ENABLE_STATISTICS_OVERLAY)
        draw_statistics_overlay(target);
}

void Device::draw_statistics_overlay(Gfx::Bitmap& target)
{
    static Core::ElapsedTimer timer;
    static String debug_string;
    static int frame_counter;

    frame_counter++;
    int milliseconds = 0;
    if (timer.is_valid())
        milliseconds = timer.elapsed();
    else
        timer.start();

    Gfx::Painter painter { target };

    if (milliseconds > MILLISECONDS_PER_STATISTICS_PERIOD) {

        int num_rendertarget_pixels = m_frame_buffer->rect().size().area();

        StringBuilder builder;
        builder.append(String::formatted("Timings      : {:.1}ms {:.1}FPS\n",
            static_cast<double>(milliseconds) / frame_counter,
            (milliseconds > 0) ? 1000.0 * frame_counter / milliseconds : 9999.0));
        builder.append(String::formatted("Triangles    : {}\n", g_num_rasterized_triangles));
        builder.append(String::formatted("SIMD usage   : {}%\n", g_num_quads > 0 ? g_num_pixels_shaded * 25 / g_num_quads : 0));
        builder.append(String::formatted("Pixels       : {}, Stencil: {}%, Shaded: {}%, Blended: {}%, Overdraw: {}%\n",
            g_num_pixels,
            g_num_pixels > 0 ? g_num_stencil_writes * 100 / g_num_pixels : 0,
            g_num_pixels > 0 ? g_num_pixels_shaded * 100 / g_num_pixels : 0,
            g_num_pixels_shaded > 0 ? g_num_pixels_blended * 100 / g_num_pixels_shaded : 0,
            num_rendertarget_pixels > 0 ? g_num_pixels_shaded * 100 / num_rendertarget_pixels - 100 : 0));
        builder.append(String::formatted("Sampler calls: {}\n", g_num_sampler_calls));

        debug_string = builder.to_string();

        frame_counter = 0;
        timer.start();
    }

    g_num_rasterized_triangles = 0;
    g_num_pixels = 0;
    g_num_pixels_shaded = 0;
    g_num_pixels_blended = 0;
    g_num_sampler_calls = 0;
    g_num_stencil_writes = 0;
    g_num_quads = 0;

    auto& font = Gfx::FontDatabase::default_fixed_width_font();

    for (int y = -1; y < 2; y++)
        for (int x = -1; x < 2; x++)
            if (x != 0 && y != 0)
                painter.draw_text(target.rect().translated(x + 2, y + 2), debug_string, font, Gfx::TextAlignment::TopLeft, Gfx::Color::Black);

    painter.draw_text(target.rect().translated(2, 2), debug_string, font, Gfx::TextAlignment::TopLeft, Gfx::Color::White);
}

void Device::set_options(GPU::RasterizerOptions const& options)
{
    m_options = options;

    if (m_options.enable_blending)
        setup_blend_factors();
}

void Device::set_light_model_params(GPU::LightModelParameters const& lighting_model)
{
    m_lighting_model = lighting_model;
}

NonnullRefPtr<GPU::Image> Device::create_image(GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 levels, u32 layers)
{
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(levels > 0);
    VERIFY(layers > 0);

    return adopt_ref(*new Image(this, pixel_format, width, height, depth, levels, layers));
}

void Device::set_sampler_config(unsigned sampler, GPU::SamplerConfig const& config)
{
    VERIFY(config.bound_image.is_null() || config.bound_image->ownership_token() == this);

    m_samplers[sampler].set_config(config);
}

void Device::set_light_state(unsigned int light_id, GPU::Light const& light)
{
    m_lights.at(light_id) = light;
}

void Device::set_material_state(GPU::Face face, GPU::Material const& material)
{
    m_materials[face] = material;
}

void Device::set_stencil_configuration(GPU::Face face, GPU::StencilConfiguration const& stencil_configuration)
{
    m_stencil_configuration[face] = stencil_configuration;
}

void Device::set_raster_position(GPU::RasterPosition const& raster_position)
{
    m_raster_position = raster_position;
}

void Device::set_clip_planes(Vector<FloatVector4> const& clip_planes)
{
    m_clip_planes = clip_planes;
}

void Device::set_raster_position(FloatVector4 const& position, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform)
{
    auto const eye_coordinates = model_view_transform * position;
    auto const clip_coordinates = projection_transform * eye_coordinates;

    // FIXME: implement clipping
    m_raster_position.valid = true;

    auto ndc_coordinates = clip_coordinates / clip_coordinates.w();
    ndc_coordinates.set_w(clip_coordinates.w());

    auto const viewport = m_options.viewport;
    auto const viewport_half_width = viewport.width() / 2.0f;
    auto const viewport_half_height = viewport.height() / 2.0f;
    auto const viewport_center_x = viewport.x() + viewport_half_width;
    auto const viewport_center_y = viewport.y() + viewport_half_height;
    auto const depth_half_range = (m_options.depth_max - m_options.depth_min) / 2;
    auto const depth_halfway = (m_options.depth_min + m_options.depth_max) / 2;

    // FIXME: implement other raster position properties such as color and texcoords

    m_raster_position.window_coordinates = {
        viewport_center_x + ndc_coordinates.x() * viewport_half_width,
        viewport_center_y + ndc_coordinates.y() * viewport_half_height,
        depth_halfway + ndc_coordinates.z() * depth_half_range,
        ndc_coordinates.w(),
    };

    m_raster_position.eye_coordinate_distance = eye_coordinates.length();
}

Gfx::IntRect Device::get_rasterization_rect_of_size(Gfx::IntSize size) const
{
    // Round the X and Y floating point coordinates to the nearest integer; OpenGL 1.5 spec:
    // "Any fragments whose centers lie inside of this rectangle (or on its bottom or left
    // boundaries) are produced in correspondence with this particular group of elements."
    return {
        round_to<int>(m_raster_position.window_coordinates.x()),
        round_to<int>(m_raster_position.window_coordinates.y()),
        size.width(),
        size.height(),
    };
}

}

extern "C" {

GPU::Device* serenity_gpu_create_device(Gfx::IntSize const& size)
{
    return make<SoftGPU::Device>(size).leak_ptr();
}
}
