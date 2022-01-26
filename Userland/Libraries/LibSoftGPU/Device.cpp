/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
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
#include <LibSoftGPU/PixelQuad.h>
#include <LibSoftGPU/SIMD.h>

namespace SoftGPU {

static long long g_num_rasterized_triangles;
static long long g_num_pixels;
static long long g_num_pixels_shaded;
static long long g_num_pixels_blended;
static long long g_num_sampler_calls;
static long long g_num_stencil_writes;
static long long g_num_quads;

using IntVector2 = Gfx::Vector2<int>;
using IntVector3 = Gfx::Vector3<int>;

using AK::SIMD::any;
using AK::SIMD::exp;
using AK::SIMD::expand4;
using AK::SIMD::f32x4;
using AK::SIMD::i32x4;
using AK::SIMD::load4_masked;
using AK::SIMD::maskbits;
using AK::SIMD::maskcount;
using AK::SIMD::none;
using AK::SIMD::store4_masked;
using AK::SIMD::to_f32x4;
using AK::SIMD::to_u32x4;
using AK::SIMD::u32x4;

constexpr static int edge_function(const IntVector2& a, const IntVector2& b, const IntVector2& c)
{
    return ((c.x() - a.x()) * (b.y() - a.y()) - (c.y() - a.y()) * (b.x() - a.x()));
}

constexpr static i32x4 edge_function4(const IntVector2& a, const IntVector2& b, const Vector2<i32x4>& c)
{
    return ((c.x() - a.x()) * (b.y() - a.y()) - (c.y() - a.y()) * (b.x() - a.x()));
}

template<typename T, typename U>
constexpr static auto interpolate(const T& v0, const T& v1, const T& v2, const Vector3<U>& barycentric_coords)
{
    return v0 * barycentric_coords.x() + v1 * barycentric_coords.y() + v2 * barycentric_coords.z();
}

ALWAYS_INLINE static u32x4 to_rgba32(const Vector4<f32x4>& v)
{
    auto clamped = v.clamped(expand4(0.0f), expand4(1.0f));
    auto r = to_u32x4(clamped.x() * 255);
    auto g = to_u32x4(clamped.y() * 255);
    auto b = to_u32x4(clamped.z() * 255);
    auto a = to_u32x4(clamped.w() * 255);

    return a << 24 | r << 16 | g << 8 | b;
}

static Vector4<f32x4> to_vec4(u32x4 rgba)
{
    auto constexpr one_over_255 = expand4(1.0f / 255);
    return {
        to_f32x4((rgba >> 16) & 0xff) * one_over_255,
        to_f32x4((rgba >> 8) & 0xff) * one_over_255,
        to_f32x4(rgba & 0xff) * one_over_255,
        to_f32x4((rgba >> 24) & 0xff) * one_over_255,
    };
}

Gfx::IntRect Device::window_coordinates_to_target_coordinates(Gfx::IntRect const& window_rect)
{
    return {
        window_rect.x(),
        m_render_target->rect().height() - window_rect.height() - window_rect.y(),
        window_rect.width(),
        window_rect.height(),
    };
}

void Device::setup_blend_factors()
{
    m_alpha_blend_factors = {};

    switch (m_options.blend_source_factor) {
    case BlendFactor::Zero:
        break;
    case BlendFactor::One:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        break;
    case BlendFactor::SrcColor:
        m_alpha_blend_factors.src_factor_src_color = 1;
        break;
    case BlendFactor::OneMinusSrcColor:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_src_color = -1;
        break;
    case BlendFactor::SrcAlpha:
        m_alpha_blend_factors.src_factor_src_alpha = 1;
        break;
    case BlendFactor::OneMinusSrcAlpha:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_src_alpha = -1;
        break;
    case BlendFactor::DstAlpha:
        m_alpha_blend_factors.src_factor_dst_alpha = 1;
        break;
    case BlendFactor::OneMinusDstAlpha:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_dst_alpha = -1;
        break;
    case BlendFactor::DstColor:
        m_alpha_blend_factors.src_factor_dst_color = 1;
        break;
    case BlendFactor::OneMinusDstColor:
        m_alpha_blend_factors.src_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.src_factor_dst_color = -1;
        break;
    case BlendFactor::SrcAlphaSaturate:
    default:
        VERIFY_NOT_REACHED();
    }

    switch (m_options.blend_destination_factor) {
    case BlendFactor::Zero:
        break;
    case BlendFactor::One:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        break;
    case BlendFactor::SrcColor:
        m_alpha_blend_factors.dst_factor_src_color = 1;
        break;
    case BlendFactor::OneMinusSrcColor:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_src_color = -1;
        break;
    case BlendFactor::SrcAlpha:
        m_alpha_blend_factors.dst_factor_src_alpha = 1;
        break;
    case BlendFactor::OneMinusSrcAlpha:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_src_alpha = -1;
        break;
    case BlendFactor::DstAlpha:
        m_alpha_blend_factors.dst_factor_dst_alpha = 1;
        break;
    case BlendFactor::OneMinusDstAlpha:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_dst_alpha = -1;
        break;
    case BlendFactor::DstColor:
        m_alpha_blend_factors.dst_factor_dst_color = 1;
        break;
    case BlendFactor::OneMinusDstColor:
        m_alpha_blend_factors.dst_constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        m_alpha_blend_factors.dst_factor_dst_color = -1;
        break;
    case BlendFactor::SrcAlphaSaturate:
    default:
        VERIFY_NOT_REACHED();
    }
}

void Device::rasterize_triangle(const Triangle& triangle)
{
    INCREASE_STATISTICS_COUNTER(g_num_rasterized_triangles, 1);

    // Return if alpha testing is a no-op
    if (m_options.enable_alpha_test && m_options.alpha_test_func == AlphaTestFunction::Never)
        return;

    // Vertices
    Vertex const vertex0 = triangle.vertices[0];
    Vertex const vertex1 = triangle.vertices[1];
    Vertex const vertex2 = triangle.vertices[2];

    constexpr int subpixel_factor = 1 << SUBPIXEL_BITS;

    // Calculate area of the triangle for later tests
    IntVector2 const v0 { static_cast<int>(vertex0.window_coordinates.x() * subpixel_factor), static_cast<int>(vertex0.window_coordinates.y() * subpixel_factor) };
    IntVector2 const v1 { static_cast<int>(vertex1.window_coordinates.x() * subpixel_factor), static_cast<int>(vertex1.window_coordinates.y() * subpixel_factor) };
    IntVector2 const v2 { static_cast<int>(vertex2.window_coordinates.x() * subpixel_factor), static_cast<int>(vertex2.window_coordinates.y() * subpixel_factor) };

    int area = edge_function(v0, v1, v2);
    if (area == 0)
        return;

    auto const one_over_area = 1.0f / area;

    auto render_bounds = m_render_target->rect();
    if (m_options.scissor_enabled)
        render_bounds.intersect(window_coordinates_to_target_coordinates(m_options.scissor_box));

    // Obey top-left rule:
    // This sets up "zero" for later pixel coverage tests.
    // Depending on where on the triangle the edge is located
    // it is either tested against 0 or 1, effectively
    // turning "< 0" into "<= 0"
    IntVector3 zero { 1, 1, 1 };
    if (v1.y() > v0.y() || (v1.y() == v0.y() && v1.x() < v0.x()))
        zero.set_z(0);
    if (v2.y() > v1.y() || (v2.y() == v1.y() && v2.x() < v1.x()))
        zero.set_x(0);
    if (v0.y() > v2.y() || (v0.y() == v2.y() && v0.x() < v2.x()))
        zero.set_y(0);

    // This function calculates the 3 edge values for the pixel relative to the triangle.
    auto calculate_edge_values4 = [v0, v1, v2](Vector2<i32x4> const& p) -> Vector3<i32x4> {
        return {
            edge_function4(v1, v2, p),
            edge_function4(v2, v0, p),
            edge_function4(v0, v1, p),
        };
    };

    // This function tests whether a point as identified by its 3 edge values lies within the triangle
    auto test_point4 = [zero](Vector3<i32x4> const& edges) -> i32x4 {
        return edges.x() >= zero.x()
            && edges.y() >= zero.y()
            && edges.z() >= zero.z();
    };

    // Calculate block-based bounds
    // clang-format off
    int const bx0 =  max(render_bounds.left(),   min(min(v0.x(), v1.x()), v2.x()) / subpixel_factor) & ~1;
    int const bx1 = (min(render_bounds.right(),  max(max(v0.x(), v1.x()), v2.x()) / subpixel_factor) & ~1) + 2;
    int const by0 =  max(render_bounds.top(),    min(min(v0.y(), v1.y()), v2.y()) / subpixel_factor) & ~1;
    int const by1 = (min(render_bounds.bottom(), max(max(v0.y(), v1.y()), v2.y()) / subpixel_factor) & ~1) + 2;
    // clang-format on

    // Fog depths
    float const vertex0_eye_absz = fabs(vertex0.eye_coordinates.z());
    float const vertex1_eye_absz = fabs(vertex1.eye_coordinates.z());
    float const vertex2_eye_absz = fabs(vertex2.eye_coordinates.z());

    int const render_bounds_left = render_bounds.x();
    int const render_bounds_right = render_bounds.x() + render_bounds.width();
    int const render_bounds_top = render_bounds.y();
    int const render_bounds_bottom = render_bounds.y() + render_bounds.height();

    auto const half_pixel_offset = Vector2<i32x4> {
        expand4(subpixel_factor / 2),
        expand4(subpixel_factor / 2),
    };

    // Stencil configuration and writing
    auto const stencil_configuration = m_stencil_configuration[Face::Front];
    auto const stencil_reference_value = stencil_configuration.reference_value & stencil_configuration.test_mask;

    auto write_to_stencil = [](u8* stencil_ptrs[4], i32x4 stencil_value, StencilOperation op, u8 reference_value, u8 write_mask, i32x4 pixel_mask) {
        if (write_mask == 0 || op == StencilOperation::Keep)
            return;

        switch (op) {
        case StencilOperation::Decrement:
            stencil_value = (stencil_value & ~write_mask) | (max(stencil_value - 1, expand4(0)) & write_mask);
            break;
        case StencilOperation::DecrementWrap:
            stencil_value = (stencil_value & ~write_mask) | (((stencil_value - 1) & 0xFF) & write_mask);
            break;
        case StencilOperation::Increment:
            stencil_value = (stencil_value & ~write_mask) | (min(stencil_value + 1, expand4(0xFF)) & write_mask);
            break;
        case StencilOperation::IncrementWrap:
            stencil_value = (stencil_value & ~write_mask) | (((stencil_value + 1) & 0xFF) & write_mask);
            break;
        case StencilOperation::Invert:
            stencil_value ^= write_mask;
            break;
        case StencilOperation::Replace:
            stencil_value = (stencil_value & ~write_mask) | (reference_value & write_mask);
            break;
        case StencilOperation::Zero:
            stencil_value &= ~write_mask;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        INCREASE_STATISTICS_COUNTER(g_num_stencil_writes, maskcount(pixel_mask));
        store4_masked(stencil_value, stencil_ptrs[0], stencil_ptrs[1], stencil_ptrs[2], stencil_ptrs[3], pixel_mask);
    };

    // Iterate over all blocks within the bounds of the triangle
    for (int by = by0; by < by1; by += 2) {
        for (int bx = bx0; bx < bx1; bx += 2) {
            PixelQuad quad;

            quad.screen_coordinates = {
                i32x4 { bx, bx + 1, bx, bx + 1 },
                i32x4 { by, by, by + 1, by + 1 },
            };

            auto edge_values = calculate_edge_values4(quad.screen_coordinates * subpixel_factor + half_pixel_offset);

            // Generate triangle coverage mask
            quad.mask = test_point4(edge_values);

            // Test quad against intersection of render target size and scissor rect
            quad.mask &= quad.screen_coordinates.x() >= render_bounds_left
                && quad.screen_coordinates.x() < render_bounds_right
                && quad.screen_coordinates.y() >= render_bounds_top
                && quad.screen_coordinates.y() < render_bounds_bottom;

            if (none(quad.mask))
                continue;

            INCREASE_STATISTICS_COUNTER(g_num_quads, 1);
            INCREASE_STATISTICS_COUNTER(g_num_pixels, maskcount(quad.mask));

            // Calculate barycentric coordinates from previously calculated edge values
            quad.barycentrics = Vector3<f32x4> {
                to_f32x4(edge_values.x()),
                to_f32x4(edge_values.y()),
                to_f32x4(edge_values.z()),
            } * one_over_area;

            int coverage_bits = maskbits(quad.mask);

            // Stencil testing
            u8* stencil_ptrs[4];
            i32x4 stencil_value;
            if (m_options.enable_stencil_test) {
                stencil_ptrs[0] = coverage_bits & 1 ? &m_stencil_buffer->scanline(by)[bx] : nullptr;
                stencil_ptrs[1] = coverage_bits & 2 ? &m_stencil_buffer->scanline(by)[bx + 1] : nullptr;
                stencil_ptrs[2] = coverage_bits & 4 ? &m_stencil_buffer->scanline(by + 1)[bx] : nullptr;
                stencil_ptrs[3] = coverage_bits & 8 ? &m_stencil_buffer->scanline(by + 1)[bx + 1] : nullptr;

                stencil_value = load4_masked(stencil_ptrs[0], stencil_ptrs[1], stencil_ptrs[2], stencil_ptrs[3], quad.mask);
                stencil_value &= stencil_configuration.test_mask;

                i32x4 stencil_test_passed;
                switch (stencil_configuration.test_function) {
                case StencilTestFunction::Always:
                    stencil_test_passed = expand4(~0);
                    break;
                case StencilTestFunction::Equal:
                    stencil_test_passed = stencil_value == stencil_reference_value;
                    break;
                case StencilTestFunction::Greater:
                    stencil_test_passed = stencil_value > stencil_reference_value;
                    break;
                case StencilTestFunction::GreaterOrEqual:
                    stencil_test_passed = stencil_value >= stencil_reference_value;
                    break;
                case StencilTestFunction::Less:
                    stencil_test_passed = stencil_value < stencil_reference_value;
                    break;
                case StencilTestFunction::LessOrEqual:
                    stencil_test_passed = stencil_value <= stencil_reference_value;
                    break;
                case StencilTestFunction::Never:
                    stencil_test_passed = expand4(0);
                    break;
                case StencilTestFunction::NotEqual:
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
                if (none(quad.mask))
                    continue;
            }

            // Depth testing
            float* depth_ptrs[4] = {
                coverage_bits & 1 ? &m_depth_buffer->scanline(by)[bx] : nullptr,
                coverage_bits & 2 ? &m_depth_buffer->scanline(by)[bx + 1] : nullptr,
                coverage_bits & 4 ? &m_depth_buffer->scanline(by + 1)[bx] : nullptr,
                coverage_bits & 8 ? &m_depth_buffer->scanline(by + 1)[bx + 1] : nullptr,
            };
            if (m_options.enable_depth_test) {
                auto depth = load4_masked(depth_ptrs[0], depth_ptrs[1], depth_ptrs[2], depth_ptrs[3], quad.mask);

                quad.depth = interpolate(vertex0.window_coordinates.z(), vertex1.window_coordinates.z(), vertex2.window_coordinates.z(), quad.barycentrics);
                // FIXME: Also apply depth_offset_factor which depends on the depth gradient
                if (m_options.depth_offset_enabled)
                    quad.depth += m_options.depth_offset_constant * NumericLimits<float>::epsilon();

                i32x4 depth_test_passed;
                switch (m_options.depth_func) {
                case DepthTestFunction::Always:
                    depth_test_passed = expand4(~0);
                    break;
                case DepthTestFunction::Never:
                    depth_test_passed = expand4(0);
                    break;
                case DepthTestFunction::Greater:
                    depth_test_passed = quad.depth > depth;
                    break;
                case DepthTestFunction::GreaterOrEqual:
                    depth_test_passed = quad.depth >= depth;
                    break;
                case DepthTestFunction::NotEqual:
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
                case DepthTestFunction::Equal:
#ifdef __SSE__
                    depth_test_passed = quad.depth == depth;
#else
                    //
                    // This is an interesting quirk that occurs due to us using the x87 FPU when Serenity is
                    // compiled for the i386 target. When we calculate our depth value to be stored in the buffer,
                    // it is an 80-bit x87 floating point number, however, when stored into the DepthBuffer, this is
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
                case DepthTestFunction::LessOrEqual:
                    depth_test_passed = quad.depth <= depth;
                    break;
                case DepthTestFunction::Less:
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
                if (none(quad.mask))
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

            // Draw the pixels according to the previously generated mask
            auto const w_coordinates = Vector3<f32x4> {
                expand4(vertex0.window_coordinates.w()),
                expand4(vertex1.window_coordinates.w()),
                expand4(vertex2.window_coordinates.w()),
            };

            auto const interpolated_reciprocal_w = interpolate(w_coordinates.x(), w_coordinates.y(), w_coordinates.z(), quad.barycentrics);
            auto const interpolated_w = 1.0f / interpolated_reciprocal_w;
            quad.barycentrics = quad.barycentrics * w_coordinates * interpolated_w;

            // FIXME: make this more generic. We want to interpolate more than just color and uv
            if (m_options.shade_smooth) {
                quad.vertex_color = interpolate(expand4(vertex0.color), expand4(vertex1.color), expand4(vertex2.color), quad.barycentrics);
            } else {
                quad.vertex_color = expand4(vertex0.color);
            }

            for (size_t i = 0; i < NUM_SAMPLERS; ++i)
                quad.texture_coordinates[i] = interpolate(expand4(vertex0.tex_coords[i]), expand4(vertex1.tex_coords[i]), expand4(vertex2.tex_coords[i]), quad.barycentrics);

            if (m_options.fog_enabled) {
                // Calculate depth of fragment for fog
                //
                // OpenGL 1.5 spec chapter 3.10: "An implementation may choose to approximate the
                // eye-coordinate distance from the eye to each fragment center by |Ze|."

                quad.fog_depth = interpolate(expand4(vertex0_eye_absz), expand4(vertex1_eye_absz), expand4(vertex2_eye_absz), quad.barycentrics);
            }

            shade_fragments(quad);

            if (m_options.enable_alpha_test && m_options.alpha_test_func != AlphaTestFunction::Always && !test_alpha(quad)) {
                continue;
            }

            // Write to depth buffer
            if (m_options.enable_depth_test && m_options.enable_depth_write)
                store4_masked(quad.depth, depth_ptrs[0], depth_ptrs[1], depth_ptrs[2], depth_ptrs[3], quad.mask);

            // We will not update the color buffer at all
            if (!m_options.color_mask || !m_options.enable_color_write)
                continue;

            Gfx::RGBA32* color_ptrs[4] = {
                coverage_bits & 1 ? &m_render_target->scanline(by)[bx] : nullptr,
                coverage_bits & 2 ? &m_render_target->scanline(by)[bx + 1] : nullptr,
                coverage_bits & 4 ? &m_render_target->scanline(by + 1)[bx] : nullptr,
                coverage_bits & 8 ? &m_render_target->scanline(by + 1)[bx + 1] : nullptr,
            };

            u32x4 dst_u32;
            if (m_options.enable_blending || m_options.color_mask != 0xffffffff)
                dst_u32 = load4_masked(color_ptrs[0], color_ptrs[1], color_ptrs[2], color_ptrs[3], quad.mask);

            if (m_options.enable_blending) {
                INCREASE_STATISTICS_COUNTER(g_num_pixels_blended, maskcount(quad.mask));

                // Blend color values from pixel_staging into m_render_target
                Vector4<f32x4> const& src = quad.out_color;
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
                store4_masked(to_rgba32(quad.out_color), color_ptrs[0], color_ptrs[1], color_ptrs[2], color_ptrs[3], quad.mask);
            else
                store4_masked((to_rgba32(quad.out_color) & m_options.color_mask) | (dst_u32 & ~m_options.color_mask), color_ptrs[0], color_ptrs[1], color_ptrs[2], color_ptrs[3], quad.mask);
        }
    }
}

Device::Device(const Gfx::IntSize& size)
    : m_render_target(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size).release_value_but_fixme_should_propagate_errors())
    , m_depth_buffer(make<DepthBuffer>(size))
    , m_stencil_buffer(MUST(StencilBuffer::try_create(size)))
{
    m_options.scissor_box = m_render_target->rect();
    m_options.viewport = m_render_target->rect();
}

DeviceInfo Device::info() const
{
    return {
        .vendor_name = "SerenityOS",
        .device_name = "SoftGPU",
        .num_texture_units = NUM_SAMPLERS,
        .num_lights = NUM_LIGHTS,
        .stencil_bits = sizeof(u8) * 8,
        .supports_npot_textures = true,
    };
}

static void generate_texture_coordinates(Vertex& vertex, RasterizerOptions const& options)
{
    auto generate_coordinate = [&](size_t texcoord_index, size_t config_index) -> float {
        auto mode = options.texcoord_generation_config[texcoord_index][config_index].mode;

        switch (mode) {
        case TexCoordGenerationMode::ObjectLinear: {
            auto coefficients = options.texcoord_generation_config[texcoord_index][config_index].coefficients;
            return coefficients.dot(vertex.position);
        }
        case TexCoordGenerationMode::EyeLinear: {
            auto coefficients = options.texcoord_generation_config[texcoord_index][config_index].coefficients;
            return coefficients.dot(vertex.eye_coordinates);
        }
        case TexCoordGenerationMode::SphereMap: {
            auto const eye_unit = vertex.eye_coordinates.normalized();
            FloatVector3 const eye_unit_xyz = { eye_unit.x(), eye_unit.y(), eye_unit.z() };
            auto const normal = vertex.normal;
            auto reflection = eye_unit_xyz - normal * 2 * normal.dot(eye_unit_xyz);
            reflection.set_z(reflection.z() + 1);
            auto const reflection_value = (config_index == 0) ? reflection.x() : reflection.y();
            return reflection_value / (2 * reflection.length()) + 0.5f;
        }
        case TexCoordGenerationMode::ReflectionMap: {
            auto const eye_unit = vertex.eye_coordinates.normalized();
            FloatVector3 const eye_unit_xyz = { eye_unit.x(), eye_unit.y(), eye_unit.z() };
            auto const normal = vertex.normal;
            auto reflection = eye_unit_xyz - normal * 2 * normal.dot(eye_unit_xyz);
            switch (config_index) {
            case 0:
                return reflection.x();
            case 1:
                return reflection.y();
            case 2:
                return reflection.z();
            default:
                VERIFY_NOT_REACHED();
            }
        }
        case TexCoordGenerationMode::NormalMap: {
            auto const normal = vertex.normal;
            switch (config_index) {
            case 0:
                return normal.x();
            case 1:
                return normal.y();
            case 2:
                return normal.z();
            default:
                VERIFY_NOT_REACHED();
            }
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    for (size_t i = 0; i < vertex.tex_coords.size(); ++i) {
        auto& tex_coord = vertex.tex_coords[i];
        auto const enabled_coords = options.texcoord_generation_enabled_coordinates[i];
        tex_coord = {
            ((enabled_coords & TexCoordGenerationCoordinate::S) > 0) ? generate_coordinate(i, 0) : tex_coord.x(),
            ((enabled_coords & TexCoordGenerationCoordinate::T) > 0) ? generate_coordinate(i, 1) : tex_coord.y(),
            ((enabled_coords & TexCoordGenerationCoordinate::R) > 0) ? generate_coordinate(i, 2) : tex_coord.z(),
            ((enabled_coords & TexCoordGenerationCoordinate::Q) > 0) ? generate_coordinate(i, 3) : tex_coord.w(),
        };
    }
}

void Device::draw_primitives(PrimitiveType primitive_type, FloatMatrix4x4 const& model_view_transform, FloatMatrix3x3 const& normal_transform,
    FloatMatrix4x4 const& projection_transform, FloatMatrix4x4 const& texture_transform, Vector<Vertex> const& vertices,
    Vector<size_t> const& enabled_texture_units)
{
    // At this point, the user has effectively specified that they are done with defining the geometry
    // of what they want to draw. We now need to do a few things (https://www.khronos.org/opengl/wiki/Rendering_Pipeline_Overview):
    //
    // 1.   Transform all of the vertices in the current vertex list into eye space by multiplying the model-view matrix
    // 2.   Transform all of the vertices from eye space into clip space by multiplying by the projection matrix
    // 3.   If culling is enabled, we cull the desired faces (https://learnopengl.com/Advanced-OpenGL/Face-culling)
    // 4.   Each element of the vertex is then divided by w to bring the positions into NDC (Normalized Device Coordinates)
    // 5.   The vertices are sorted (for the rasterizer, how are we doing this? 3Dfx did this top to bottom in terms of vertex y coordinates)
    // 6.   The vertices are then sent off to the rasterizer and drawn to the screen

    m_enabled_texture_units = enabled_texture_units;

    m_triangle_list.clear_with_capacity();
    m_processed_triangles.clear_with_capacity();

    // Let's construct some triangles
    if (primitive_type == PrimitiveType::Triangles) {
        Triangle triangle;
        if (vertices.size() < 3)
            return;
        for (size_t i = 0; i < vertices.size() - 2; i += 3) {
            triangle.vertices[0] = vertices.at(i);
            triangle.vertices[1] = vertices.at(i + 1);
            triangle.vertices[2] = vertices.at(i + 2);

            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == PrimitiveType::Quads) {
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
    } else if (primitive_type == PrimitiveType::TriangleFan) {
        Triangle triangle;
        triangle.vertices[0] = vertices.at(0); // Root vertex is always the vertex defined first

        // This is technically `n-2` triangles. We start at index 1
        for (size_t i = 1; i < vertices.size() - 1; i++) {
            triangle.vertices[1] = vertices.at(i);
            triangle.vertices[2] = vertices.at(i + 1);
            m_triangle_list.append(triangle);
        }
    } else if (primitive_type == PrimitiveType::TriangleStrip) {
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

    // Now let's transform each triangle and send that to the GPU
    auto const viewport = window_coordinates_to_target_coordinates(m_options.viewport);
    auto const viewport_half_width = viewport.width() / 2.0f;
    auto const viewport_half_height = viewport.height() / 2.0f;
    auto const viewport_center_x = viewport.x() + viewport_half_width;
    auto const viewport_center_y = viewport.y() + viewport_half_height;
    auto const depth_half_range = (m_options.depth_max - m_options.depth_min) / 2;
    auto const depth_halfway = (m_options.depth_min + m_options.depth_max) / 2;
    for (auto& triangle : m_triangle_list) {
        // Transform vertices into eye coordinates using the model-view transform
        triangle.vertices[0].eye_coordinates = model_view_transform * triangle.vertices[0].position;
        triangle.vertices[1].eye_coordinates = model_view_transform * triangle.vertices[1].position;
        triangle.vertices[2].eye_coordinates = model_view_transform * triangle.vertices[2].position;

        // Transform the vertex normals into eye-space
        triangle.vertices[0].normal = transform_direction(model_view_transform, triangle.vertices[0].normal);
        triangle.vertices[1].normal = transform_direction(model_view_transform, triangle.vertices[1].normal);
        triangle.vertices[2].normal = transform_direction(model_view_transform, triangle.vertices[2].normal);

        // Calculate per-vertex lighting
        if (m_options.lighting_enabled) {
            auto const& material = m_materials.at(0);
            for (auto& vertex : triangle.vertices) {
                auto ambient = material.ambient;
                auto diffuse = material.diffuse;
                auto emissive = material.emissive;
                auto specular = material.specular;

                if (m_options.color_material_enabled
                    && (m_options.color_material_face == ColorMaterialFace::Front || m_options.color_material_face == ColorMaterialFace::FrontAndBack)) {
                    switch (m_options.color_material_mode) {
                    case ColorMaterialMode::Ambient:
                        ambient = vertex.color;
                        break;
                    case ColorMaterialMode::AmbientAndDiffuse:
                        ambient = vertex.color;
                        diffuse = vertex.color;
                        break;
                    case ColorMaterialMode::Diffuse:
                        diffuse = vertex.color;
                        break;
                    case ColorMaterialMode::Emissive:
                        emissive = vertex.color;
                        break;
                    case ColorMaterialMode::Specular:
                        specular = vertex.color;
                        break;
                    }
                }

                FloatVector4 result_color = emissive + (ambient * m_lighting_model.scene_ambient_color);

                for (auto const& light : m_lights) {
                    if (!light.is_enabled)
                        continue;

                    // We need to save the length here because the attenuation factor requires a non
                    // normalized vector!
                    auto sgi_arrow_operator = [](FloatVector4 const& p1, FloatVector4 const& p2, float& saved_length) {
                        if ((p1.w() != 0.0f) && (p2.w() == 0.0f)) {
                            saved_length = p2.length();
                            return (p2 / saved_length).xyz();
                        } else if ((p1.w() == 0.0f) && (p2.w() != 0.0f)) {
                            saved_length = p2.length();
                            return -(p1 / saved_length).xyz();
                        } else {
                            // FIXME: The OpenGL 1.5 spec says nothing about the case where P1 and P2 BOTH have a w value of 1, which would
                            // then mean the light position has an implicit value of (0, 0, 0, 0). This doesn't make any logical sense, and it most likely
                            // a typographical error. Most other GL implementations seem to just fix it to the distance from the vertex to the light, which
                            // seems to work just fine.
                            // If somebody with more insight about this could clarify this eventually, that'd be great.
                            auto distance = (p2 - p1);
                            saved_length = distance.length();
                            return (distance / saved_length).xyz();
                        }
                    };

                    auto sgi_dot_operator = [](FloatVector3 const& d1, FloatVector3 const& d2) {
                        return AK::max(d1.dot(d2), 0.0f);
                    };

                    float vector_length = 0.0f;
                    FloatVector3 vertex_to_light = sgi_arrow_operator(vertex.eye_coordinates, light.position, vector_length);

                    // Light attenuation value.
                    float light_attenuation_factor = 1.0f;
                    if (light.position.w() != 0.0f) {
                        auto const vertex_to_light_length = vertex_to_light.length();
                        auto const vertex_to_light_length_squared = vertex_to_light_length * vertex_to_light_length;

                        light_attenuation_factor = 1.0f / (light.constant_attenuation + (light.linear_attenuation * vertex_to_light_length) + (light.quadratic_attenuation * vertex_to_light_length_squared));
                    }

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
                    (void)m_lighting_model.single_color;

                    // FIXME: Two sided lighting should be implemented eventually (I believe this is where the normals are -ve and then lighting is calculated with the BACK material)
                    (void)m_lighting_model.two_sided_lighting;

                    // Ambient
                    auto const ambient_component = ambient * light.ambient_intensity;

                    // Diffuse
                    auto const normal_dot_vertex_to_light = sgi_dot_operator(vertex.normal, vertex_to_light);
                    auto const diffuse_component = ((diffuse * light.diffuse_intensity) * normal_dot_vertex_to_light);

                    // Specular
                    FloatVector4 specular_component = { 0.0f, 0.0f, 0.0f, 0.0f };
                    if (normal_dot_vertex_to_light > 0.0f) {
                        FloatVector3 half_vector_normalized;
                        if (!m_lighting_model.viewer_at_infinity) {
                            half_vector_normalized = (vertex_to_light + FloatVector3(0.0f, 0.0f, 1.0f)).normalized();
                        } else {
                            auto const vertex_to_eye_point = sgi_arrow_operator(vertex.eye_coordinates.normalized(), FloatVector4(0.0f, 0.0f, 0.0f, 1.0f), vector_length);
                            half_vector_normalized = vertex_to_light + vertex_to_eye_point;
                        }

                        auto const normal_dot_half_vector = sgi_dot_operator(vertex.normal.normalized(), half_vector_normalized);
                        auto const specular_coefficient = AK::pow(normal_dot_half_vector, material.shininess);
                        specular_component = (specular * light.specular_intensity) * specular_coefficient;
                    }

                    FloatVector4 color = ambient_component;
                    color += diffuse_component;
                    color += specular_component;
                    color = color * light_attenuation_factor * spotlight_factor;
                    result_color += color;
                }

                vertex.color = result_color;
                vertex.color.set_w(diffuse.w()); // OpenGL 1.5 spec, page 59: "The A produced by lighting is the alpha value associated with diffuse color material"
                vertex.color.clamp(0.0f, 1.0f);
            }
        }

        // Transform eye coordinates into clip coordinates using the projection transform
        triangle.vertices[0].clip_coordinates = projection_transform * triangle.vertices[0].eye_coordinates;
        triangle.vertices[1].clip_coordinates = projection_transform * triangle.vertices[1].eye_coordinates;
        triangle.vertices[2].clip_coordinates = projection_transform * triangle.vertices[2].eye_coordinates;

        // At this point, we're in clip space
        // Here's where we do the clipping. This is a really crude implementation of the
        // https://learnopengl.com/Getting-started/Coordinate-Systems
        // "Note that if only a part of a primitive e.g. a triangle is outside the clipping volume OpenGL
        // will reconstruct the triangle as one or more triangles to fit inside the clipping range. "
        //
        // ALL VERTICES ARE DEFINED IN A CLOCKWISE ORDER

        // Okay, let's do some face culling first

        m_clipped_vertices.clear_with_capacity();
        m_clipped_vertices.append(triangle.vertices[0]);
        m_clipped_vertices.append(triangle.vertices[1]);
        m_clipped_vertices.append(triangle.vertices[2]);
        m_clipper.clip_triangle_against_frustum(m_clipped_vertices);

        if (m_clipped_vertices.size() < 3)
            continue;

        for (auto& vec : m_clipped_vertices) {
            // To normalized device coordinates (NDC)
            auto const one_over_w = 1 / vec.clip_coordinates.w();
            auto const ndc_coordinates = FloatVector4 {
                vec.clip_coordinates.x() * one_over_w,
                vec.clip_coordinates.y() * one_over_w,
                vec.clip_coordinates.z() * one_over_w,
                one_over_w,
            };

            // To window coordinates - note that we flip the Y coordinate into target space
            vec.window_coordinates = {
                viewport_center_x + ndc_coordinates.x() * viewport_half_width,
                viewport_center_y - ndc_coordinates.y() * viewport_half_height,
                depth_halfway + ndc_coordinates.z() * depth_half_range,
                ndc_coordinates.w(),
            };
        }

        Triangle tri;
        tri.vertices[0] = m_clipped_vertices[0];
        for (size_t i = 1; i < m_clipped_vertices.size() - 1; i++) {
            tri.vertices[1] = m_clipped_vertices[i];
            tri.vertices[2] = m_clipped_vertices[i + 1];
            m_processed_triangles.append(tri);
        }
    }

    // Generate texture coordinates if at least one coordinate is enabled
    bool texture_coordinate_generation_enabled = false;
    for (auto const coordinates_enabled : m_options.texcoord_generation_enabled_coordinates) {
        if (coordinates_enabled != TexCoordGenerationCoordinate::None) {
            texture_coordinate_generation_enabled = true;
            break;
        }
    }

    for (auto& triangle : m_processed_triangles) {
        // Let's calculate the (signed) area of the triangle
        // https://cp-algorithms.com/geometry/oriented-triangle-area.html
        float dxAB = triangle.vertices[0].window_coordinates.x() - triangle.vertices[1].window_coordinates.x(); // A.x - B.x
        float dxBC = triangle.vertices[1].window_coordinates.x() - triangle.vertices[2].window_coordinates.x(); // B.X - C.x
        float dyAB = triangle.vertices[0].window_coordinates.y() - triangle.vertices[1].window_coordinates.y();
        float dyBC = triangle.vertices[1].window_coordinates.y() - triangle.vertices[2].window_coordinates.y();
        float area = (dxAB * dyBC) - (dxBC * dyAB);

        if (area == 0.0f)
            continue;

        if (m_options.enable_culling) {
            bool is_front = (m_options.front_face == WindingOrder::CounterClockwise ? area < 0 : area > 0);

            if (!is_front && m_options.cull_back)
                continue;

            if (is_front && m_options.cull_front)
                continue;
        }

        if (area > 0)
            swap(triangle.vertices[0], triangle.vertices[1]);

        // Transform normals
        triangle.vertices[0].normal = normal_transform * triangle.vertices[0].normal;
        triangle.vertices[1].normal = normal_transform * triangle.vertices[1].normal;
        triangle.vertices[2].normal = normal_transform * triangle.vertices[2].normal;
        if (m_options.normalization_enabled) {
            triangle.vertices[0].normal.normalize();
            triangle.vertices[1].normal.normalize();
            triangle.vertices[2].normal.normalize();
        }

        if (texture_coordinate_generation_enabled) {
            generate_texture_coordinates(triangle.vertices[0], m_options);
            generate_texture_coordinates(triangle.vertices[1], m_options);
            generate_texture_coordinates(triangle.vertices[2], m_options);
        }

        // Apply texture transformation
        for (size_t i = 0; i < NUM_SAMPLERS; ++i) {
            triangle.vertices[0].tex_coords[i] = texture_transform * triangle.vertices[0].tex_coords[i];
            triangle.vertices[1].tex_coords[i] = texture_transform * triangle.vertices[1].tex_coords[i];
            triangle.vertices[2].tex_coords[i] = texture_transform * triangle.vertices[2].tex_coords[i];
        }

        rasterize_triangle(triangle);
    }
}

ALWAYS_INLINE void Device::shade_fragments(PixelQuad& quad)
{
    quad.out_color = quad.vertex_color;

    for (size_t i : m_enabled_texture_units) {
        // FIXME: implement GL_TEXTURE_1D, GL_TEXTURE_3D and GL_TEXTURE_CUBE_MAP
        auto const& sampler = m_samplers[i];

        auto texel = sampler.sample_2d({ quad.texture_coordinates[i].x(), quad.texture_coordinates[i].y() });
        INCREASE_STATISTICS_COUNTER(g_num_sampler_calls, 1);

        // FIXME: Implement more blend modes
        switch (sampler.config().fixed_function_texture_env_mode) {
        case TextureEnvMode::Modulate:
            quad.out_color = quad.out_color * texel;
            break;
        case TextureEnvMode::Replace:
            quad.out_color = texel;
            break;
        case TextureEnvMode::Decal: {
            auto src_alpha = quad.out_color.w();
            quad.out_color.set_x(mix(quad.out_color.x(), texel.x(), src_alpha));
            quad.out_color.set_y(mix(quad.out_color.y(), texel.y(), src_alpha));
            quad.out_color.set_z(mix(quad.out_color.z(), texel.z(), src_alpha));
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }
    }

    // Calculate fog
    // Math from here: https://opengl-notes.readthedocs.io/en/latest/topics/texturing/aliasing.html

    // FIXME: exponential fog is not vectorized, we should add a SIMD exp function that calculates an approximation.
    if (m_options.fog_enabled) {
        auto factor = expand4(0.0f);
        switch (m_options.fog_mode) {
        case FogMode::Linear:
            factor = (m_options.fog_end - quad.fog_depth) / (m_options.fog_end - m_options.fog_start);
            break;
        case FogMode::Exp: {
            auto argument = -m_options.fog_density * quad.fog_depth;
            factor = exp(argument);
        } break;
        case FogMode::Exp2: {
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
}

ALWAYS_INLINE bool Device::test_alpha(PixelQuad& quad)
{
    auto const alpha = quad.out_color.w();
    auto const ref_value = expand4(m_options.alpha_test_ref_value);

    switch (m_options.alpha_test_func) {
    case AlphaTestFunction::Less:
        quad.mask &= alpha < ref_value;
        break;
    case AlphaTestFunction::Equal:
        quad.mask &= alpha == ref_value;
        break;
    case AlphaTestFunction::LessOrEqual:
        quad.mask &= alpha <= ref_value;
        break;
    case AlphaTestFunction::Greater:
        quad.mask &= alpha > ref_value;
        break;
    case AlphaTestFunction::NotEqual:
        quad.mask &= alpha != ref_value;
        break;
    case AlphaTestFunction::GreaterOrEqual:
        quad.mask &= alpha >= ref_value;
        break;
    case AlphaTestFunction::Never:
    case AlphaTestFunction::Always:
    default:
        VERIFY_NOT_REACHED();
    }

    return any(quad.mask);
}

void Device::resize(const Gfx::IntSize& size)
{
    wait_for_all_threads();

    m_render_target = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size).release_value_but_fixme_should_propagate_errors();
    m_depth_buffer = adopt_own(*new DepthBuffer(size));
}

void Device::clear_color(const FloatVector4& color)
{
    wait_for_all_threads();

    uint8_t r = static_cast<uint8_t>(clamp(color.x(), 0.0f, 1.0f) * 255);
    uint8_t g = static_cast<uint8_t>(clamp(color.y(), 0.0f, 1.0f) * 255);
    uint8_t b = static_cast<uint8_t>(clamp(color.z(), 0.0f, 1.0f) * 255);
    uint8_t a = static_cast<uint8_t>(clamp(color.w(), 0.0f, 1.0f) * 255);
    auto const fill_color = Gfx::Color(r, g, b, a);

    if (m_options.scissor_enabled) {
        auto fill_rect = m_render_target->rect();
        fill_rect.intersect(window_coordinates_to_target_coordinates(m_options.scissor_box));
        Gfx::Painter painter { *m_render_target };
        painter.fill_rect(fill_rect, fill_color);
        return;
    }

    m_render_target->fill(fill_color);
}

void Device::clear_depth(float depth)
{
    wait_for_all_threads();

    if (m_options.scissor_enabled) {
        m_depth_buffer->clear(window_coordinates_to_target_coordinates(m_options.scissor_box), depth);
        return;
    }

    m_depth_buffer->clear(depth);
}

void Device::clear_stencil(u8 value)
{
    Gfx::IntRect clear_rect = m_stencil_buffer->rect();

    if (m_options.scissor_enabled)
        clear_rect.intersect(window_coordinates_to_target_coordinates(m_options.scissor_box));

    m_stencil_buffer->clear(clear_rect, value);
}

void Device::blit_to_color_buffer_at_raster_position(Gfx::Bitmap const& source)
{
    if (!m_raster_position.valid)
        return;

    wait_for_all_threads();

    INCREASE_STATISTICS_COUNTER(g_num_pixels, source.width() * source.height());
    INCREASE_STATISTICS_COUNTER(g_num_pixels_shaded, source.width() * source.height());

    Gfx::Painter painter { *m_render_target };
    auto const blit_rect = raster_rect_in_target_coordinates(source.size());
    painter.blit({ blit_rect.x(), blit_rect.y() }, source, source.rect(), 1.0f, true);
}

void Device::blit_to_depth_buffer_at_raster_position(Vector<float> const& depth_values, size_t width, size_t height)
{
    if (!m_raster_position.valid)
        return;

    auto const raster_rect = raster_rect_in_target_coordinates({ width, height });
    auto const y1 = raster_rect.y();
    auto const y2 = y1 + height;
    auto const x1 = raster_rect.x();
    int const x2 = x1 + width;

    auto index = 0;
    for (int y = y2 - 1; y >= y1; --y) {
        auto depth_line = m_depth_buffer->scanline(y);
        for (int x = x1; x < x2; ++x) {
            depth_line[x] = depth_values.at(index++);
        }
    }
}

void Device::blit_to(Gfx::Bitmap& target)
{
    wait_for_all_threads();

    Gfx::Painter painter { target };
    painter.blit({ 0, 0 }, *m_render_target, m_render_target->rect(), 1.0f, false);

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

        int num_rendertarget_pixels = m_render_target->width() * m_render_target->height();

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

void Device::wait_for_all_threads() const
{
    // FIXME: Wait for all render threads to finish when multithreading is being implemented
}

void Device::set_options(const RasterizerOptions& options)
{
    wait_for_all_threads();

    m_options = options;

    if (m_options.enable_blending)
        setup_blend_factors();

    // FIXME: Recreate or reinitialize render threads here when multithreading is being implemented
}

void Device::set_light_model_params(const LightModelParameters& lighting_model)
{
    wait_for_all_threads();

    m_lighting_model = lighting_model;

    // FIXME: Recreate or reinitialize render threads here when multithreading is being implemented
}

Gfx::RGBA32 Device::get_backbuffer_pixel(int x, int y)
{
    // FIXME: Reading individual pixels is very slow, rewrite this to transfer whole blocks
    if (x < 0 || y < 0 || x >= m_render_target->width() || y >= m_render_target->height())
        return 0;

    return m_render_target->scanline(y)[x];
}

float Device::get_depthbuffer_value(int x, int y)
{
    // FIXME: Reading individual pixels is very slow, rewrite this to transfer whole blocks
    if (x < 0 || y < 0 || x >= m_render_target->width() || y >= m_render_target->height())
        return 1.0f;

    return m_depth_buffer->scanline(y)[x];
}

NonnullRefPtr<Image> Device::create_image(ImageFormat format, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers)
{
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(levels > 0);
    VERIFY(layers > 0);

    return adopt_ref(*new Image(format, width, height, depth, levels, layers));
}

void Device::set_sampler_config(unsigned sampler, SamplerConfig const& config)
{
    m_samplers[sampler].set_config(config);
}

void Device::set_light_state(unsigned int light_id, Light const& light)
{
    m_lights.at(light_id) = light;
}

void Device::set_material_state(Face face, Material const& material)
{
    m_materials[face] = material;
}

void Device::set_stencil_configuration(Face face, StencilConfiguration const& stencil_configuration)
{
    m_stencil_configuration[face] = stencil_configuration;
}

void Device::set_raster_position(RasterPosition const& raster_position)
{
    m_raster_position = raster_position;
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

Gfx::IntRect Device::raster_rect_in_target_coordinates(Gfx::IntSize size)
{
    auto const raster_rect = Gfx::IntRect {
        static_cast<int>(m_raster_position.window_coordinates.x()),
        static_cast<int>(m_raster_position.window_coordinates.y()),
        size.width(),
        size.height(),
    };
    return window_coordinates_to_target_coordinates(raster_rect);
}

}
