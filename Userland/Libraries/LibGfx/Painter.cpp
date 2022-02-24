/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Painter.h"
#include "Bitmap.h"
#include "Emoji.h"
#include "Font.h"
#include "FontDatabase.h"
#include "Gamma.h"
#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/Math.h>
#include <AK/Memory.h>
#include <AK/Queue.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/FillPathImplementation.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>
#include <LibGfx/TextDirection.h>
#include <LibGfx/TextLayout.h>
#include <stdio.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

namespace Gfx {

template<BitmapFormat format = BitmapFormat::Invalid>
ALWAYS_INLINE Color get_pixel(Gfx::Bitmap const& bitmap, int x, int y)
{
    if constexpr (format == BitmapFormat::Indexed8)
        return bitmap.palette_color(bitmap.scanline_u8(y)[x]);
    if constexpr (format == BitmapFormat::Indexed4)
        return bitmap.palette_color(bitmap.scanline_u8(y)[x]);
    if constexpr (format == BitmapFormat::Indexed2)
        return bitmap.palette_color(bitmap.scanline_u8(y)[x]);
    if constexpr (format == BitmapFormat::Indexed1)
        return bitmap.palette_color(bitmap.scanline_u8(y)[x]);
    if constexpr (format == BitmapFormat::BGRx8888)
        return Color::from_rgb(bitmap.scanline(y)[x]);
    if constexpr (format == BitmapFormat::BGRA8888)
        return Color::from_rgba(bitmap.scanline(y)[x]);
    return bitmap.get_pixel(x, y);
}

Painter::Painter(Gfx::Bitmap& bitmap)
    : m_target(bitmap)
{
    int scale = bitmap.scale();
    VERIFY(bitmap.format() == Gfx::BitmapFormat::BGRx8888 || bitmap.format() == Gfx::BitmapFormat::BGRA8888);
    VERIFY(bitmap.physical_width() % scale == 0);
    VERIFY(bitmap.physical_height() % scale == 0);
    m_state_stack.append(State());
    state().font = nullptr;
    state().clip_rect = { { 0, 0 }, bitmap.size() };
    state().scale = scale;
    m_clip_origin = state().clip_rect;
}

Painter::~Painter()
{
}

void Painter::fill_rect_with_draw_op(IntRect const& a_rect, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            set_physical_pixel_with_draw_op(dst[j], color);
        dst += dst_skip;
    }
}

void Painter::clear_rect(IntRect const& a_rect, Color color)
{
    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    VERIFY(m_target->rect().contains(rect));
    rect *= scale();

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        fast_u32_fill(dst, color.value(), rect.width());
        dst += dst_skip;
    }
}

void Painter::fill_physical_rect(IntRect const& physical_rect, Color color)
{
    // Callers must do clipping.
    RGBA32* dst = m_target->scanline(physical_rect.top()) + physical_rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = physical_rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < physical_rect.width(); ++j)
            dst[j] = Color::from_rgba(dst[j]).blend(color).value();
        dst += dst_skip;
    }
}

void Painter::fill_rect(IntRect const& a_rect, Color color)
{
    if (color.alpha() == 0)
        return;

    if (draw_op() != DrawOp::Copy) {
        fill_rect_with_draw_op(a_rect, color);
        return;
    }

    if (color.alpha() == 0xff) {
        clear_rect(a_rect, color);
        return;
    }

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;
    VERIFY(m_target->rect().contains(rect));

    fill_physical_rect(rect * scale(), color);
}

void Painter::fill_rect_with_dither_pattern(IntRect const& a_rect, Color color_a, Color color_b)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = 0; i < rect.height(); ++i) {
        for (int j = 0; j < rect.width(); ++j) {
            bool checkboard_use_a = ((rect.left() + i) & 1) ^ ((rect.top() + j) & 1);
            if (checkboard_use_a && !color_a.alpha())
                continue;
            if (!checkboard_use_a && !color_b.alpha())
                continue;
            dst[j] = checkboard_use_a ? color_a.value() : color_b.value();
        }
        dst += dst_skip;
    }
}

void Painter::fill_rect_with_checkerboard(IntRect const& a_rect, IntSize const& cell_size, Color color_dark, Color color_light)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    int first_cell_column = rect.x() / cell_size.width();
    int prologue_length = min(rect.width(), cell_size.width() - (rect.x() % cell_size.width()));
    int number_of_aligned_strips = (rect.width() - prologue_length) / cell_size.width();

    for (int i = 0; i < rect.height(); ++i) {
        int y = rect.y() + i;
        int cell_row = y / cell_size.height();
        bool odd_row = cell_row & 1;

        // Prologue: Paint the unaligned part up to the first intersection.
        int j = 0;
        int cell_column = first_cell_column;

        {
            bool odd_cell = cell_column & 1;
            auto color = (odd_row ^ odd_cell) ? color_light.value() : color_dark.value();
            fast_u32_fill(&dst[j], color, prologue_length);
            j += prologue_length;
        }

        // Aligned run: Paint the maximum number of aligned cell strips.
        for (int strip = 0; strip < number_of_aligned_strips; ++strip) {
            ++cell_column;
            bool odd_cell = cell_column & 1;
            auto color = (odd_row ^ odd_cell) ? color_light.value() : color_dark.value();
            fast_u32_fill(&dst[j], color, cell_size.width());
            j += cell_size.width();
        }

        // Epilogue: Paint the unaligned part until the end of the rect.
        if (j != rect.width()) {
            ++cell_column;
            bool odd_cell = cell_column & 1;
            auto color = (odd_row ^ odd_cell) ? color_light.value() : color_dark.value();
            int epilogue_length = rect.width() - j;
            fast_u32_fill(&dst[j], color, epilogue_length);
            j += epilogue_length;
        }

        dst += dst_skip;
    }
}

void Painter::fill_rect_with_gradient(Orientation orientation, IntRect const& a_rect, Color gradient_start, Color gradient_end)
{
    if (gradient_start == gradient_end) {
        fill_rect(a_rect, gradient_start);
        return;
    }

    auto rect = to_physical(a_rect);
    auto clipped_rect = IntRect::intersection(rect, clip_rect() * scale());
    if (clipped_rect.is_empty())
        return;

    int offset = clipped_rect.primary_offset_for_orientation(orientation) - rect.primary_offset_for_orientation(orientation);

    RGBA32* dst = m_target->scanline(clipped_rect.top()) + clipped_rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    float increment = (1.0 / ((rect.primary_size_for_orientation(orientation))));
    float alpha_increment = increment * ((float)gradient_end.alpha() - (float)gradient_start.alpha());

    if (orientation == Orientation::Horizontal) {
        for (int i = clipped_rect.height() - 1; i >= 0; --i) {
            float c = offset * increment;
            float c_alpha = gradient_start.alpha() + offset * alpha_increment;
            for (int j = 0; j < clipped_rect.width(); ++j) {
                auto color = gamma_accurate_blend(gradient_start, gradient_end, c);
                color.set_alpha(c_alpha);
                dst[j] = color.value();
                c_alpha += alpha_increment;
                c += increment;
            }
            dst += dst_skip;
        }
    } else {
        float c = offset * increment;
        float c_alpha = gradient_start.alpha() + offset * alpha_increment;
        for (int i = clipped_rect.height() - 1; i >= 0; --i) {
            auto color = gamma_accurate_blend(gradient_end, gradient_start, c);
            color.set_alpha(c_alpha);
            for (int j = 0; j < clipped_rect.width(); ++j) {
                dst[j] = color.value();
            }
            c_alpha += alpha_increment;
            c += increment;
            dst += dst_skip;
        }
    }
}

void Painter::fill_rect_with_gradient(IntRect const& a_rect, Color gradient_start, Color gradient_end)
{
    return fill_rect_with_gradient(Orientation::Horizontal, a_rect, gradient_start, gradient_end);
}

void Painter::fill_rect_with_rounded_corners(IntRect const& a_rect, Color color, int radius)
{
    return fill_rect_with_rounded_corners(a_rect, color, radius, radius, radius, radius);
}

void Painter::fill_rect_with_rounded_corners(IntRect const& a_rect, Color color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius)
{
    // Fasttrack for rects without any border radii
    if (!top_left_radius && !top_right_radius && !bottom_right_radius && !bottom_left_radius)
        return fill_rect(a_rect, color);

    // Fully transparent, dont care.
    if (color.alpha() == 0)
        return;

    // FIXME: Allow for elliptically rounded corners
    IntRect top_left_corner = {
        a_rect.x(),
        a_rect.y(),
        top_left_radius,
        top_left_radius
    };
    IntRect top_right_corner = {
        a_rect.x() + a_rect.width() - top_right_radius,
        a_rect.y(),
        top_right_radius,
        top_right_radius
    };
    IntRect bottom_right_corner = {
        a_rect.x() + a_rect.width() - bottom_right_radius,
        a_rect.y() + a_rect.height() - bottom_right_radius,
        bottom_right_radius,
        bottom_right_radius
    };
    IntRect bottom_left_corner = {
        a_rect.x(),
        a_rect.y() + a_rect.height() - bottom_left_radius,
        bottom_left_radius,
        bottom_left_radius
    };

    IntRect top_rect = {
        a_rect.x() + top_left_radius,
        a_rect.y(),
        a_rect.width() - top_left_radius - top_right_radius, top_left_radius
    };
    IntRect right_rect = {
        a_rect.x() + a_rect.width() - top_right_radius,
        a_rect.y() + top_right_radius,
        top_right_radius,
        a_rect.height() - top_right_radius - bottom_right_radius
    };
    IntRect bottom_rect = {
        a_rect.x() + bottom_left_radius,
        a_rect.y() + a_rect.height() - bottom_right_radius,
        a_rect.width() - bottom_left_radius - bottom_right_radius,
        bottom_right_radius
    };
    IntRect left_rect = {
        a_rect.x(),
        a_rect.y() + top_left_radius,
        bottom_left_radius,
        a_rect.height() - top_left_radius - bottom_left_radius
    };

    IntRect inner = {
        left_rect.x() + left_rect.width(),
        left_rect.y(),
        a_rect.width() - left_rect.width() - right_rect.width(),
        a_rect.height() - top_rect.height() - bottom_rect.height()
    };

    fill_rect(top_rect, color);
    fill_rect(right_rect, color);
    fill_rect(bottom_rect, color);
    fill_rect(left_rect, color);

    fill_rect(inner, color);

    if (top_left_radius)
        fill_rounded_corner(top_left_corner, top_left_radius, color, CornerOrientation::TopLeft);
    if (top_right_radius)
        fill_rounded_corner(top_right_corner, top_right_radius, color, CornerOrientation::TopRight);
    if (bottom_left_radius)
        fill_rounded_corner(bottom_left_corner, bottom_left_radius, color, CornerOrientation::BottomLeft);
    if (bottom_right_radius)
        fill_rounded_corner(bottom_right_corner, bottom_right_radius, color, CornerOrientation::BottomRight);
}

void Painter::fill_rounded_corner(IntRect const& a_rect, int radius, Color color, CornerOrientation orientation)
{
    // Care about clipping
    auto translated_a_rect = a_rect.translated(translation());
    auto rect = translated_a_rect.intersected(clip_rect());

    if (rect.is_empty())
        return;
    VERIFY(m_target->rect().contains(rect));

    // We got cut on the top!
    // FIXME: Also account for clipping on the x-axis
    int clip_offset = 0;
    if (translated_a_rect.y() < rect.y())
        clip_offset = rect.y() - translated_a_rect.y();

    radius *= scale();
    rect *= scale();
    clip_offset *= scale();

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    IntPoint circle_center;
    switch (orientation) {
    case CornerOrientation::TopLeft:
        circle_center = { radius, radius + 1 };
        break;
    case CornerOrientation::TopRight:
        circle_center = { -1, radius + 1 };
        break;
    case CornerOrientation::BottomRight:
        circle_center = { -1, 0 };
        break;
    case CornerOrientation::BottomLeft:
        circle_center = { radius, 0 };
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    int radius2 = radius * radius;
    auto is_in_circle = [&](int x, int y) {
        int distance2 = (circle_center.x() - x) * (circle_center.x() - x) + (circle_center.y() - y) * (circle_center.y() - y);
        // To reflect the grid and be compatible with the draw_circle_arc_intersecting algorithm
        // add 1/2 to the radius
        return distance2 <= (radius2 + radius + 0.25);
    };

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            if (is_in_circle(j, rect.height() - i + clip_offset))
                dst[j] = Color::from_rgba(dst[j]).blend(color).value();
        dst += dst_skip;
    }
}

void Painter::draw_circle_arc_intersecting(IntRect const& a_rect, IntPoint const& center, int radius, Color color, int thickness)
{
    if (thickness <= 0)
        return;

    // Care about clipping
    auto translated_a_rect = a_rect.translated(translation());
    auto rect = translated_a_rect.intersected(clip_rect());

    if (rect.is_empty())
        return;
    VERIFY(m_target->rect().contains(rect));

    // We got cut on the top!
    // FIXME: Also account for clipping on the x-axis
    int clip_offset = 0;
    if (translated_a_rect.y() < rect.y())
        clip_offset = rect.y() - translated_a_rect.y();

    if (thickness > radius)
        thickness = radius;

    int radius2 = radius * radius;
    auto is_on_arc = [&](int x, int y) {
        int distance2 = (center.x() - x) * (center.x() - x) + (center.y() - y) * (center.y() - y);
        // Is within a circle of radius 1/2 around (x,y), so basically within the current pixel.
        // Technically this is angle-dependent and should be between 1/2 and sqrt(2)/2, but this works.
        return distance2 <= (radius2 + radius + 0.25) && distance2 >= (radius2 - radius + 0.25);
    };

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            if (is_on_arc(j, rect.height() - i + clip_offset))
                dst[j] = Color::from_rgba(dst[j]).blend(color).value();
        dst += dst_skip;
    }

    return draw_circle_arc_intersecting(a_rect, center, radius - 1, color, thickness - 1);
}

void Painter::fill_ellipse(IntRect const& a_rect, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    VERIFY(m_target->rect().contains(rect));

    for (int i = 1; i < a_rect.height(); i++) {
        double y = a_rect.height() * 0.5 - i;
        double x = a_rect.width() * sqrt(0.25 - y * y / a_rect.height() / a_rect.height());
        draw_line({ a_rect.x() + a_rect.width() / 2 - (int)x, a_rect.y() + i }, { a_rect.x() + a_rect.width() / 2 + (int)x - 1, a_rect.y() + i }, color);
    }
}

void Painter::draw_ellipse_intersecting(IntRect const& rect, Color color, int thickness)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (thickness <= 0)
        return;

    constexpr int number_samples = 100; // FIXME: dynamically work out the number of samples based upon the rect size
    double increment = M_PI / number_samples;

    auto ellipse_x = [&](double theta) -> int {
        return (AK::cos(theta) * rect.width() / AK::sqrt(2.)) + rect.center().x();
    };

    auto ellipse_y = [&](double theta) -> int {
        return (AK::sin(theta) * rect.height() / AK::sqrt(2.)) + rect.center().y();
    };

    for (auto theta = 0.0; theta < 2 * M_PI; theta += increment) {
        draw_line({ ellipse_x(theta), ellipse_y(theta) }, { ellipse_x(theta + increment), ellipse_y(theta + increment) }, color, thickness);
    }
}

template<typename RectType, typename Callback>
static void for_each_pixel_around_rect_clockwise(RectType const& rect, Callback callback)
{
    if (rect.is_empty())
        return;
    for (auto x = rect.left(); x <= rect.right(); ++x) {
        callback(x, rect.top());
    }
    for (auto y = rect.top() + 1; y <= rect.bottom(); ++y) {
        callback(rect.right(), y);
    }
    for (auto x = rect.right() - 1; x >= rect.left(); --x) {
        callback(x, rect.bottom());
    }
    for (auto y = rect.bottom() - 1; y > rect.top(); --y) {
        callback(rect.left(), y);
    }
}

void Painter::draw_focus_rect(IntRect const& rect, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (rect.is_empty())
        return;
    bool state = false;
    for_each_pixel_around_rect_clockwise(rect, [&](auto x, auto y) {
        if (state)
            set_pixel(x, y, color);
        state = !state;
    });
}

void Painter::draw_rect(IntRect const& a_rect, Color color, bool rough)
{
    IntRect rect = a_rect.translated(translation());
    auto clipped_rect = rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    int min_y = clipped_rect.top();
    int max_y = clipped_rect.bottom();
    int scale = this->scale();

    if (rect.top() >= clipped_rect.top() && rect.top() <= clipped_rect.bottom()) {
        int start_x = rough ? max(rect.x() + 1, clipped_rect.x()) : clipped_rect.x();
        int width = rough ? min(rect.width() - 2, clipped_rect.width()) : clipped_rect.width();
        for (int i = 0; i < scale; ++i)
            fill_physical_scanline_with_draw_op(rect.top() * scale + i, start_x * scale, width * scale, color);
        ++min_y;
    }
    if (rect.bottom() >= clipped_rect.top() && rect.bottom() <= clipped_rect.bottom()) {
        int start_x = rough ? max(rect.x() + 1, clipped_rect.x()) : clipped_rect.x();
        int width = rough ? min(rect.width() - 2, clipped_rect.width()) : clipped_rect.width();
        for (int i = 0; i < scale; ++i)
            fill_physical_scanline_with_draw_op(max_y * scale + i, start_x * scale, width * scale, color);
        --max_y;
    }

    bool draw_left_side = rect.left() >= clipped_rect.left();
    bool draw_right_side = rect.right() == clipped_rect.right();

    if (draw_left_side && draw_right_side) {
        // Specialized loop when drawing both sides.
        for (int y = min_y * scale; y <= max_y * scale; ++y) {
            auto* bits = m_target->scanline(y);
            for (int i = 0; i < scale; ++i)
                set_physical_pixel_with_draw_op(bits[rect.left() * scale + i], color);
            for (int i = 0; i < scale; ++i)
                set_physical_pixel_with_draw_op(bits[rect.right() * scale + i], color);
        }
    } else {
        for (int y = min_y * scale; y <= max_y * scale; ++y) {
            auto* bits = m_target->scanline(y);
            if (draw_left_side)
                for (int i = 0; i < scale; ++i)
                    set_physical_pixel_with_draw_op(bits[rect.left() * scale + i], color);
            if (draw_right_side)
                for (int i = 0; i < scale; ++i)
                    set_physical_pixel_with_draw_op(bits[rect.right() * scale + i], color);
        }
    }
}

void Painter::draw_rect_with_thickness(IntRect const& rect, Color color, int thickness)
{
    if (thickness <= 0)
        return;

    IntPoint p1 = rect.location();
    IntPoint p2 = { rect.location().x() + rect.width(), rect.location().y() };
    IntPoint p3 = { rect.location().x() + rect.width(), rect.location().y() + rect.height() };
    IntPoint p4 = { rect.location().x(), rect.location().y() + rect.height() };

    draw_line(p1, p2, color, thickness);
    draw_line(p2, p3, color, thickness);
    draw_line(p3, p4, color, thickness);
    draw_line(p4, p1, color, thickness);
}

void Painter::draw_bitmap(IntPoint const& p, CharacterBitmap const& bitmap, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = IntRect(p, bitmap.size()).translated(translation());
    auto clipped_rect = rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    int const first_row = clipped_rect.top() - rect.top();
    int const last_row = clipped_rect.bottom() - rect.top();
    int const first_column = clipped_rect.left() - rect.left();
    int const last_column = clipped_rect.right() - rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);
    char const* bitmap_row = &bitmap.bits()[first_row * bitmap.width() + first_column];
    size_t const bitmap_skip = bitmap.width();

    for (int row = first_row; row <= last_row; ++row) {
        for (int j = 0; j <= (last_column - first_column); ++j) {
            char fc = bitmap_row[j];
            if (fc == '#')
                dst[j] = color.value();
        }
        bitmap_row += bitmap_skip;
        dst += dst_skip;
    }
}

void Painter::draw_bitmap(IntPoint const& p, GlyphBitmap const& bitmap, Color color)
{
    auto dst_rect = IntRect(p, bitmap.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    int const first_row = clipped_rect.top() - dst_rect.top();
    int const last_row = clipped_rect.bottom() - dst_rect.top();
    int const first_column = clipped_rect.left() - dst_rect.left();
    int const last_column = clipped_rect.right() - dst_rect.left();

    int scale = this->scale();
    RGBA32* dst = m_target->scanline(clipped_rect.y() * scale) + clipped_rect.x() * scale;
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (scale == 1) {
        for (int row = first_row; row <= last_row; ++row) {
            for (int j = 0; j <= (last_column - first_column); ++j) {
                if (bitmap.bit_at(j + first_column, row))
                    dst[j] = Color::from_rgba(dst[j]).blend(color).value();
            }
            dst += dst_skip;
        }
    } else {
        for (int row = first_row; row <= last_row; ++row) {
            for (int j = 0; j <= (last_column - first_column); ++j) {
                if (bitmap.bit_at((j + first_column), row)) {
                    for (int iy = 0; iy < scale; ++iy)
                        for (int ix = 0; ix < scale; ++ix) {
                            auto pixel_index = j * scale + ix + iy * dst_skip;
                            dst[pixel_index] = Color::from_rgba(dst[pixel_index]).blend(color).value();
                        }
                }
            }
            dst += dst_skip * scale;
        }
    }
}

void Painter::draw_triangle(IntPoint const& a, IntPoint const& b, IntPoint const& c, Color color)
{
    IntPoint p0(to_physical(a));
    IntPoint p1(to_physical(b));
    IntPoint p2(to_physical(c));

    // sort points from top to bottom
    if (p0.y() > p1.y())
        swap(p0, p1);
    if (p0.y() > p2.y())
        swap(p0, p2);
    if (p1.y() > p2.y())
        swap(p1, p2);

    // return if top and bottom points are on same line
    if (p0.y() == p2.y())
        return;

    // return if top is below clip rect or bottom is above clip rect
    auto clip = clip_rect();
    if (p0.y() >= clip.bottom())
        return;
    if (p2.y() < clip.top())
        return;

    int rgba = color.value();

    float dx02 = (float)(p2.x() - p0.x()) / (p2.y() - p0.y());
    float x01 = p0.x();
    float x02 = p0.x();

    if (p0.y() != p1.y()) { // p0 and p1 are on different lines
        float dx01 = (float)(p1.x() - p0.x()) / (p1.y() - p0.y());

        int top = p0.y();
        if (top < clip.top()) {
            x01 += dx01 * (clip.top() - top);
            x02 += dx02 * (clip.top() - top);
            top = clip.top();
        }

        for (int y = top; y < p1.y() && y < clip.bottom(); ++y) { // XXX <=?
            int start = x01 > x02 ? max((int)x02, clip.left()) : max((int)x01, clip.left());
            int end = x01 > x02 ? min((int)x01, clip.right()) : min((int)x02, clip.right());
            auto* scanline = m_target->scanline(y);
            for (int x = start; x < end; x++) {
                scanline[x] = rgba;
            }
            x01 += dx01;
            x02 += dx02;
        }
    }

    // return if middle point and bottom point are on same line
    if (p1.y() == p2.y())
        return;

    float x12 = p1.x();
    float dx12 = (float)(p2.x() - p1.x()) / (p2.y() - p1.y());
    int top = p1.y();
    if (top < clip.top()) {
        x02 += dx02 * (clip.top() - top);
        x12 += dx12 * (clip.top() - top);
        top = clip.top();
    }

    for (int y = top; y < p2.y() && y < clip.bottom(); ++y) { // XXX <=?
        int start = x12 > x02 ? max((int)x02, clip.left()) : max((int)x12, clip.left());
        int end = x12 > x02 ? min((int)x12, clip.right()) : min((int)x02, clip.right());
        auto* scanline = m_target->scanline(y);
        for (int x = start; x < end; x++) {
            scanline[x] = rgba;
        }
        x02 += dx02;
        x12 += dx12;
    }
}

struct BlitState {
    enum AlphaState {
        NoAlpha = 0,
        SrcAlpha = 1,
        DstAlpha = 2,
        BothAlpha = SrcAlpha | DstAlpha
    };

    RGBA32 const* src;
    RGBA32* dst;
    size_t src_pitch;
    size_t dst_pitch;
    int row_count;
    int column_count;
    float opacity;
};

template<BlitState::AlphaState has_alpha>
static void do_blit_with_opacity(BlitState& state)
{
    for (int row = 0; row < state.row_count; ++row) {
        for (int x = 0; x < state.column_count; ++x) {
            Color dest_color = (has_alpha & BlitState::DstAlpha) ? Color::from_rgba(state.dst[x]) : Color::from_rgb(state.dst[x]);
            if constexpr (has_alpha & BlitState::SrcAlpha) {
                Color src_color_with_alpha = Color::from_rgba(state.src[x]);
                float pixel_opacity = src_color_with_alpha.alpha() / 255.0;
                src_color_with_alpha.set_alpha(255 * (state.opacity * pixel_opacity));
                state.dst[x] = dest_color.blend(src_color_with_alpha).value();
            } else {
                Color src_color_with_alpha = Color::from_rgb(state.src[x]);
                src_color_with_alpha.set_alpha(state.opacity * 255);
                state.dst[x] = dest_color.blend(src_color_with_alpha).value();
            }
        }
        state.dst += state.dst_pitch;
        state.src += state.src_pitch;
    }
}

void Painter::blit_with_opacity(IntPoint const& position, Gfx::Bitmap const& source, IntRect const& a_src_rect, float opacity, bool apply_alpha)
{
    VERIFY(scale() >= source.scale() && "painter doesn't support downsampling scale factors");

    if (opacity >= 1.0f && !(source.has_alpha_channel() && apply_alpha))
        return blit(position, source, a_src_rect);

    IntRect safe_src_rect = IntRect::intersection(a_src_rect, source.rect());
    if (scale() != source.scale())
        return draw_scaled_bitmap({ position, safe_src_rect.size() }, source, safe_src_rect, opacity);

    auto dst_rect = IntRect(position, safe_src_rect.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    int scale = this->scale();
    auto src_rect = a_src_rect * scale;
    clipped_rect *= scale;
    dst_rect *= scale;

    int const first_row = clipped_rect.top() - dst_rect.top();
    int const last_row = clipped_rect.bottom() - dst_rect.top();
    int const first_column = clipped_rect.left() - dst_rect.left();
    int const last_column = clipped_rect.right() - dst_rect.left();

    BlitState blit_state {
        .src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column,
        .dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x(),
        .src_pitch = source.pitch() / sizeof(RGBA32),
        .dst_pitch = m_target->pitch() / sizeof(RGBA32),
        .row_count = last_row - first_row + 1,
        .column_count = last_column - first_column + 1,
        .opacity = opacity
    };

    if (source.has_alpha_channel() && apply_alpha) {
        if (m_target->has_alpha_channel())
            do_blit_with_opacity<BlitState::BothAlpha>(blit_state);
        else
            do_blit_with_opacity<BlitState::SrcAlpha>(blit_state);
    } else {
        if (m_target->has_alpha_channel())
            do_blit_with_opacity<BlitState::DstAlpha>(blit_state);
        else
            do_blit_with_opacity<BlitState::NoAlpha>(blit_state);
    }
}

void Painter::blit_filtered(IntPoint const& position, Gfx::Bitmap const& source, IntRect const& src_rect, Function<Color(Color)> filter)
{
    VERIFY((source.scale() == 1 || source.scale() == scale()) && "blit_filtered only supports integer upsampling");

    IntRect safe_src_rect = src_rect.intersected(source.rect());
    auto dst_rect = IntRect(position, safe_src_rect.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    int scale = this->scale();
    clipped_rect *= scale;
    dst_rect *= scale;
    safe_src_rect *= source.scale();

    int const first_row = clipped_rect.top() - dst_rect.top();
    int const last_row = clipped_rect.bottom() - dst_rect.top();
    int const first_column = clipped_rect.left() - dst_rect.left();
    int const last_column = clipped_rect.right() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    int s = scale / source.scale();
    if (s == 1) {
        RGBA32 const* src = source.scanline(safe_src_rect.top() + first_row) + safe_src_rect.left() + first_column;
        size_t const src_skip = source.pitch() / sizeof(RGBA32);

        for (int row = first_row; row <= last_row; ++row) {
            for (int x = 0; x <= (last_column - first_column); ++x) {
                u8 alpha = Color::from_rgba(src[x]).alpha();
                if (alpha == 0xff) {
                    auto color = filter(Color::from_rgba(src[x]));
                    if (color.alpha() == 0xff)
                        dst[x] = color.value();
                    else
                        dst[x] = Color::from_rgba(dst[x]).blend(color).value();
                } else if (!alpha)
                    continue;
                else
                    dst[x] = Color::from_rgba(dst[x]).blend(filter(Color::from_rgba(src[x]))).value();
            }
            dst += dst_skip;
            src += src_skip;
        }
    } else {
        for (int row = first_row; row <= last_row; ++row) {
            RGBA32 const* src = source.scanline(safe_src_rect.top() + row / s) + safe_src_rect.left() + first_column / s;
            for (int x = 0; x <= (last_column - first_column); ++x) {
                u8 alpha = Color::from_rgba(src[x / s]).alpha();
                if (alpha == 0xff) {
                    auto color = filter(Color::from_rgba(src[x / s]));
                    if (color.alpha() == 0xff)
                        dst[x] = color.value();
                    else
                        dst[x] = Color::from_rgba(dst[x]).blend(color).value();
                } else if (!alpha)
                    continue;
                else
                    dst[x] = Color::from_rgba(dst[x]).blend(filter(Color::from_rgba(src[x / s]))).value();
            }
            dst += dst_skip;
        }
    }
}

void Painter::blit_brightened(IntPoint const& position, Gfx::Bitmap const& source, IntRect const& src_rect)
{
    return blit_filtered(position, source, src_rect, [](Color src) {
        return src.lightened();
    });
}

void Painter::blit_dimmed(IntPoint const& position, Gfx::Bitmap const& source, IntRect const& src_rect)
{
    return blit_filtered(position, source, src_rect, [](Color src) {
        return src.to_grayscale().lightened();
    });
}

void Painter::draw_tiled_bitmap(IntRect const& a_dst_rect, Gfx::Bitmap const& source)
{
    VERIFY((source.scale() == 1 || source.scale() == scale()) && "draw_tiled_bitmap only supports integer upsampling");

    auto dst_rect = a_dst_rect.translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    int scale = this->scale();
    clipped_rect *= scale;
    dst_rect *= scale;

    int const first_row = (clipped_rect.top() - dst_rect.top());
    int const last_row = (clipped_rect.bottom() - dst_rect.top());
    int const first_column = (clipped_rect.left() - dst_rect.left());
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (source.format() == BitmapFormat::BGRx8888 || source.format() == BitmapFormat::BGRA8888) {
        int s = scale / source.scale();
        if (s == 1) {
            int x_start = first_column + a_dst_rect.left() * scale;
            for (int row = first_row; row <= last_row; ++row) {
                RGBA32 const* sl = source.scanline((row + a_dst_rect.top() * scale) % source.physical_height());
                for (int x = x_start; x < clipped_rect.width() + x_start; ++x) {
                    dst[x - x_start] = sl[x % source.physical_width()];
                }
                dst += dst_skip;
            }
        } else {
            int x_start = first_column + a_dst_rect.left() * scale;
            for (int row = first_row; row <= last_row; ++row) {
                RGBA32 const* sl = source.scanline(((row + a_dst_rect.top() * scale) / s) % source.physical_height());
                for (int x = x_start; x < clipped_rect.width() + x_start; ++x) {
                    dst[x - x_start] = sl[(x / s) % source.physical_width()];
                }
                dst += dst_skip;
            }
        }
        return;
    }

    VERIFY_NOT_REACHED();
}

void Painter::blit_offset(IntPoint const& a_position, Gfx::Bitmap const& source, IntRect const& a_src_rect, IntPoint const& offset)
{
    auto src_rect = IntRect { a_src_rect.location() - offset, a_src_rect.size() };
    auto position = a_position;
    if (src_rect.x() < 0) {
        position.set_x(position.x() - src_rect.x());
        src_rect.set_x(0);
    }
    if (src_rect.y() < 0) {
        position.set_y(position.y() - src_rect.y());
        src_rect.set_y(0);
    }
    blit(position, source, src_rect);
}

void Painter::blit(IntPoint const& position, Gfx::Bitmap const& source, IntRect const& a_src_rect, float opacity, bool apply_alpha)
{
    VERIFY(scale() >= source.scale() && "painter doesn't support downsampling scale factors");

    if (opacity < 1.0f || (source.has_alpha_channel() && apply_alpha))
        return blit_with_opacity(position, source, a_src_rect, opacity, apply_alpha);

    auto safe_src_rect = a_src_rect.intersected(source.rect());
    if (scale() != source.scale())
        return draw_scaled_bitmap({ position, safe_src_rect.size() }, source, safe_src_rect, opacity);

    // If we get here, the Painter might have a scale factor, but the source bitmap has the same scale factor.
    // We need to transform from logical to physical coordinates, but we can just copy pixels without resampling.
    auto dst_rect = IntRect(position, safe_src_rect.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    // All computations below are in physical coordinates.
    int scale = this->scale();
    auto src_rect = a_src_rect * scale;
    clipped_rect *= scale;
    dst_rect *= scale;

    int const first_row = clipped_rect.top() - dst_rect.top();
    int const last_row = clipped_rect.bottom() - dst_rect.top();
    int const first_column = clipped_rect.left() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (source.format() == BitmapFormat::BGRx8888 || source.format() == BitmapFormat::BGRA8888) {
        RGBA32 const* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        size_t const src_skip = source.pitch() / sizeof(RGBA32);
        for (int row = first_row; row <= last_row; ++row) {
            fast_u32_copy(dst, src, clipped_rect.width());
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    if (source.format() == BitmapFormat::RGBA8888) {
        u32 const* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        size_t const src_skip = source.pitch() / sizeof(u32);
        for (int row = first_row; row <= last_row; ++row) {
            for (int i = 0; i < clipped_rect.width(); ++i) {
                u32 rgba = src[i];
                u32 bgra = (rgba & 0xff00ff00)
                    | ((rgba & 0x000000ff) << 16)
                    | ((rgba & 0x00ff0000) >> 16);
                dst[i] = bgra;
            }
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    if (Bitmap::is_indexed(source.format())) {
        u8 const* src = source.scanline_u8(src_rect.top() + first_row) + src_rect.left() + first_column;
        size_t const src_skip = source.pitch();
        for (int row = first_row; row <= last_row; ++row) {
            for (int i = 0; i < clipped_rect.width(); ++i)
                dst[i] = source.palette_color(src[i]).value();
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    VERIFY_NOT_REACHED();
}

template<bool has_alpha_channel, typename GetPixel>
ALWAYS_INLINE static void do_draw_integer_scaled_bitmap(Gfx::Bitmap& target, IntRect const& dst_rect, IntRect const& src_rect, Gfx::Bitmap const& source, int hfactor, int vfactor, GetPixel get_pixel, float opacity)
{
    bool has_opacity = opacity != 1.0f;
    for (int y = 0; y < src_rect.height(); ++y) {
        int dst_y = dst_rect.y() + y * vfactor;
        for (int x = 0; x < src_rect.width(); ++x) {
            auto src_pixel = get_pixel(source, x + src_rect.left(), y + src_rect.top());
            if (has_opacity)
                src_pixel.set_alpha(src_pixel.alpha() * opacity);
            for (int yo = 0; yo < vfactor; ++yo) {
                auto* scanline = (Color*)target.scanline(dst_y + yo);
                int dst_x = dst_rect.x() + x * hfactor;
                for (int xo = 0; xo < hfactor; ++xo) {
                    if constexpr (has_alpha_channel)
                        scanline[dst_x + xo] = scanline[dst_x + xo].blend(src_pixel);
                    else
                        scanline[dst_x + xo] = src_pixel;
                }
            }
        }
    }
}

template<bool has_alpha_channel, bool do_bilinear_blend, typename GetPixel>
ALWAYS_INLINE static void do_draw_scaled_bitmap(Gfx::Bitmap& target, IntRect const& dst_rect, IntRect const& clipped_rect, Gfx::Bitmap const& source, FloatRect const& src_rect, GetPixel get_pixel, float opacity)
{
    if constexpr (!do_bilinear_blend) {
        IntRect int_src_rect = enclosing_int_rect(src_rect);
        if (dst_rect == clipped_rect && int_src_rect == src_rect && !(dst_rect.width() % int_src_rect.width()) && !(dst_rect.height() % int_src_rect.height())) {
            int hfactor = dst_rect.width() / int_src_rect.width();
            int vfactor = dst_rect.height() / int_src_rect.height();
            if (hfactor == 2 && vfactor == 2)
                return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, int_src_rect, source, 2, 2, get_pixel, opacity);
            if (hfactor == 3 && vfactor == 3)
                return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, int_src_rect, source, 3, 3, get_pixel, opacity);
            if (hfactor == 4 && vfactor == 4)
                return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, int_src_rect, source, 4, 4, get_pixel, opacity);
            return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, int_src_rect, source, hfactor, vfactor, get_pixel, opacity);
        }
    }

    bool has_opacity = opacity != 1.0f;
    i64 shift = (i64)1 << 32;
    i64 fractional_mask = (shift - (u64)1);
    i64 half_pixel = (i64)1 << 31;
    i64 hscale = (src_rect.width() * shift) / dst_rect.width();
    i64 vscale = (src_rect.height() * shift) / dst_rect.height();
    i64 src_left = src_rect.left() * shift;
    i64 src_top = src_rect.top() * shift;

    for (int y = clipped_rect.top(); y <= clipped_rect.bottom(); ++y) {
        auto* scanline = (Color*)target.scanline(y);
        for (int x = clipped_rect.left(); x <= clipped_rect.right(); ++x) {
            auto desired_x = ((x - dst_rect.x()) * hscale + src_left);
            auto desired_y = ((y - dst_rect.y()) * vscale + src_top);

            Color src_pixel;
            if constexpr (do_bilinear_blend) {
                auto scaled_x0 = clamp((desired_x - half_pixel) >> 32, 0, src_rect.width() - 1);
                auto scaled_x1 = clamp((desired_x + half_pixel) >> 32, 0, src_rect.width() - 1);
                auto scaled_y0 = clamp((desired_y - half_pixel) >> 32, 0, src_rect.height() - 1);
                auto scaled_y1 = clamp((desired_y + half_pixel) >> 32, 0, src_rect.height() - 1);

                float x_ratio = (((desired_x + half_pixel) & fractional_mask) / (float)shift);
                float y_ratio = (((desired_y + half_pixel) & fractional_mask) / (float)shift);

                src_pixel = get_pixel(source, scaled_x0, scaled_y0).interpolate(get_pixel(source, scaled_x1, scaled_y0), x_ratio).interpolate(get_pixel(source, scaled_x0, scaled_y1).interpolate(get_pixel(source, scaled_x1, scaled_y1), x_ratio), y_ratio);
            } else {
                auto scaled_x = desired_x >> 32;
                auto scaled_y = desired_y >> 32;
                src_pixel = get_pixel(source, scaled_x, scaled_y);
            }

            if (has_opacity)
                src_pixel.set_alpha(src_pixel.alpha() * opacity);
            if constexpr (has_alpha_channel) {
                scanline[x] = scanline[x].blend(src_pixel);
            } else {
                scanline[x] = src_pixel;
            }
        }
    }
}

template<bool has_alpha_channel, typename GetPixel>
ALWAYS_INLINE static void do_draw_scaled_bitmap(Gfx::Bitmap& target, IntRect const& dst_rect, IntRect const& clipped_rect, Gfx::Bitmap const& source, FloatRect const& src_rect, GetPixel get_pixel, float opacity, Painter::ScalingMode scaling_mode)
{
    switch (scaling_mode) {
    case Painter::ScalingMode::NearestNeighbor:
        do_draw_scaled_bitmap<has_alpha_channel, false>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);
        break;
    case Painter::ScalingMode::BilinearBlend:
        do_draw_scaled_bitmap<has_alpha_channel, true>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);
        break;
    }
}

void Painter::draw_scaled_bitmap(IntRect const& a_dst_rect, Gfx::Bitmap const& source, IntRect const& a_src_rect, float opacity, ScalingMode scaling_mode)
{
    draw_scaled_bitmap(a_dst_rect, source, FloatRect { a_src_rect }, opacity, scaling_mode);
}

void Painter::draw_scaled_bitmap(IntRect const& a_dst_rect, Gfx::Bitmap const& source, FloatRect const& a_src_rect, float opacity, ScalingMode scaling_mode)
{
    IntRect int_src_rect = enclosing_int_rect(a_src_rect);
    if (scale() == source.scale() && a_src_rect == int_src_rect && a_dst_rect.size() == int_src_rect.size())
        return blit(a_dst_rect.location(), source, int_src_rect, opacity);

    auto dst_rect = to_physical(a_dst_rect);
    auto src_rect = a_src_rect * source.scale();
    auto clipped_rect = dst_rect.intersected(clip_rect() * scale());
    if (clipped_rect.is_empty())
        return;

    if (source.has_alpha_channel() || opacity != 1.0f) {
        switch (source.format()) {
        case BitmapFormat::BGRx8888:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::BGRx8888>, opacity, scaling_mode);
            break;
        case BitmapFormat::BGRA8888:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::BGRA8888>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed8>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed4:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed4>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed2:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed2>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed1:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed1>, opacity, scaling_mode);
            break;
        default:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Invalid>, opacity, scaling_mode);
            break;
        }
    } else {
        switch (source.format()) {
        case BitmapFormat::BGRx8888:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::BGRx8888>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed8>, opacity, scaling_mode);
            break;
        default:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Invalid>, opacity, scaling_mode);
            break;
        }
    }
}

FLATTEN void Painter::draw_glyph(IntPoint const& point, u32 code_point, Color color)
{
    draw_glyph(point, code_point, font(), color);
}

FLATTEN void Painter::draw_glyph(IntPoint const& point, u32 code_point, Font const& font, Color color)
{
    auto glyph = font.glyph(code_point);
    auto top_left = point + IntPoint(glyph.left_bearing(), 0);

    if (glyph.is_glyph_bitmap()) {
        draw_bitmap(top_left, glyph.glyph_bitmap(), color);
    } else {
        blit_filtered(top_left, *glyph.bitmap(), glyph.bitmap()->rect(), [color](Color pixel) -> Color {
            return pixel.multiply(color);
        });
    }
}

void Painter::draw_emoji(IntPoint const& point, Gfx::Bitmap const& emoji, Font const& font)
{
    IntRect dst_rect {
        point.x(),
        point.y(),
        font.glyph_height() * emoji.width() / emoji.height(),
        font.glyph_height()
    };
    draw_scaled_bitmap(dst_rect, emoji, emoji.rect());
}

void Painter::draw_glyph_or_emoji(IntPoint const& point, u32 code_point, Font const& font, Color color)
{
    StringBuilder builder;
    builder.append_code_point(code_point);
    auto it = Utf8View { builder.string_view() }.begin();
    return draw_glyph_or_emoji(point, it, font, color);
}

void Painter::draw_glyph_or_emoji(IntPoint const& point, Utf8CodePointIterator& it, Font const& font, Color color)
{
    // FIXME: These should live somewhere else.
    constexpr u32 text_variation_selector = 0xFE0E;
    constexpr u32 emoji_variation_selector = 0xFE0F;
    constexpr u32 regional_indicator_symbol_a = 0x1F1E6;
    constexpr u32 regional_indicator_symbol_z = 0x1F1FF;

    auto initial_it = it;
    u32 code_point = *it;
    auto next_code_point = it.peek(1);

    ScopeGuard consume_variation_selector = [&] {
        // If we advanced the iterator to consume an emoji sequence, don't look for another variation selector.
        if (initial_it != it)
            return;
        // Otherwise, discard one code point if it's a variation selector.
        auto next_code_point = it.peek(1);
        if (next_code_point == text_variation_selector || next_code_point == emoji_variation_selector)
            ++it;
    };

    auto code_point_is_regional_indicator = code_point >= regional_indicator_symbol_a && code_point <= regional_indicator_symbol_z;
    auto font_contains_glyph = font.contains_glyph(code_point);

    auto check_for_emoji = false
        // Flag emojis consist of two regional indicators.
        || code_point_is_regional_indicator
        // U+00A9 (copyright) or U+00AE (registered) are text glyphs by default,
        // keycap emojis ({#,*,0-9} U+FE0F U+20E3) start with a regular ASCII character.
        // Both cases are handled by peeking for the variation selector.
        || next_code_point == emoji_variation_selector;

    // If the font contains the glyph, and we know it's not the start of an emoji, draw a text glyph.
    if (font_contains_glyph && !check_for_emoji) {
        draw_glyph(point, code_point, font, color);
        return;
    }

    // If we didn't find a text glyph, or have an emoji variation selector or regional indicator, try to draw an emoji glyph.
    if (auto const* emoji = Emoji::emoji_for_code_point_iterator(it)) {
        draw_emoji(point, *emoji, font);
        return;
    }

    // If that failed, but we have a text glyph fallback, draw that.
    if (font_contains_glyph) {
        draw_glyph(point, code_point, font, color);
        return;
    }

    // No suitable glyph found, draw a replacement character.
    dbgln_if(EMOJI_DEBUG, "Failed to find a glyph or emoji for code_point {}", code_point);
    draw_glyph(point, 0xFFFD, font, color);
}

template<typename DrawGlyphFunction>
void draw_text_line(IntRect const& a_rect, Utf8View const& text, Font const& font, TextAlignment alignment, TextDirection direction, DrawGlyphFunction draw_glyph)
{
    auto rect = a_rect;

    switch (alignment) {
    case TextAlignment::TopLeft:
    case TextAlignment::CenterLeft:
    case TextAlignment::BottomLeft:
        break;
    case TextAlignment::TopRight:
    case TextAlignment::CenterRight:
    case TextAlignment::BottomRight:
        rect.set_x(rect.right() - font.width(text));
        break;
    case TextAlignment::Center: {
        auto shrunken_rect = rect;
        shrunken_rect.set_width(font.width(text));
        shrunken_rect.center_within(rect);
        rect = shrunken_rect;
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    if (is_vertically_centered_text_alignment(alignment)) {
        int distance_from_baseline_to_bottom = (font.glyph_height() - 1) - font.baseline();
        rect.translate_by(0, distance_from_baseline_to_bottom / 2);
    }

    auto point = rect.location();
    int space_width = font.glyph_width(' ') + font.glyph_spacing();

    if (direction == TextDirection::RTL) {
        point.translate_by(rect.width(), 0); // Start drawing from the end
        space_width = -space_width;          // Draw spaces backwards
    }

    for (auto it = text.begin(); it != text.end(); ++it) {
        auto code_point = *it;
        if (code_point == ' ') {
            point.translate_by(space_width, 0);
            continue;
        }
        IntSize glyph_size(font.glyph_or_emoji_width(code_point) + font.glyph_spacing(), font.glyph_height());
        if (direction == TextDirection::RTL)
            point.translate_by(-glyph_size.width(), 0); // If we are drawing right to left, we have to move backwards before drawing the glyph
        draw_glyph({ point, glyph_size }, it);
        if (direction == TextDirection::LTR)
            point.translate_by(glyph_size.width(), 0);
        // The callback function might have exhausted the iterator.
        if (it == text.end())
            break;
    }
}

static inline size_t draw_text_get_length(Utf8View const& text)
{
    return text.byte_length();
}

Vector<DirectionalRun> Painter::split_text_into_directional_runs(Utf8View const& text, TextDirection initial_direction)
{
    // FIXME: This is a *very* simplified version of the UNICODE BIDIRECTIONAL ALGORITHM (https://www.unicode.org/reports/tr9/), that can render most bidirectional text
    //  but also produces awkward results in a large amount of edge cases. This should probably be replaced with a fully spec compliant implementation at some point.

    // FIXME: Support HTML "dir" attribute (how?)
    u8 paragraph_embedding_level = initial_direction == TextDirection::LTR ? 0 : 1;
    Vector<u8> embedding_levels;
    embedding_levels.ensure_capacity(text.length());
    for (size_t i = 0; i < text.length(); i++)
        embedding_levels.unchecked_append(paragraph_embedding_level);

    // FIXME: Support Explicit Directional Formatting Characters

    Vector<BidirectionalClass> character_classes;
    character_classes.ensure_capacity(text.length());
    for (u32 code_point : text)
        character_classes.unchecked_append(get_char_bidi_class(code_point));

    // resolving weak types
    BidirectionalClass paragraph_class = initial_direction == TextDirection::LTR ? BidirectionalClass::STRONG_LTR : BidirectionalClass::STRONG_RTL;
    for (size_t i = 0; i < character_classes.size(); i++) {
        if (character_classes[i] != BidirectionalClass::WEAK_SEPARATORS)
            continue;
        for (ssize_t j = i - 1; j >= 0; j--) {
            auto character_class = character_classes[j];
            if (character_class != BidirectionalClass::STRONG_RTL && character_class != BidirectionalClass::STRONG_LTR)
                continue;
            character_classes[i] = character_class;
            break;
        }
        if (character_classes[i] == BidirectionalClass::WEAK_SEPARATORS)
            character_classes[i] = paragraph_class;
    }

    // resolving neutral types
    auto left_side = BidirectionalClass::NEUTRAL;
    auto sequence_length = 0;
    for (size_t i = 0; i < character_classes.size(); i++) {
        auto character_class = character_classes[i];
        if (left_side == BidirectionalClass::NEUTRAL) {
            if (character_class != BidirectionalClass::NEUTRAL)
                left_side = character_class;
            else
                character_classes[i] = paragraph_class;
            continue;
        }
        if (character_class != BidirectionalClass::NEUTRAL) {
            BidirectionalClass sequence_class;
            if (bidi_class_to_direction(left_side) == bidi_class_to_direction(character_class)) {
                sequence_class = left_side == BidirectionalClass::STRONG_RTL ? BidirectionalClass::STRONG_RTL : BidirectionalClass::STRONG_LTR;
            } else {
                sequence_class = paragraph_class;
            }
            for (auto j = 0; j < sequence_length; j++) {
                character_classes[i - j - 1] = sequence_class;
            }
            sequence_length = 0;
            left_side = character_class;
        } else {
            sequence_length++;
        }
    }
    for (auto i = 0; i < sequence_length; i++)
        character_classes[character_classes.size() - i - 1] = paragraph_class;

    // resolving implicit levels
    for (size_t i = 0; i < character_classes.size(); i++) {
        auto character_class = character_classes[i];
        if ((embedding_levels[i] % 2) == 0) {
            if (character_class == BidirectionalClass::STRONG_RTL)
                embedding_levels[i] += 1;
            else if (character_class == BidirectionalClass::WEAK_NUMBERS || character_class == BidirectionalClass::WEAK_SEPARATORS)
                embedding_levels[i] += 2;
        } else {
            if (character_class == BidirectionalClass::STRONG_LTR || character_class == BidirectionalClass::WEAK_NUMBERS || character_class == BidirectionalClass::WEAK_SEPARATORS)
                embedding_levels[i] += 1;
        }
    }

    // splitting into runs
    auto run_code_points_start = text.begin();
    auto next_code_points_slice = [&](auto length) {
        Vector<u32> run_code_points;
        run_code_points.ensure_capacity(length);
        for (size_t j = 0; j < length; ++j, ++run_code_points_start)
            run_code_points.unchecked_append(*run_code_points_start);
        return run_code_points;
    };
    Vector<DirectionalRun> runs;
    size_t start = 0;
    u8 level = embedding_levels[0];
    for (size_t i = 1; i < embedding_levels.size(); ++i) {
        if (embedding_levels[i] == level)
            continue;
        auto code_points_slice = next_code_points_slice(i - start);
        runs.append({ move(code_points_slice), level });
        start = i;
        level = embedding_levels[i];
    }
    auto code_points_slice = next_code_points_slice(embedding_levels.size() - start);
    runs.append({ move(code_points_slice), level });

    // reordering resolved levels
    // FIXME: missing special cases for trailing whitespace characters
    u8 minimum_level = 128;
    u8 maximum_level = 0;
    for (auto& run : runs) {
        minimum_level = min(minimum_level, run.embedding_level());
        maximum_level = max(minimum_level, run.embedding_level());
    }
    if ((minimum_level % 2) == 0)
        minimum_level++;
    auto runs_count = runs.size() - 1;
    while (maximum_level <= minimum_level) {
        size_t run_index = 0;
        while (run_index < runs_count) {
            while (run_index < runs_count && runs[run_index].embedding_level() < maximum_level)
                run_index++;
            auto reverse_start = run_index;
            while (run_index <= runs_count && runs[run_index].embedding_level() >= maximum_level)
                run_index++;
            auto reverse_end = run_index - 1;
            while (reverse_start < reverse_end) {
                swap(runs[reverse_start], runs[reverse_end]);
                reverse_start++;
                reverse_end--;
            }
        }
        maximum_level--;
    }

    // mirroring RTL mirror characters
    for (auto& run : runs) {
        if (run.direction() == TextDirection::LTR)
            continue;
        for (auto& code_point : run.code_points()) {
            code_point = get_mirror_char(code_point);
        }
    }

    return runs;
}

bool Painter::text_contains_bidirectional_text(Utf8View const& text, TextDirection initial_direction)
{
    for (u32 code_point : text) {
        auto char_class = get_char_bidi_class(code_point);
        if (char_class == BidirectionalClass::NEUTRAL)
            continue;
        if (bidi_class_to_direction(char_class) != initial_direction)
            return true;
    }
    return false;
}

template<typename DrawGlyphFunction>
void Painter::do_draw_text(IntRect const& rect, Utf8View const& text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping, DrawGlyphFunction draw_glyph)
{
    if (draw_text_get_length(text) == 0)
        return;

    TextLayout layout(&font, text, rect);

    static int const line_spacing = 4;
    int line_height = font.glyph_height() + line_spacing;

    auto lines = layout.lines(elision, wrapping, line_spacing);
    auto bounding_rect = layout.bounding_rect(wrapping, line_spacing);

    switch (alignment) {
    case TextAlignment::TopLeft:
        bounding_rect.set_location(rect.location());
        break;
    case TextAlignment::TopRight:
        bounding_rect.set_location({ (rect.right() + 1) - bounding_rect.width(), rect.y() });
        break;
    case TextAlignment::CenterLeft:
        bounding_rect.set_location({ rect.x(), rect.center().y() - (bounding_rect.height() / 2) });
        break;
    case TextAlignment::CenterRight:
        bounding_rect.set_location({ (rect.right() + 1) - bounding_rect.width(), rect.center().y() - (bounding_rect.height() / 2) });
        break;
    case TextAlignment::Center:
        bounding_rect.center_within(rect);
        break;
    case TextAlignment::BottomLeft:
        bounding_rect.set_location({ rect.x(), (rect.bottom() + 1) - bounding_rect.height() });
        break;
    case TextAlignment::BottomRight:
        bounding_rect.set_location({ (rect.right() + 1) - bounding_rect.width(), (rect.bottom() + 1) - bounding_rect.height() });
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    bounding_rect.intersect(rect);

    for (size_t i = 0; i < lines.size(); ++i) {
        auto line = Utf8View { lines[i] };

        IntRect line_rect { bounding_rect.x(), bounding_rect.y() + static_cast<int>(i) * line_height, bounding_rect.width(), line_height };
        line_rect.intersect(rect);

        TextDirection line_direction = get_text_direction(line);
        if (text_contains_bidirectional_text(line, line_direction)) { // Slow Path: The line contains mixed BiDi classes
            auto directional_runs = split_text_into_directional_runs(line, line_direction);
            auto current_dx = line_direction == TextDirection::LTR ? 0 : line_rect.width();
            for (auto& directional_run : directional_runs) {
                auto run_width = font.width(directional_run.text());
                if (line_direction == TextDirection::RTL)
                    current_dx -= run_width;
                auto run_rect = line_rect.translated(current_dx, 0);
                run_rect.set_width(run_width);

                // NOTE: DirectionalRun returns Utf32View which isn't
                // compatible with draw_text_line.
                StringBuilder builder;
                builder.append(directional_run.text());
                auto line_text = Utf8View { builder.string_view() };

                draw_text_line(run_rect, line_text, font, alignment, directional_run.direction(), draw_glyph);
                if (line_direction == TextDirection::LTR)
                    current_dx += run_width;
            }
        } else {
            draw_text_line(line_rect, line, font, alignment, line_direction, draw_glyph);
        }
    }
}

void Painter::draw_text(IntRect const& rect, StringView text, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    draw_text(rect, text, font(), alignment, color, elision, wrapping);
}

void Painter::draw_text(IntRect const& rect, Utf32View const& text, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    draw_text(rect, text, font(), alignment, color, elision, wrapping);
}

void Painter::draw_text(IntRect const& rect, StringView raw_text, Font const& font, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    Utf8View text { raw_text };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](IntRect const& r, Utf8CodePointIterator& it) {
        draw_glyph_or_emoji(r.location(), it, font, color);
    });
}

void Painter::draw_text(IntRect const& rect, Utf32View const& raw_text, Font const& font, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    // FIXME: UTF-32 should eventually be completely removed, but for the time
    // being some places might depend on it, so we do some internal conversion.
    StringBuilder builder;
    builder.append(raw_text);
    auto text = Utf8View { builder.string_view() };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](IntRect const& r, Utf8CodePointIterator& it) {
        draw_glyph_or_emoji(r.location(), it, font, color);
    });
}

void Painter::draw_text(Function<void(IntRect const&, Utf8CodePointIterator&)> draw_one_glyph, IntRect const& rect, Utf8View const& text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](IntRect const& r, Utf8CodePointIterator& it) {
        draw_one_glyph(r, it);
    });
}

void Painter::draw_text(Function<void(IntRect const&, Utf8CodePointIterator&)> draw_one_glyph, IntRect const& rect, StringView raw_text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    Utf8View text { raw_text };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](IntRect const& r, Utf8CodePointIterator& it) {
        draw_one_glyph(r, it);
    });
}

void Painter::draw_text(Function<void(IntRect const&, Utf8CodePointIterator&)> draw_one_glyph, IntRect const& rect, Utf32View const& raw_text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    // FIXME: UTF-32 should eventually be completely removed, but for the time
    // being some places might depend on it, so we do some internal conversion.
    StringBuilder builder;
    builder.append(raw_text);
    auto text = Utf8View { builder.string_view() };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](IntRect const& r, Utf8CodePointIterator& it) {
        draw_one_glyph(r, it);
    });
}

void Painter::set_pixel(IntPoint const& p, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto point = p;
    point.translate_by(state().translation);
    if (!clip_rect().contains(point))
        return;
    m_target->scanline(point.y())[point.x()] = color.value();
}

ALWAYS_INLINE void Painter::set_physical_pixel_with_draw_op(u32& pixel, Color const& color)
{
    // This always sets a single physical pixel, independent of scale().
    // This should only be called by routines that already handle scale.

    switch (draw_op()) {
    case DrawOp::Copy:
        pixel = color.value();
        break;
    case DrawOp::Xor:
        pixel = color.xored(Color::from_rgba(pixel)).value();
        break;
    case DrawOp::Invert:
        pixel = Color::from_rgba(pixel).inverted().value();
        break;
    }
}

ALWAYS_INLINE void Painter::fill_physical_scanline_with_draw_op(int y, int x, int width, Color const& color)
{
    // This always draws a single physical scanline, independent of scale().
    // This should only be called by routines that already handle scale.

    switch (draw_op()) {
    case DrawOp::Copy:
        fast_u32_fill(m_target->scanline(y) + x, color.value(), width);
        break;
    case DrawOp::Xor: {
        auto* pixel = m_target->scanline(y) + x;
        auto* end = pixel + width;
        while (pixel < end) {
            *pixel = Color::from_rgba(*pixel).xored(color).value();
            pixel++;
        }
        break;
    }
    case DrawOp::Invert: {
        auto* pixel = m_target->scanline(y) + x;
        auto* end = pixel + width;
        while (pixel < end) {
            *pixel = Color::from_rgba(*pixel).inverted().value();
            pixel++;
        }
        break;
    }
    }
}

void Painter::draw_physical_pixel(IntPoint const& physical_position, Color color, int thickness)
{
    // This always draws a single physical pixel, independent of scale().
    // This should only be called by routines that already handle scale
    // (including scaling thickness).
    VERIFY(draw_op() == DrawOp::Copy);

    if (thickness <= 0)
        return;

    if (thickness == 1) { // Implies scale() == 1.
        auto& pixel = m_target->scanline(physical_position.y())[physical_position.x()];
        return set_physical_pixel_with_draw_op(pixel, Color::from_rgba(pixel).blend(color));
    }

    IntRect rect { physical_position, { thickness, thickness } };
    rect.intersect(clip_rect() * scale());
    fill_physical_rect(rect, color);
}

void Painter::draw_line(IntPoint const& a_p1, IntPoint const& a_p2, Color color, int thickness, LineStyle style, Color alternate_color)
{
    if (thickness <= 0)
        return;

    if (color.alpha() == 0)
        return;

    auto clip_rect = this->clip_rect() * scale();

    auto const p1 = thickness > 1 ? a_p1.translated(-(thickness / 2), -(thickness / 2)) : a_p1;
    auto const p2 = thickness > 1 ? a_p2.translated(-(thickness / 2), -(thickness / 2)) : a_p2;

    auto point1 = to_physical(p1);
    auto point2 = to_physical(p2);
    thickness *= scale();

    auto alternate_color_is_transparent = alternate_color == Color::Transparent;

    // Special case: vertical line.
    if (point1.x() == point2.x()) {
        int const x = point1.x();
        if (x < clip_rect.left() || x > clip_rect.right())
            return;
        if (point1.y() > point2.y())
            swap(point1, point2);
        if (point1.y() > clip_rect.bottom())
            return;
        if (point2.y() < clip_rect.top())
            return;
        int min_y = max(point1.y(), clip_rect.top());
        int max_y = min(point2.y(), clip_rect.bottom());
        if (style == LineStyle::Dotted) {
            for (int y = min_y; y <= max_y; y += thickness * 2)
                draw_physical_pixel({ x, y }, color, thickness);
        } else if (style == LineStyle::Dashed) {
            for (int y = min_y; y <= max_y; y += thickness * 6) {
                draw_physical_pixel({ x, y }, color, thickness);
                draw_physical_pixel({ x, min(y + thickness, max_y) }, color, thickness);
                draw_physical_pixel({ x, min(y + thickness * 2, max_y) }, color, thickness);
                if (!alternate_color_is_transparent) {
                    draw_physical_pixel({ x, min(y + thickness * 3, max_y) }, alternate_color, thickness);
                    draw_physical_pixel({ x, min(y + thickness * 4, max_y) }, alternate_color, thickness);
                    draw_physical_pixel({ x, min(y + thickness * 5, max_y) }, alternate_color, thickness);
                }
            }
        } else {
            for (int y = min_y; y <= max_y; y += thickness)
                draw_physical_pixel({ x, y }, color, thickness);
            draw_physical_pixel({ x, max_y }, color, thickness);
        }
        return;
    }

    // Special case: horizontal line.
    if (point1.y() == point2.y()) {
        int const y = point1.y();
        if (y < clip_rect.top() || y > clip_rect.bottom())
            return;
        if (point1.x() > point2.x())
            swap(point1, point2);
        if (point1.x() > clip_rect.right())
            return;
        if (point2.x() < clip_rect.left())
            return;
        int min_x = max(point1.x(), clip_rect.left());
        int max_x = min(point2.x(), clip_rect.right());
        if (style == LineStyle::Dotted) {
            for (int x = min_x; x <= max_x; x += thickness * 2)
                draw_physical_pixel({ x, y }, color, thickness);
        } else if (style == LineStyle::Dashed) {
            for (int x = min_x; x <= max_x; x += thickness * 6) {
                draw_physical_pixel({ x, y }, color, thickness);
                draw_physical_pixel({ min(x + thickness, max_x), y }, color, thickness);
                draw_physical_pixel({ min(x + thickness * 2, max_x), y }, color, thickness);
                if (!alternate_color_is_transparent) {
                    draw_physical_pixel({ min(x + thickness * 3, max_x), y }, alternate_color, thickness);
                    draw_physical_pixel({ min(x + thickness * 4, max_x), y }, alternate_color, thickness);
                    draw_physical_pixel({ min(x + thickness * 5, max_x), y }, alternate_color, thickness);
                }
            }
        } else {
            for (int x = min_x; x <= max_x; x += thickness)
                draw_physical_pixel({ x, y }, color, thickness);
            draw_physical_pixel({ max_x, y }, color, thickness);
        }
        return;
    }

    // FIXME: Implement dotted/dashed diagonal lines.
    VERIFY(style == LineStyle::Solid);

    int const adx = abs(point2.x() - point1.x());
    int const ady = abs(point2.y() - point1.y());

    if (adx > ady) {
        if (point1.x() > point2.x())
            swap(point1, point2);
    } else {
        if (point1.y() > point2.y())
            swap(point1, point2);
    }

    // FIXME: Implement clipping below.
    int const dx = point2.x() - point1.x();
    int const dy = point2.y() - point1.y();
    int error = 0;

    if (dx > dy) {
        int const y_step = dy == 0 ? 0 : (dy > 0 ? 1 : -1);
        int const delta_error = 2 * abs(dy);
        int y = point1.y();
        for (int x = point1.x(); x <= point2.x(); ++x) {
            if (clip_rect.contains(x, y))
                draw_physical_pixel({ x, y }, color, thickness);
            error += delta_error;
            if (error >= dx) {
                y += y_step;
                error -= 2 * dx;
            }
        }
    } else {
        int const x_step = dx == 0 ? 0 : (dx > 0 ? 1 : -1);
        int const delta_error = 2 * abs(dx);
        int x = point1.x();
        for (int y = point1.y(); y <= point2.y(); ++y) {
            if (clip_rect.contains(x, y))
                draw_physical_pixel({ x, y }, color, thickness);
            error += delta_error;
            if (error >= dy) {
                x += x_step;
                error -= 2 * dy;
            }
        }
    }
}

void Painter::draw_triangle_wave(IntPoint const& a_p1, IntPoint const& a_p2, Color color, int amplitude, int thickness)
{
    // FIXME: Support more than horizontal waves
    VERIFY(a_p1.y() == a_p2.y());

    auto const p1 = thickness > 1 ? a_p1.translated(-(thickness / 2), -(thickness / 2)) : a_p1;
    auto const p2 = thickness > 1 ? a_p2.translated(-(thickness / 2), -(thickness / 2)) : a_p2;

    auto point1 = to_physical(p1);
    auto point2 = to_physical(p2);

    auto y = point1.y();

    for (int x = 0; x <= point2.x() - point1.x(); ++x) {
        auto y_offset = abs(x % (2 * amplitude) - amplitude) - amplitude;
        draw_physical_pixel({ point1.x() + x, y + y_offset }, color, thickness);
    }
}

static bool can_approximate_bezier_curve(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& control)
{
    constexpr static int tolerance = 15;

    auto p1x = 3 * control.x() - 2 * p1.x() - p2.x();
    auto p1y = 3 * control.y() - 2 * p1.y() - p2.y();
    auto p2x = 3 * control.x() - 2 * p2.x() - p1.x();
    auto p2y = 3 * control.y() - 2 * p2.y() - p1.y();

    p1x = p1x * p1x;
    p1y = p1y * p1y;
    p2x = p2x * p2x;
    p2y = p2y * p2y;

    return max(p1x, p2x) + max(p1y, p2y) <= tolerance;
}

// static
void Painter::for_each_line_segment_on_bezier_curve(FloatPoint const& control_point, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>& callback)
{
    struct SegmentDescriptor {
        FloatPoint control_point;
        FloatPoint p1;
        FloatPoint p2;
    };

    static constexpr auto split_quadratic_bezier_curve = [](FloatPoint const& original_control, FloatPoint const& p1, FloatPoint const& p2, auto& segments) {
        auto po1_midpoint = original_control + p1;
        po1_midpoint /= 2;

        auto po2_midpoint = original_control + p2;
        po2_midpoint /= 2;

        auto new_segment = po1_midpoint + po2_midpoint;
        new_segment /= 2;

        segments.enqueue({ po1_midpoint, p1, new_segment });
        segments.enqueue({ po2_midpoint, new_segment, p2 });
    };

    Queue<SegmentDescriptor> segments;
    segments.enqueue({ control_point, p1, p2 });
    while (!segments.is_empty()) {
        auto segment = segments.dequeue();

        if (can_approximate_bezier_curve(segment.p1, segment.p2, segment.control_point))
            callback(segment.p1, segment.p2);
        else
            split_quadratic_bezier_curve(segment.control_point, segment.p1, segment.p2, segments);
    }
}

void Painter::for_each_line_segment_on_bezier_curve(FloatPoint const& control_point, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>&& callback)
{
    for_each_line_segment_on_bezier_curve(control_point, p1, p2, callback);
}

void Painter::draw_quadratic_bezier_curve(IntPoint const& control_point, IntPoint const& p1, IntPoint const& p2, Color color, int thickness, LineStyle style)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (thickness <= 0)
        return;

    for_each_line_segment_on_bezier_curve(FloatPoint(control_point), FloatPoint(p1), FloatPoint(p2), [&](FloatPoint const& fp1, FloatPoint const& fp2) {
        draw_line(IntPoint(fp1.x(), fp1.y()), IntPoint(fp2.x(), fp2.y()), color, thickness, style);
    });
}

void Painter::for_each_line_segment_on_cubic_bezier_curve(FloatPoint const& control_point_0, FloatPoint const& control_point_1, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>&& callback)
{
    for_each_line_segment_on_cubic_bezier_curve(control_point_0, control_point_1, p1, p2, callback);
}

static bool can_approximate_cubic_bezier_curve(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& control_0, FloatPoint const& control_1)
{
    constexpr float tolerance = 15; // Arbitrary, seems like 10-30 produces nice results.

    auto ax = 3 * control_0.x() - 2 * p1.x() - p2.x();
    auto ay = 3 * control_0.y() - 2 * p1.y() - p2.y();
    auto bx = 3 * control_1.x() - p1.x() - 2 * p2.x();
    auto by = 3 * control_1.y() - p1.y() - 2 * p2.y();

    ax *= ax;
    ay *= ay;
    bx *= bx;
    by *= by;

    return max(ax, bx) + max(ay, by) <= tolerance;
}

// static
void Painter::for_each_line_segment_on_cubic_bezier_curve(FloatPoint const& control_point_0, FloatPoint const& control_point_1, FloatPoint const& p1, FloatPoint const& p2, Function<void(FloatPoint const&, FloatPoint const&)>& callback)
{
    struct ControlPair {
        FloatPoint control_point_0;
        FloatPoint control_point_1;
    };
    struct SegmentDescriptor {
        ControlPair control_points;
        FloatPoint p1;
        FloatPoint p2;
    };

    static constexpr auto split_cubic_bezier_curve = [](ControlPair const& original_controls, FloatPoint const& p1, FloatPoint const& p2, auto& segments) {
        Array level_1_midpoints {
            (p1 + original_controls.control_point_0) / 2,
            (original_controls.control_point_0 + original_controls.control_point_1) / 2,
            (original_controls.control_point_1 + p2) / 2,
        };
        Array level_2_midpoints {
            (level_1_midpoints[0] + level_1_midpoints[1]) / 2,
            (level_1_midpoints[1] + level_1_midpoints[2]) / 2,
        };
        auto level_3_midpoint = (level_2_midpoints[0] + level_2_midpoints[1]) / 2;

        segments.enqueue({ { level_1_midpoints[0], level_2_midpoints[0] }, p1, level_3_midpoint });
        segments.enqueue({ { level_2_midpoints[1], level_1_midpoints[2] }, level_3_midpoint, p2 });
    };

    Queue<SegmentDescriptor> segments;
    segments.enqueue({ { control_point_0, control_point_1 }, p1, p2 });
    while (!segments.is_empty()) {
        auto segment = segments.dequeue();

        if (can_approximate_cubic_bezier_curve(segment.p1, segment.p2, segment.control_points.control_point_0, segment.control_points.control_point_1))
            callback(segment.p1, segment.p2);
        else
            split_cubic_bezier_curve(segment.control_points, segment.p1, segment.p2, segments);
    }
}

void Painter::draw_cubic_bezier_curve(IntPoint const& control_point_0, IntPoint const& control_point_1, IntPoint const& p1, IntPoint const& p2, Color color, int thickness, Painter::LineStyle style)
{
    for_each_line_segment_on_cubic_bezier_curve(FloatPoint(control_point_0), FloatPoint(control_point_1), FloatPoint(p1), FloatPoint(p2), [&](FloatPoint const& fp1, FloatPoint const& fp2) {
        draw_line(IntPoint(fp1.x(), fp1.y()), IntPoint(fp2.x(), fp2.y()), color, thickness, style);
    });
}

// static
void Painter::for_each_line_segment_on_elliptical_arc(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& center, FloatPoint const radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint const&, FloatPoint const&)>& callback)
{
    if (radii.x() <= 0 || radii.y() <= 0)
        return;

    auto start = p1;
    auto end = p2;

    if (theta_delta < 0) {
        swap(start, end);
        theta_1 = theta_1 + theta_delta;
        theta_delta = fabsf(theta_delta);
    }

    auto relative_start = start - center;

    auto a = radii.x();
    auto b = radii.y();

    // The segments are at most 1 long
    auto largest_radius = max(a, b);
    double theta_step = atan(1 / (double)largest_radius);

    FloatPoint current_point = relative_start;
    FloatPoint next_point = { 0, 0 };

    auto sin_x_axis = AK::sin(x_axis_rotation);
    auto cos_x_axis = AK::cos(x_axis_rotation);
    auto rotate_point = [sin_x_axis, cos_x_axis](FloatPoint& p) {
        auto original_x = p.x();
        auto original_y = p.y();

        p.set_x(original_x * cos_x_axis - original_y * sin_x_axis);
        p.set_y(original_x * sin_x_axis + original_y * cos_x_axis);
    };

    for (double theta = theta_1; theta <= ((double)theta_1 + (double)theta_delta); theta += theta_step) {
        next_point.set_x(a * AK::cos<float>(theta));
        next_point.set_y(b * AK::sin<float>(theta));
        rotate_point(next_point);

        callback(current_point + center, next_point + center);

        current_point = next_point;
    }

    callback(current_point + center, end);
}

// static
void Painter::for_each_line_segment_on_elliptical_arc(FloatPoint const& p1, FloatPoint const& p2, FloatPoint const& center, FloatPoint const radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint const&, FloatPoint const&)>&& callback)
{
    for_each_line_segment_on_elliptical_arc(p1, p2, center, radii, x_axis_rotation, theta_1, theta_delta, callback);
}

void Painter::draw_elliptical_arc(IntPoint const& p1, IntPoint const& p2, IntPoint const& center, FloatPoint const& radii, float x_axis_rotation, float theta_1, float theta_delta, Color color, int thickness, LineStyle style)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (thickness <= 0)
        return;

    for_each_line_segment_on_elliptical_arc(FloatPoint(p1), FloatPoint(p2), FloatPoint(center), radii, x_axis_rotation, theta_1, theta_delta, [&](FloatPoint const& fp1, FloatPoint const& fp2) {
        draw_line(IntPoint(fp1.x(), fp1.y()), IntPoint(fp2.x(), fp2.y()), color, thickness, style);
    });
}

void Painter::add_clip_rect(IntRect const& rect)
{
    state().clip_rect.intersect(rect.translated(translation()));
    state().clip_rect.intersect(m_target->rect()); // FIXME: This shouldn't be necessary?
}

void Painter::clear_clip_rect()
{
    state().clip_rect = m_clip_origin;
}

PainterStateSaver::PainterStateSaver(Painter& painter)
    : m_painter(painter)
{
    m_painter.save();
}

PainterStateSaver::~PainterStateSaver()
{
    m_painter.restore();
}

void Painter::stroke_path(Path const& path, Color color, int thickness)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (thickness <= 0)
        return;

    FloatPoint cursor;

    for (auto& segment : path.segments()) {
        switch (segment.type()) {
        case Segment::Type::Invalid:
            VERIFY_NOT_REACHED();
            break;
        case Segment::Type::MoveTo:
            cursor = segment.point();
            break;
        case Segment::Type::LineTo:
            draw_line(cursor.to_type<int>(), segment.point().to_type<int>(), color, thickness);
            cursor = segment.point();
            break;
        case Segment::Type::QuadraticBezierCurveTo: {
            auto& through = static_cast<QuadraticBezierCurveSegment const&>(segment).through();
            draw_quadratic_bezier_curve(through.to_type<int>(), cursor.to_type<int>(), segment.point().to_type<int>(), color, thickness);
            cursor = segment.point();
            break;
        }
        case Segment::Type::CubicBezierCurveTo: {
            auto& curve = static_cast<CubicBezierCurveSegment const&>(segment);
            auto& through_0 = curve.through_0();
            auto& through_1 = curve.through_1();
            draw_cubic_bezier_curve(through_0.to_type<int>(), through_1.to_type<int>(), cursor.to_type<int>(), segment.point().to_type<int>(), color, thickness);
            cursor = segment.point();
            break;
        }
        case Segment::Type::EllipticalArcTo:
            auto& arc = static_cast<EllipticalArcSegment const&>(segment);
            draw_elliptical_arc(cursor.to_type<int>(), segment.point().to_type<int>(), arc.center().to_type<int>(), arc.radii(), arc.x_axis_rotation(), arc.theta_1(), arc.theta_delta(), color, thickness);
            cursor = segment.point();
            break;
        }
    }
}

void Painter::fill_path(Path const& path, Color color, WindingRule winding_rule)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.
    Detail::fill_path<Detail::FillPathMode::PlaceOnIntGrid>(*this, path, color, winding_rule);
}

void Painter::blit_disabled(IntPoint const& location, Gfx::Bitmap const& bitmap, IntRect const& rect, Palette const& palette)
{
    auto bright_color = palette.threed_highlight();
    auto dark_color = palette.threed_shadow1();
    blit_filtered(location.translated(1, 1), bitmap, rect, [&](auto) {
        return bright_color;
    });
    blit_filtered(location, bitmap, rect, [&](Color src) {
        int gray = src.to_grayscale().red();
        if (gray > 160)
            return bright_color;
        return dark_color;
    });
}

void Painter::blit_tiled(IntRect const& dst_rect, Gfx::Bitmap const& bitmap, IntRect const& rect)
{
    auto tile_width = rect.width();
    auto tile_height = rect.height();
    auto dst_right = dst_rect.right();
    auto dst_bottom = dst_rect.bottom();
    for (int tile_y = dst_rect.top(); tile_y < dst_bottom; tile_y += tile_height) {
        for (int tile_x = dst_rect.left(); tile_x < dst_right; tile_x += tile_width) {
            IntRect tile_src_rect = rect;
            auto tile_x_overflow = tile_x + tile_width - dst_right;
            if (tile_x_overflow > 0) {
                tile_src_rect.set_width(tile_width - tile_x_overflow);
            }
            auto tile_y_overflow = tile_y + tile_height - dst_bottom;
            if (tile_y_overflow > 0) {
                tile_src_rect.set_height(tile_height - tile_y_overflow);
            }
            blit(IntPoint(tile_x, tile_y), bitmap, tile_src_rect);
        }
    }
}

String parse_ampersand_string(StringView raw_text, Optional<size_t>* underline_offset)
{
    if (raw_text.is_empty())
        return String::empty();

    StringBuilder builder;

    for (size_t i = 0; i < raw_text.length(); ++i) {
        if (raw_text[i] == '&') {
            if (i != (raw_text.length() - 1) && raw_text[i + 1] == '&') {
                builder.append(raw_text[i]);
                ++i;
            } else if (underline_offset && !(*underline_offset).has_value()) {
                *underline_offset = i;
            }
            continue;
        }
        builder.append(raw_text[i]);
    }
    return builder.to_string();
}

void Gfx::Painter::draw_ui_text(Gfx::IntRect const& rect, StringView text, Gfx::Font const& font, Gfx::TextAlignment text_alignment, Gfx::Color color)
{
    Optional<size_t> underline_offset;
    auto name_to_draw = parse_ampersand_string(text, &underline_offset);

    Gfx::IntRect text_rect { 0, 0, font.width(name_to_draw), font.glyph_height() };
    text_rect.align_within(rect, text_alignment);

    draw_text(text_rect, name_to_draw, font, text_alignment, color);

    if (underline_offset.has_value()) {
        Utf8View utf8_view { name_to_draw };
        int width = 0;
        for (auto it = utf8_view.begin(); it != utf8_view.end(); ++it) {
            if (utf8_view.byte_offset_of(it) >= underline_offset.value()) {
                int y = text_rect.bottom() + 1;
                int x1 = text_rect.left() + width;
                int x2 = x1 + font.glyph_or_emoji_width(*it);
                draw_line({ x1, y }, { x2, y }, color);
                break;
            }
            width += font.glyph_or_emoji_width(*it) + font.glyph_spacing();
        }
    }
}
}
