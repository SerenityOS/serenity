/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftwareRasterizer.h"
#include <AK/Function.h>
#include <LibGfx/Painter.h>

namespace GL {

static constexpr size_t RASTERIZER_BLOCK_SIZE = 16;

struct FloatVector2 {
    float x;
    float y;
};

constexpr static float triangle_area(const FloatVector2& a, const FloatVector2& b, const FloatVector2& c)
{
    return ((c.x - a.x) * (b.y - a.y) - (c.y - a.y) * (b.x - a.x)) / 2;
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

template<typename PS>
static void rasterize_triangle(Gfx::Bitmap& render_target, const GLTriangle& triangle, PS pixel_shader)
{
    // Since the algorithm is based on blocks of uniform size, we need
    // to ensure that our render_target size is actually a multiple of the block size
    VERIFY((render_target.width() % RASTERIZER_BLOCK_SIZE) == 0);
    VERIFY((render_target.height() % RASTERIZER_BLOCK_SIZE) == 0);

    // Calculate area of the triangle for later tests
    FloatVector2 v0 = { triangle.vertices[0].x, triangle.vertices[0].y };
    FloatVector2 v1 = { triangle.vertices[1].x, triangle.vertices[1].y };
    FloatVector2 v2 = { triangle.vertices[2].x, triangle.vertices[2].y };

    float area = triangle_area(v0, v1, v2);
    if (area == 0)
        return;

    float one_over_area = 1 / area;

    // Obey top-left rule:
    // This sets up "zero" for later pixel coverage tests.
    // Depending on where on the triangle the edge is located
    // it is either tested against 0 or float epsilon, effectively
    // turning "< 0" into "<= 0"
    float constexpr epsilon = AK::NumericLimits<float>::epsilon();
    FloatVector4 zero { epsilon, epsilon, epsilon, 0.0f };
    if (v1.y > v0.y || (v1.y == v0.y && v1.x < v0.x))
        zero.set_z(0);
    if (v2.y > v1.y || (v2.y == v1.y && v2.x < v1.x))
        zero.set_x(0);
    if (v0.y > v2.y || (v0.y == v2.y && v0.x < v2.x))
        zero.set_y(0);

    // This function calculates the barycentric coordinates for the pixel relative to the triangle.
    auto barycentric_coordinates = [v0, v1, v2, one_over_area](float x, float y) -> FloatVector4 {
        FloatVector2 p { x, y };
        return {
            triangle_area(v1, v2, p) * one_over_area,
            triangle_area(v2, v0, p) * one_over_area,
            triangle_area(v0, v1, p) * one_over_area,
            0.0f
        };
    };

    // This function tests whether a point lies within the triangle
    auto test_point = [zero](const FloatVector4& point) -> bool {
        return point.x() >= zero.x()
            && point.y() >= zero.y()
            && point.z() >= zero.z();
    };

    // Calculate bounds
    FloatVector2 min { AK::min(v0.x, AK::min(v1.x, v2.x)), AK::min(v0.y, AK::min(v1.y, v2.y)) };
    FloatVector2 max { AK::max(v0.x, AK::max(v1.x, v2.x)), AK::max(v0.y, AK::max(v1.y, v2.y)) };

    // Calculate block-based bounds
    int iminx = floorf(min.x);
    int iminy = floorf(min.y);
    int imaxx = ceilf(max.x);
    int imaxy = ceilf(max.y);

    iminx = clamp(iminx, 0, render_target.width() - 1);
    imaxx = clamp(imaxx, 0, render_target.width() - 1);
    iminy = clamp(iminy, 0, render_target.height() - 1);
    imaxy = clamp(imaxy, 0, render_target.height() - 1);

    int bx0 = iminx / RASTERIZER_BLOCK_SIZE;
    int bx1 = imaxx / RASTERIZER_BLOCK_SIZE + 1;
    int by0 = iminy / RASTERIZER_BLOCK_SIZE;
    int by1 = imaxy / RASTERIZER_BLOCK_SIZE + 1;

    // Iterate over all blocks within the bounds of the triangle
    for (int by = by0; by < by1; by++) {
        for (int bx = bx0; bx < bx1; bx++) {

            // The 4 block corners
            int x0 = bx * RASTERIZER_BLOCK_SIZE;
            int y0 = by * RASTERIZER_BLOCK_SIZE;
            int x1 = bx * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE;
            int y1 = by * RASTERIZER_BLOCK_SIZE + RASTERIZER_BLOCK_SIZE;

            // Barycentric coordinates of the 4 block corners
            auto a = barycentric_coordinates(x0, y0);
            auto b = barycentric_coordinates(x1, y0);
            auto c = barycentric_coordinates(x0, y1);
            auto d = barycentric_coordinates(x1, y1);

            // If the whole block is outside any of the triangle edges we can discard it completely
            if ((a.x() < zero.x() && b.x() < zero.x() && c.x() < zero.x() && d.x() < zero.x())
                || (a.y() < zero.y() && b.y() < zero.y() && c.y() < zero.y() && d.y() < zero.y())
                || (a.z() < zero.z() && b.z() < zero.z() && c.z() < zero.z() && d.z() < zero.z()))
                continue;

            // barycentric coordinate derrivatives
            auto dcdx = (b - a) / RASTERIZER_BLOCK_SIZE;
            auto dcdy = (c - a) / RASTERIZER_BLOCK_SIZE;

            if (test_point(a) && test_point(b) && test_point(c) && test_point(d)) {
                // The block is fully contained within the triangle
                // Fill the block without further coverage tests
                for (int y = y0; y < y1; y++) {
                    auto coords = a;
                    auto* pixels = &render_target.scanline(y)[x0];
                    for (int x = x0; x < x1; x++) {
                        *pixels++ = to_rgba32(pixel_shader(coords, triangle));
                        coords = coords + dcdx;
                    }
                    a = a + dcdy;
                }
            } else {
                // The block overlaps at least one triangle edge
                // We need to test coverage of every pixel within the block
                for (int y = y0; y < y1; y++) {
                    auto coords = a;
                    auto* pixels = &render_target.scanline(y)[x0];
                    for (int x = x0; x < x1; x++) {
                        if (test_point(coords)) {
                            *pixels = to_rgba32(pixel_shader(coords, triangle));
                        }
                        pixels++;
                        coords = coords + dcdx;
                    }
                    a = a + dcdy;
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
{
}

void SoftwareRasterizer::submit_triangle(const GLTriangle& triangle)
{
    if (m_options.shade_smooth) {
        rasterize_triangle(*m_render_target, triangle, [](const FloatVector4& v, const GLTriangle& t) -> FloatVector4 {
            const float r = t.vertices[0].r * v.x() + t.vertices[1].r * v.y() + t.vertices[2].r * v.z();
            const float g = t.vertices[0].g * v.x() + t.vertices[1].g * v.y() + t.vertices[2].g * v.z();
            const float b = t.vertices[0].b * v.x() + t.vertices[1].b * v.y() + t.vertices[2].b * v.z();
            const float a = t.vertices[0].a * v.x() + t.vertices[1].a * v.y() + t.vertices[2].a * v.z();
            return { r, g, b, a };
        });
    } else {
        rasterize_triangle(*m_render_target, triangle, [](const FloatVector4&, const GLTriangle& t) -> FloatVector4 {
            return { t.vertices[0].r, t.vertices[0].g, t.vertices[0].b, t.vertices[0].a };
        });
    }
}

void SoftwareRasterizer::resize(const Gfx::IntSize& min_size)
{
    wait_for_all_threads();

    m_render_target = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, closest_multiple(min_size, RASTERIZER_BLOCK_SIZE));
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

void SoftwareRasterizer::clear_depth(float)
{
    wait_for_all_threads();

    // FIXME: implement this
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

}
