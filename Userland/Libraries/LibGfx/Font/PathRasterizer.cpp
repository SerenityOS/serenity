/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/PathRasterizer.h>

namespace Gfx {

PathRasterizer::PathRasterizer(Gfx::IntSize size)
    : m_size(size)
{
    m_data.resize(m_size.area());
    for (int i = 0; i < m_size.area(); i++)
        m_data[i] = 0.f;
}

void PathRasterizer::draw_path(Gfx::Path& path)
{
    for (auto& line : path.split_lines())
        draw_line(line.a(), line.b());
}

RefPtr<Gfx::Bitmap> PathRasterizer::accumulate()
{
    auto bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, m_size);
    if (bitmap_or_error.is_error())
        return {};
    auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    Color base_color = Color::from_rgb(0xffffff);
    for (int y = 0; y < m_size.height(); y++) {
        float accumulator = 0.f;
        for (int x = 0; x < m_size.width(); x++) {
            accumulator += m_data[y * m_size.width() + x];
            float value = AK::min(AK::abs(accumulator), 1.f);
            u8 alpha = value * 255.f;
            bitmap->set_pixel(x, y, base_color.with_alpha(alpha));
        }
    }
    return bitmap;
}

void PathRasterizer::draw_line(Gfx::FloatPoint p0, Gfx::FloatPoint p1)
{
    // FIXME: Shift x and y according to dy/dx
    if (p0.x() < 0.f)
        p0.set_x(roundf(p0.x()));
    if (p0.y() < 0.f)
        p0.set_y(roundf(p0.y()));
    if (p1.x() < 0.f)
        p1.set_x(roundf(p1.x()));
    if (p1.y() < 0.f)
        p1.set_y(roundf(p1.y()));

    if (p0.x() < 0.f || p0.y() < 0.f || p0.x() > m_size.width() || p0.y() > m_size.height()) {
        dbgln("!P0({},{})", p0.x(), p0.y());
        return;
    }

    if (p1.x() < 0.f || p1.y() < 0.f || p1.x() > m_size.width() || p1.y() > m_size.height()) {
        dbgln("!P1({},{})", p1.x(), p1.y());
        return;
    }

    // If we're on the same Y, there's no need to draw
    if (p0.y() == p1.y())
        return;

    float direction = -1.f;
    if (p1.y() < p0.y()) {
        direction = 1.f;
        AK::swap(p0, p1);
    }

    float const dxdy = (p1.x() - p0.x()) / (p1.y() - p0.y());
    float const dydx = AK::abs(1.f / dxdy);

    u32 y0 = floorf(p0.y());
    u32 y1 = ceilf(p1.y());
    float x_cur = p0.x();

    for (u32 y = y0; y < y1; y++) {
        u32 line_offset = m_size.width() * y;

        float dy = AK::min(y + 1.f, p1.y()) - AK::max(static_cast<float>(y), p0.y());
        float directed_dy = dy * direction;
        float x_next = x_cur + dy * dxdy;
        x_next = AK::max(x_next, 0.f);
        float x0 = x_cur;
        float x1 = x_next;
        if (x1 < x0) {
            x1 = x_cur;
            x0 = x_next;
        }
        float x0_floor = floorf(x0);
        float x1_ceil = ceilf(x1);
        u32 x0_floor_i = x0_floor;

        if (x1_ceil <= x0_floor + 1.f) {
            // If x0 and x1 are within the same pixel, then area to the right is (1 - (mid(x0, x1) - x0_floor)) * dy
            float area = .5f * (x0 + x1) - x0_floor;
            m_data[line_offset + x0_floor_i] += directed_dy * (1.f - area);
            if (x0_floor_i + 1 < static_cast<u32>(m_size.width()))
                m_data[line_offset + x0_floor_i + 1] += directed_dy * area;
        } else {
            float x0_right = 1.f - (x0 - x0_floor);
            u32 x1_floor_i = floorf(x1);
            float area_upto_here = .5f * x0_right * x0_right * dydx;
            m_data[line_offset + x0_floor_i] += direction * area_upto_here;
            for (u32 x = x0_floor_i + 1; x < x1_floor_i; x++) {
                m_data[line_offset + x] += direction * dydx;
                area_upto_here += dydx;
            }
            float remaining_area = dy - area_upto_here;
            m_data[line_offset + x1_floor_i] += direction * remaining_area;
        }

        x_cur = x_next;
    }
}

}
