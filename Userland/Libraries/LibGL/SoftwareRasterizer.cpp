/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftwareRasterizer.h"
#include <AK/Function.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector3.h>

namespace GL {

using IntVector2 = Gfx::Vector2<int>;
using IntVector3 = Gfx::Vector3<int>;

static constexpr int RASTERIZER_BLOCK_SIZE = 16;

constexpr static int edge_function(const IntVector2& a, const IntVector2& b, const IntVector2& c)
{
    return ((c.x() - a.x()) * (b.y() - a.y()) - (c.y() - a.y()) * (b.x() - a.x()));
}

template<typename T>
constexpr static T interpolate(const T& v0, const T& v1, const T& v2, const FloatVector3& barycentric_coords)
{
    return v0 * barycentric_coords.x() + v1 * barycentric_coords.y() + v2 * barycentric_coords.z();
}

static Gfx::RGBA32 to_rgba32(const FloatVector4& v)
{
    auto clamped = v.clamped(0, 1);
    u8 r = clamped.x() * 255;
    u8 g = clamped.y() * 255;
    u8 b = clamped.z() * 255;
    u8 a = clamped.w() * 255;
    return a << 24 | b << 16 | g << 8 | r;
}

static FloatVector4 to_vec4(Gfx::RGBA32 rgba)
{
    return {
        (rgba & 0xff) / 255.0f,
        ((rgba >> 8) & 0xff) / 255.0f,
        ((rgba >> 16) & 0xff) / 255.0f,
        ((rgba >> 24) & 0xff) / 255.0f
    };
}

static constexpr void setup_blend_factors(GLenum mode, FloatVector4& constant, float& src_alpha, float& dst_alpha, float& src_color, float& dst_color)
{
    constant = { 0.0f, 0.0f, 0.0f, 0.0f };
    src_alpha = 0;
    dst_alpha = 0;
    src_color = 0;
    dst_color = 0;

    switch (mode) {
    case GL_ZERO:
        break;
    case GL_ONE:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        break;
    case GL_SRC_COLOR:
        src_color = 1;
        break;
    case GL_ONE_MINUS_SRC_COLOR:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        src_color = -1;
        break;
    case GL_SRC_ALPHA:
        src_alpha = 1;
        break;
    case GL_ONE_MINUS_SRC_ALPHA:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        src_alpha = -1;
        break;
    case GL_DST_ALPHA:
        dst_alpha = -1;
        break;
    case GL_ONE_MINUS_DST_ALPHA:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        dst_alpha = -1;
        break;
    case GL_DST_COLOR:
        dst_color = 1;
        break;
    case GL_ONE_MINUS_DST_COLOR:
        constant = { 1.0f, 1.0f, 1.0f, 1.0f };
        dst_color = -1;
        break;
    case GL_SRC_ALPHA_SATURATE:
        // FIXME: How do we implement this?
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

template<typename PS>
static void rasterize_triangle(const RasterizerOptions& options, Gfx::Bitmap& render_target, DepthBuffer& depth_buffer, const GLTriangle& triangle, PS pixel_shader)
{
    // Since the algorithm is based on blocks of uniform size, we need
    // to ensure that our render_target size is actually a multiple of the block size
    VERIFY((render_target.width() % RASTERIZER_BLOCK_SIZE) == 0);
    VERIFY((render_target.height() % RASTERIZER_BLOCK_SIZE) == 0);

    // Calculate area of the triangle for later tests
    IntVector2 v0 { (int)triangle.vertices[0].x, (int)triangle.vertices[0].y };
    IntVector2 v1 { (int)triangle.vertices[1].x, (int)triangle.vertices[1].y };
    IntVector2 v2 { (int)triangle.vertices[2].x, (int)triangle.vertices[2].y };

    int area = edge_function(v0, v1, v2);
    if (area == 0)
        return;

    float one_over_area = 1.0f / area;

    FloatVector4 src_constant {};
    float src_factor_src_alpha = 0;
    float src_factor_dst_alpha = 0;
    float src_factor_src_color = 0;
    float src_factor_dst_color = 0;

    FloatVector4 dst_constant {};
    float dst_factor_src_alpha = 0;
    float dst_factor_dst_alpha = 0;
    float dst_factor_src_color = 0;
    float dst_factor_dst_color = 0;

    if (options.enable_blending) {
        setup_blend_factors(
            options.blend_source_factor,
            src_constant,
            src_factor_src_alpha,
            src_factor_dst_alpha,
            src_factor_src_color,
            src_factor_dst_color);

        setup_blend_factors(
            options.blend_source_factor,
            dst_constant,
            dst_factor_src_alpha,
            dst_factor_dst_alpha,
            dst_factor_src_color,
            dst_factor_dst_color);
    }

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
    auto calculate_edge_values = [v0, v1, v2](const IntVector2& p) -> IntVector3 {
        return {
            edge_function(v1, v2, p),
            edge_function(v2, v0, p),
            edge_function(v0, v1, p),
        };
    };

    // This function tests whether a point as identified by its 3 edge values lies within the triangle
    auto test_point = [zero](const IntVector3& edges) -> bool {
        return edges.x() >= zero.x()
            && edges.y() >= zero.y()
            && edges.z() >= zero.z();
    };

    // Calculate block-based bounds
    // clang-format off
    const int bx0 = max(0,                      min(min(v0.x(), v1.x()), v2.x())                            ) / RASTERIZER_BLOCK_SIZE;
    const int bx1 = min(render_target.width(),  max(max(v0.x(), v1.x()), v2.x()) + RASTERIZER_BLOCK_SIZE - 1) / RASTERIZER_BLOCK_SIZE;
    const int by0 = max(0,                      min(min(v0.y(), v1.y()), v2.y())                            ) / RASTERIZER_BLOCK_SIZE;
    const int by1 = min(render_target.height(), max(max(v0.y(), v1.y()), v2.y()) + RASTERIZER_BLOCK_SIZE - 1) / RASTERIZER_BLOCK_SIZE;
    // clang-format on

    static_assert(RASTERIZER_BLOCK_SIZE < sizeof(int) * 8, "RASTERIZER_BLOCK_SIZE must be smaller than the pixel_mask's width in bits");
    int pixel_mask[RASTERIZER_BLOCK_SIZE];

    FloatVector4 pixel_buffer[RASTERIZER_BLOCK_SIZE][RASTERIZER_BLOCK_SIZE];

    // Iterate over all blocks within the bounds of the triangle
    for (int by = by0; by < by1; by++) {
        for (int bx = bx0; bx < bx1; bx++) {

            // Edge values of the 4 block corners
            // clang-format off
            auto b0 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE,                         by * RASTERIZER_BLOCK_SIZE });
            auto b1 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE, by * RASTERIZER_BLOCK_SIZE });
            auto b2 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE,                         by * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE });
            auto b3 = calculate_edge_values({ bx * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE, by * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE });
            // clang-format on

            // If the whole block is outside any of the triangle edges we can discard it completely
            // We test this by and'ing the relevant edge function values together for all block corners
            // and checking if the negative sign bit is set for all of them
            if ((b0.x() & b1.x() & b2.x() & b3.x()) & 0x80000000)
                continue;

            if ((b0.y() & b1.y() & b2.y() & b3.y()) & 0x80000000)
                continue;

            if ((b0.z() & b1.z() & b2.z() & b3.z()) & 0x80000000)
                continue;

            // edge value derivatives
            auto dbdx = (b1 - b0) / RASTERIZER_BLOCK_SIZE;
            auto dbdy = (b2 - b0) / RASTERIZER_BLOCK_SIZE;
            // step edge value after each horizontal span: 1 down, BLOCK_SIZE left
            auto step_y = dbdy - dbdx * RASTERIZER_BLOCK_SIZE;

            int x0 = bx * RASTERIZER_BLOCK_SIZE;
            int y0 = by * RASTERIZER_BLOCK_SIZE;

            // Generate the coverage mask
            if (test_point(b0) && test_point(b1) && test_point(b2) && test_point(b3)) {
                // The block is fully contained within the triangle. Fill the mask with all 1s
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    pixel_mask[y] = -1;
                }
            } else {
                // The block overlaps at least one triangle edge.
                // We need to test coverage of every pixel within the block.
                auto coords = b0;
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++, coords += step_y) {
                    pixel_mask[y] = 0;

                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, coords += dbdx) {
                        if (test_point(coords))
                            pixel_mask[y] |= 1 << x;
                    }
                }
            }

            // AND the depth mask onto the coverage mask
            if (options.enable_depth_test) {
                int z_pass_count = 0;
                auto coords = b0;

                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++, coords += step_y) {
                    if (pixel_mask[y] == 0) {
                        coords += dbdx * RASTERIZER_BLOCK_SIZE;
                        continue;
                    }

                    auto* depth = &depth_buffer.scanline(y0 + y)[x0];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, coords += dbdx, depth++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        auto barycentric = FloatVector3(coords.x(), coords.y(), coords.z()) * one_over_area;
                        float z = interpolate(triangle.vertices[0].z, triangle.vertices[1].z, triangle.vertices[2].z, barycentric);
                        if (z >= *depth) {
                            pixel_mask[y] ^= 1 << x;
                            continue;
                        }

                        *depth = z;
                        z_pass_count++;
                    }
                }

                // Nice, no pixels passed the depth test -> block rejected by early z
                if (z_pass_count == 0)
                    continue;
            }

            // Draw the pixels according to the previously generated mask
            auto coords = b0;
            for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++, coords += step_y) {
                if (pixel_mask[y] == 0) {
                    coords += dbdx * RASTERIZER_BLOCK_SIZE;
                    continue;
                }

                auto* pixel = pixel_buffer[y];
                for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, coords += dbdx, pixel++) {
                    if (~pixel_mask[y] & (1 << x))
                        continue;

                    // Perspective correct barycentric coordinates
                    auto barycentric = FloatVector3(coords.x(), coords.y(), coords.z()) * one_over_area;
                    float interpolated_reciprocal_w = interpolate(triangle.vertices[0].w, triangle.vertices[1].w, triangle.vertices[2].w, barycentric);
                    float interpolated_w = 1 / interpolated_reciprocal_w;
                    barycentric = barycentric * FloatVector3(triangle.vertices[0].w, triangle.vertices[1].w, triangle.vertices[2].w) * interpolated_w;

                    // FIXME: make this more generic. We want to interpolate more than just color and uv
                    FloatVector4 vertex_color;
                    if (options.shade_smooth) {
                        vertex_color = interpolate(
                            FloatVector4(triangle.vertices[0].r, triangle.vertices[0].g, triangle.vertices[0].b, triangle.vertices[0].a),
                            FloatVector4(triangle.vertices[1].r, triangle.vertices[1].g, triangle.vertices[1].b, triangle.vertices[1].a),
                            FloatVector4(triangle.vertices[2].r, triangle.vertices[2].g, triangle.vertices[2].b, triangle.vertices[2].a),
                            barycentric);
                    } else {
                        vertex_color = { triangle.vertices[0].r, triangle.vertices[0].g, triangle.vertices[0].b, triangle.vertices[0].a };
                    }

                    auto uv = interpolate(
                        FloatVector2(triangle.vertices[0].u, triangle.vertices[0].v),
                        FloatVector2(triangle.vertices[1].u, triangle.vertices[1].v),
                        FloatVector2(triangle.vertices[2].u, triangle.vertices[2].v),
                        barycentric);

                    *pixel = pixel_shader(uv, vertex_color);
                }
            }

            if (options.enable_alpha_test && options.alpha_test_func != GL_ALWAYS) {
                // FIXME: I'm not sure if this is the right place to test this.
                // If we tested this right at the beginning of our rasterizer routine
                // we could skip a lot of work but the GL spec might disagree.
                if (options.alpha_test_func == GL_NEVER)
                    continue;

                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    auto src = pixel_buffer[y];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, src++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        bool passed = true;

                        switch (options.alpha_test_func) {
                        case GL_LESS:
                            passed = src->w() < options.alpha_test_ref_value;
                            break;
                        case GL_EQUAL:
                            passed = src->w() == options.alpha_test_ref_value;
                            break;
                        case GL_LEQUAL:
                            passed = src->w() <= options.alpha_test_ref_value;
                            break;
                        case GL_GREATER:
                            passed = src->w() > options.alpha_test_ref_value;
                            break;
                        case GL_NOTEQUAL:
                            passed = src->w() != options.alpha_test_ref_value;
                            break;
                        case GL_GEQUAL:
                            passed = src->w() >= options.alpha_test_ref_value;
                            break;
                        }

                        if (!passed)
                            pixel_mask[y] ^= (1 << x);
                    }
                }
            }

            if (options.enable_blending) {
                // Blend color values from pixel_buffer into render_target
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    auto src = pixel_buffer[y];
                    auto dst = &render_target.scanline(y + y0)[x0];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, src++, dst++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        auto float_dst = to_vec4(*dst);

                        auto src_factor = src_constant
                            + *src * src_factor_src_color
                            + FloatVector4(src->w(), src->w(), src->w(), src->w()) * src_factor_src_alpha
                            + float_dst * src_factor_dst_color
                            + FloatVector4(float_dst.w(), float_dst.w(), float_dst.w(), float_dst.w()) * src_factor_dst_alpha;

                        auto dst_factor = dst_constant
                            + *src * dst_factor_src_color
                            + FloatVector4(src->w(), src->w(), src->w(), src->w()) * dst_factor_src_alpha
                            + float_dst * dst_factor_dst_color
                            + FloatVector4(float_dst.w(), float_dst.w(), float_dst.w(), float_dst.w()) * dst_factor_dst_alpha;

                        *dst = to_rgba32(*src * src_factor + float_dst * dst_factor);
                    }
                }
            } else {
                // Copy color values from pixel_buffer into render_target
                for (int y = 0; y < RASTERIZER_BLOCK_SIZE; y++) {
                    auto src = pixel_buffer[y];
                    auto dst = &render_target.scanline(y + y0)[x0];
                    for (int x = 0; x < RASTERIZER_BLOCK_SIZE; x++, src++, dst++) {
                        if (~pixel_mask[y] & (1 << x))
                            continue;

                        *dst = to_rgba32(*src);
                    }
                }
            }
        }
    }
}

static Gfx::IntSize closest_multiple(const Gfx::IntSize& min_size, size_t step)
{
    int width = ((min_size.width() + step - 1) / step) * step;
    int height = ((min_size.height() + step - 1) / step) * step;
    return { width, height };
}

SoftwareRasterizer::SoftwareRasterizer(const Gfx::IntSize& min_size)
    : m_render_target { Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, closest_multiple(min_size, RASTERIZER_BLOCK_SIZE)) }
    , m_depth_buffer { adopt_own(*new DepthBuffer(closest_multiple(min_size, RASTERIZER_BLOCK_SIZE))) }
{
}

void SoftwareRasterizer::submit_triangle(const GLTriangle& triangle)
{
    rasterize_triangle(m_options, *m_render_target, *m_depth_buffer, triangle, [](const FloatVector2&, const FloatVector4& color) -> FloatVector4 {
        return color;
    });
}

void SoftwareRasterizer::submit_triangle(const GLTriangle& triangle, const Array<TextureUnit, 32>& texture_units)
{
    rasterize_triangle(m_options, *m_render_target, *m_depth_buffer, triangle, [&texture_units](const FloatVector2& uv, const FloatVector4& color) -> FloatVector4 {
        // TODO: We'd do some kind of multitexturing/blending here
        // Construct a vector for the texel we want to sample
        FloatVector4 texel = color;

        for (const auto& texture_unit : texture_units) {

            // No texture is bound to this texture unit
            if (!texture_unit.is_bound())
                continue;

            // FIXME: Don't assume Texture2D, _and_ work out how we blend/do multitexturing properly.....
            texel = texel * static_ptr_cast<Texture2D>(texture_unit.bound_texture())->sample_texel(uv);
        }

        return texel;
    });
}

void SoftwareRasterizer::resize(const Gfx::IntSize& min_size)
{
    wait_for_all_threads();

    m_render_target = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, closest_multiple(min_size, RASTERIZER_BLOCK_SIZE));
    m_depth_buffer = adopt_own(*new DepthBuffer(m_render_target->size()));
}

void SoftwareRasterizer::clear_color(const FloatVector4& color)
{
    wait_for_all_threads();

    uint8_t r = static_cast<uint8_t>(clamp(color.x(), 0.0f, 1.0f) * 255);
    uint8_t g = static_cast<uint8_t>(clamp(color.y(), 0.0f, 1.0f) * 255);
    uint8_t b = static_cast<uint8_t>(clamp(color.z(), 0.0f, 1.0f) * 255);
    uint8_t a = static_cast<uint8_t>(clamp(color.w(), 0.0f, 1.0f) * 255);

    m_render_target->fill(Gfx::Color(r, g, b, a));
}

void SoftwareRasterizer::clear_depth(float depth)
{
    wait_for_all_threads();

    m_depth_buffer->clear(depth);
}

void SoftwareRasterizer::blit_to(Gfx::Bitmap& target)
{
    wait_for_all_threads();

    Gfx::Painter painter { target };
    painter.blit({ 0, 0 }, *m_render_target, m_render_target->rect(), 1.0f, false);
}

void SoftwareRasterizer::wait_for_all_threads() const
{
    // FIXME: Wait for all render threads to finish when multithreading is being implemented
}

void SoftwareRasterizer::set_options(const RasterizerOptions& options)
{
    wait_for_all_threads();

    m_options = options;

    // FIXME: Recreate or reinitialize render threads here when multithreading is being implemented
}

Gfx::RGBA32 SoftwareRasterizer::get_backbuffer_pixel(int x, int y)
{
    // FIXME: Reading individual pixels is very slow, rewrite this to transfer whole blocks
    if (x < 0 || y < 0 || x >= m_render_target->width() || y >= m_render_target->height())
        return 0;

    return m_render_target->scanline(y)[x];
}

float SoftwareRasterizer::get_depthbuffer_value(int x, int y)
{
    // FIXME: Reading individual pixels is very slow, rewrite this to transfer whole blocks
    if (x < 0 || y < 0 || x >= m_render_target->width() || y >= m_render_target->height())
        return 1.0f;

    return m_depth_buffer->scanline(y)[x];
}

}
