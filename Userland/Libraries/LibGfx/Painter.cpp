/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Painter.h"
#include "Bitmap.h"
#include "Font/Emoji.h"
#include "Font/Font.h"
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
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>
#include <LibGfx/Quad.h>
#include <LibGfx/TextDirection.h>
#include <LibGfx/TextLayout.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/Emoji.h>
#include <stdio.h>

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif

namespace Gfx {

static bool should_paint_as_space(u32 code_point)
{
    return is_ascii_space(code_point) || code_point == 0xa0;
}

ALWAYS_INLINE static Color color_for_format(BitmapFormat format, ARGB32 value)
{
    switch (format) {
    case BitmapFormat::BGRA8888:
        return Color::from_argb(value);
    case BitmapFormat::BGRx8888:
        return Color::from_rgb(value);
    // FIXME: Handle other formats
    default:
        VERIFY_NOT_REACHED();
    }
}

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
        return Color::from_argb(bitmap.scanline(y)[x]);
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

void Painter::fill_rect_with_draw_op(IntRect const& a_rect, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    ARGB32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

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

    ARGB32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        fast_u32_fill(dst, color.value(), rect.width());
        dst += dst_skip;
    }
}

void Painter::fill_physical_rect(IntRect const& physical_rect, Color color)
{
    // Callers must do clipping.
    ARGB32* dst = m_target->scanline(physical_rect.top()) + physical_rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

    auto dst_format = target()->format();
    for (int i = physical_rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < physical_rect.width(); ++j)
            dst[j] = color_for_format(dst_format, dst[j]).blend(color).value();
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

void Painter::fill_rect(IntRect const& rect, PaintStyle const& paint_style)
{
    auto a_rect = rect.translated(translation());
    auto clipped_rect = a_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    a_rect *= scale();
    clipped_rect *= scale();
    auto start_offset = clipped_rect.location() - a_rect.location();
    paint_style.paint(a_rect, [&](PaintStyle::SamplerFunction sample) {
        for (int y = 0; y < clipped_rect.height(); ++y) {
            for (int x = 0; x < clipped_rect.width(); ++x) {
                IntPoint point(x, y);
                set_physical_pixel(point + clipped_rect.location(), sample(point + start_offset), true);
            }
        }
    });
}

void Painter::fill_rect_with_dither_pattern(IntRect const& a_rect, Color color_a, Color color_b)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    ARGB32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

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

void Painter::fill_rect_with_checkerboard(IntRect const& a_rect, IntSize cell_size, Color color_dark, Color color_light)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto translated_rect = a_rect.translated(translation());
    auto rect = translated_rect.intersected(clip_rect());
    if (rect.is_empty())
        return;

    ARGB32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

    int first_cell_column = (rect.x() - translated_rect.x()) / cell_size.width();
    int prologue_length = min(rect.width(), cell_size.width() - ((rect.x() - translated_rect.x()) % cell_size.width()));
    int number_of_aligned_strips = (rect.width() - prologue_length) / cell_size.width();

    for (int i = 0; i < rect.height(); ++i) {
        int y = rect.y() - translated_rect.y() + i;
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
    return fill_rect_with_linear_gradient(a_rect, Array { ColorStop { gradient_start, 0 }, ColorStop { gradient_end, 1 } }, orientation == Orientation::Horizontal ? 90.0f : 0.0f);
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

    ARGB32* dst = m_target->scanline(rect.top()) + rect.left();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

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

    auto dst_format = target()->format();
    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            if (is_in_circle(j, rect.height() - i + clip_offset))
                dst[j] = color_for_format(dst_format, dst[j]).blend(color).value();
        dst += dst_skip;
    }
}

void Painter::draw_circle_arc_intersecting(IntRect const& a_rect, IntPoint center, int radius, Color color, int thickness)
{
    if (thickness <= 0 || radius <= 0)
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

    ARGB32* dst = m_target->scanline(rect.top()) + rect.left();
    auto dst_format = target()->format();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            if (is_on_arc(j, rect.height() - i + clip_offset))
                dst[j] = color_for_format(dst_format, dst[j]).blend(color).value();
        dst += dst_skip;
    }

    return draw_circle_arc_intersecting(a_rect, center, radius - 1, color, thickness - 1);
}

// The callback will only be called for a quarter of the ellipse, the user is intended to deduce other points.
// As the coordinate space is relative to the center of the rectangle, it's simply (x, y), (x, -y), (-x, y) and (-x, -y).
static void on_each_ellipse_point(IntRect const& rect, Function<void(IntPoint)>&& callback)
{
    // Note: This is an implementation of the Midpoint Ellipse Algorithm.
    double const a = rect.width() / 2;
    double const a_square = a * a;
    double const b = rect.height() / 2;
    double const b_square = b * b;

    int x = 0;
    auto y = static_cast<int>(b);

    double dx = 2 * b_square * x;
    double dy = 2 * a_square * y;

    // For region 1:
    auto decision_parameter = b_square - a_square * b + .25 * a_square;

    while (dx < dy) {
        callback({ x, y });

        if (decision_parameter >= 0) {
            y--;
            dy -= 2 * a_square;
            decision_parameter -= dy;
        }
        x++;
        dx += 2 * b_square;
        decision_parameter += dx + b_square;
    }

    // For region 2:
    decision_parameter = b_square * ((x + 0.5) * (x + 0.5)) + a_square * ((y - 1) * (y - 1)) - a_square * b_square;

    while (y >= 0) {
        callback({ x, y });

        if (decision_parameter <= 0) {
            x++;
            dx += 2 * b_square;
            decision_parameter += dx;
        }
        y--;
        dy -= 2 * a_square;
        decision_parameter += a_square - dy;
    }
}

void Painter::fill_ellipse(IntRect const& a_rect, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    VERIFY(m_target->rect().contains(rect));

    auto const center = a_rect.center();

    on_each_ellipse_point(rect, [this, &color, center](IntPoint position) {
        IntPoint const directions[4] = { { position.x(), position.y() }, { -position.x(), position.y() }, { position.x(), -position.y() }, { -position.x(), -position.y() } };

        draw_line(center + directions[0], center + directions[1], color);
        draw_line(center + directions[2], center + directions[3], color);
    });
}

void Painter::draw_ellipse_intersecting(IntRect const& rect, Color color, int thickness)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (thickness <= 0)
        return;

    auto const center = rect.center();

    on_each_ellipse_point(rect, [this, &color, thickness, center](IntPoint position) {
        IntPoint const directions[4] = { { position.x(), position.y() }, { position.x(), -position.y() }, { -position.x(), position.y() }, { -position.x(), -position.y() } };
        for (auto const delta : directions) {
            auto const point = center + delta;
            draw_line(point, point, color, thickness);
        }
    });
}

template<typename RectType, typename Callback>
static void for_each_pixel_around_rect_clockwise(RectType const& rect, Callback callback)
{
    if (rect.is_empty())
        return;
    for (auto x = rect.left(); x < rect.right(); ++x)
        callback(x, rect.top());
    for (auto y = rect.top() + 1; y < rect.bottom(); ++y)
        callback(rect.right() - 1, y);
    for (auto x = rect.right() - 2; x >= rect.left(); --x)
        callback(x, rect.bottom() - 1);
    for (auto y = rect.bottom() - 2; y > rect.top(); --y)
        callback(rect.left(), y);
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
    int max_y = clipped_rect.bottom() - 1;
    int scale = this->scale();

    if (rect.top() >= clipped_rect.top() && rect.top() < clipped_rect.bottom()) {
        int start_x = rough ? max(rect.x() + 1, clipped_rect.x()) : clipped_rect.x();
        int width = rough ? min(rect.width() - 2, clipped_rect.width()) : clipped_rect.width();
        for (int i = 0; i < scale; ++i)
            fill_physical_scanline_with_draw_op(rect.top() * scale + i, start_x * scale, width * scale, color);
        ++min_y;
    }
    if (rect.bottom() > clipped_rect.top() && rect.bottom() <= clipped_rect.bottom()) {
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
                set_physical_pixel_with_draw_op(bits[(rect.right() - 1) * scale + i], color);
        }
    } else {
        for (int y = min_y * scale; y <= max_y * scale; ++y) {
            auto* bits = m_target->scanline(y);
            if (draw_left_side)
                for (int i = 0; i < scale; ++i)
                    set_physical_pixel_with_draw_op(bits[rect.left() * scale + i], color);
            if (draw_right_side)
                for (int i = 0; i < scale; ++i)
                    set_physical_pixel_with_draw_op(bits[(rect.right() - 1) * scale + i], color);
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

void Painter::draw_bitmap(IntPoint p, CharacterBitmap const& bitmap, Color color)
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
    ARGB32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);
    char const* bitmap_row = &bitmap.bits()[first_row * bitmap.width() + first_column];
    size_t const bitmap_skip = bitmap.width();

    for (int row = first_row; row < last_row; ++row) {
        for (int j = 0; j < (last_column - first_column); ++j) {
            char fc = bitmap_row[j];
            if (fc == '#')
                dst[j] = color.value();
        }
        bitmap_row += bitmap_skip;
        dst += dst_skip;
    }
}

void Painter::draw_bitmap(IntPoint p, GlyphBitmap const& bitmap, Color color)
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
    ARGB32* dst = m_target->scanline(clipped_rect.y() * scale) + clipped_rect.x() * scale;
    auto dst_format = target()->format();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

    if (scale == 1) {
        for (int row = first_row; row < last_row; ++row) {
            for (int j = 0; j < (last_column - first_column); ++j) {
                if (bitmap.bit_at(j + first_column, row))
                    dst[j] = color_for_format(dst_format, dst[j]).blend(color).value();
            }
            dst += dst_skip;
        }
    } else {
        for (int row = first_row; row < last_row; ++row) {
            for (int j = 0; j < (last_column - first_column); ++j) {
                if (bitmap.bit_at((j + first_column), row)) {
                    for (int iy = 0; iy < scale; ++iy)
                        for (int ix = 0; ix < scale; ++ix) {
                            auto pixel_index = j * scale + ix + iy * dst_skip;
                            dst[pixel_index] = color_for_format(dst_format, dst[pixel_index]).blend(color).value();
                        }
                }
            }
            dst += dst_skip * scale;
        }
    }
}

void Painter::draw_triangle(IntPoint offset, ReadonlySpan<IntPoint> control_points, Color color)
{
    VERIFY(control_points.size() == 3);
    draw_triangle(control_points[0] + offset, control_points[1] + offset, control_points[2] + offset, color);
}

void Painter::draw_triangle(IntPoint a, IntPoint b, IntPoint c, Color color)
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

    // return if all points are on the same line vertically
    if (p0.x() == p1.x() && p1.x() == p2.x())
        return;

    // return if top is below clip rect or bottom is above clip rect
    auto clip = clip_rect();
    if (p0.y() >= clip.bottom() - 1)
        return;
    if (p2.y() < clip.top())
        return;

    class BoundaryLine {
    private:
        IntPoint m_base {};
        IntPoint m_path {};

    public:
        BoundaryLine(IntPoint a, IntPoint b)
        {
            VERIFY(a.y() <= b.y());
            m_base = a;
            m_path = b - a;
        }

        int top_y() const { return m_base.y(); }

        int bottom_y() const { return m_base.y() + m_path.y(); }

        bool is_vertical() const { return m_path.x() == 0; }

        bool is_horizontal() const { return m_path.y() == 0; }

        bool in_y_range(int y) const { return y >= top_y() && y <= bottom_y(); }

        Optional<int> intersection_on_x(int y) const
        {
            if (!in_y_range(y))
                return {};
            if (is_horizontal())
                return {};
            if (is_vertical())
                return m_base.x();

            int y_diff = y - top_y();
            int x_d = m_path.x() * y_diff, y_d = m_path.y();

            return (x_d / y_d) + m_base.x();
        }
    };

    BoundaryLine l0(p0, p1), l1(p0, p2), l2(p1, p2);

    int rgba = color.value();

    for (int y = max(p0.y(), clip.top()); y < min(p2.y() + 1, clip.bottom()); y++) {
        Optional<int>
            x0 = l0.intersection_on_x(y),
            x1 = l1.intersection_on_x(y),
            x2 = l2.intersection_on_x(y);

        int result_a = 0, result_b = 0;

        if (x0.has_value()) {
            result_a = x0.value();
            if (x1.has_value() && ((!x2.has_value()) || (result_a != x1.value()))) {
                result_b = x1.value();
            } else {
                result_b = x2.value();
            }
        } else if (x1.has_value()) {
            result_a = x1.value();
            result_b = x2.value();
        }

        if (result_a > result_b)
            swap(result_a, result_b);

        int left_bound = result_a, right_bound = result_b;

        ARGB32* scanline = m_target->scanline(y);
        for (int x = max(left_bound, clip.left()); x <= min(right_bound, clip.right() - 1); x++)
            scanline[x] = rgba;
    }
}

struct BlitState {
    enum AlphaState {
        NoAlpha = 0,
        SrcAlpha = 1,
        DstAlpha = 2,
        BothAlpha = SrcAlpha | DstAlpha
    };

    ARGB32 const* src;
    ARGB32* dst;
    size_t src_pitch;
    size_t dst_pitch;
    int row_count;
    int column_count;
    float opacity;
    BitmapFormat src_format;
};

// FIXME: This is a hack to support blit_with_opacity() with RGBA8888 source.
//        Ideally we'd have a more generic solution that allows any source format.
static void swap_red_and_blue_channels(Color& color)
{
    u32 rgba = color.value();
    u32 bgra = (rgba & 0xff00ff00)
        | ((rgba & 0x000000ff) << 16)
        | ((rgba & 0x00ff0000) >> 16);
    color = Color::from_argb(bgra);
}

// FIXME: This function is very unoptimized.
template<BlitState::AlphaState has_alpha>
static void do_blit_with_opacity(BlitState& state)
{
    for (int row = 0; row < state.row_count; ++row) {
        for (int x = 0; x < state.column_count; ++x) {
            Color dest_color = (has_alpha & BlitState::DstAlpha) ? Color::from_argb(state.dst[x]) : Color::from_rgb(state.dst[x]);
            if constexpr (has_alpha & BlitState::SrcAlpha) {
                Color src_color_with_alpha = Color::from_argb(state.src[x]);
                if (state.src_format == BitmapFormat::RGBA8888)
                    swap_red_and_blue_channels(src_color_with_alpha);
                float pixel_opacity = src_color_with_alpha.alpha() / 255.0;
                src_color_with_alpha.set_alpha(255 * (state.opacity * pixel_opacity));
                state.dst[x] = dest_color.blend(src_color_with_alpha).value();
            } else {
                Color src_color_with_alpha = Color::from_rgb(state.src[x]);
                if (state.src_format == BitmapFormat::RGBA8888)
                    swap_red_and_blue_channels(src_color_with_alpha);
                src_color_with_alpha.set_alpha(state.opacity * 255);
                state.dst[x] = dest_color.blend(src_color_with_alpha).value();
            }
        }
        state.dst += state.dst_pitch;
        state.src += state.src_pitch;
    }
}

void Painter::blit_with_opacity(IntPoint position, Gfx::Bitmap const& source, IntRect const& a_src_rect, float opacity, bool apply_alpha)
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
        .src_pitch = source.pitch() / sizeof(ARGB32),
        .dst_pitch = m_target->pitch() / sizeof(ARGB32),
        .row_count = last_row - first_row,
        .column_count = last_column - first_column,
        .opacity = opacity,
        .src_format = source.format(),
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

void Painter::blit_filtered(IntPoint position, Gfx::Bitmap const& source, IntRect const& src_rect, Function<Color(Color)> const& filter)
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
    ARGB32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);
    auto dst_format = target()->format();
    auto src_format = source.format();

    int s = scale / source.scale();
    if (s == 1) {
        ARGB32 const* src = source.scanline(safe_src_rect.top() + first_row) + safe_src_rect.left() + first_column;
        size_t const src_skip = source.pitch() / sizeof(ARGB32);

        for (int row = first_row; row < last_row; ++row) {
            for (int x = 0; x < (last_column - first_column); ++x) {
                auto source_color = color_for_format(src_format, src[x]);
                if (source_color.alpha() == 0)
                    continue;
                auto filtered_color = filter(source_color);
                if (filtered_color.alpha() == 0xff)
                    dst[x] = filtered_color.value();
                else
                    dst[x] = color_for_format(dst_format, dst[x]).blend(filtered_color).value();
            }
            dst += dst_skip;
            src += src_skip;
        }
    } else {
        for (int row = first_row; row < last_row; ++row) {
            ARGB32 const* src = source.scanline(safe_src_rect.top() + row / s) + safe_src_rect.left() + first_column / s;
            for (int x = 0; x < (last_column - first_column); ++x) {
                auto source_color = color_for_format(src_format, src[x / s]);
                if (source_color.alpha() == 0)
                    continue;
                auto filtered_color = filter(source_color);
                if (filtered_color.alpha() == 0xff)
                    dst[x] = filtered_color.value();
                else
                    dst[x] = color_for_format(dst_format, dst[x]).blend(filtered_color).value();
            }
            dst += dst_skip;
        }
    }
}

void Painter::blit_brightened(IntPoint position, Gfx::Bitmap const& source, IntRect const& src_rect)
{
    return blit_filtered(position, source, src_rect, [](Color src) {
        return src.lightened();
    });
}

void Painter::blit_dimmed(IntPoint position, Gfx::Bitmap const& source, IntRect const& src_rect)
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

    int const first_row = clipped_rect.top() - dst_rect.top();
    int const last_row = clipped_rect.bottom() - dst_rect.top();
    int const first_column = clipped_rect.left() - dst_rect.left();
    ARGB32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

    if (source.format() == BitmapFormat::BGRx8888 || source.format() == BitmapFormat::BGRA8888) {
        int s = scale / source.scale();
        if (s == 1) {
            int x_start = first_column + a_dst_rect.left() * scale;
            for (int row = first_row; row < last_row; ++row) {
                ARGB32 const* sl = source.scanline((row + a_dst_rect.top() * scale) % source.physical_height());
                for (int x = x_start; x < clipped_rect.width() + x_start; ++x)
                    dst[x - x_start] = sl[x % source.physical_width()];
                dst += dst_skip;
            }
        } else {
            int x_start = first_column + a_dst_rect.left() * scale;
            for (int row = first_row; row < last_row; ++row) {
                ARGB32 const* sl = source.scanline(((row + a_dst_rect.top() * scale) / s) % source.physical_height());
                for (int x = x_start; x < clipped_rect.width() + x_start; ++x)
                    dst[x - x_start] = sl[(x / s) % source.physical_width()];
                dst += dst_skip;
            }
        }
        return;
    }

    VERIFY_NOT_REACHED();
}

void Painter::blit_offset(IntPoint a_position, Gfx::Bitmap const& source, IntRect const& a_src_rect, IntPoint offset)
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

void Painter::blit(IntPoint position, Gfx::Bitmap const& source, IntRect const& a_src_rect, float opacity, bool apply_alpha)
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
    ARGB32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    size_t const dst_skip = m_target->pitch() / sizeof(ARGB32);

    if (source.format() == BitmapFormat::BGRx8888 || source.format() == BitmapFormat::BGRA8888) {
        ARGB32 const* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        size_t const src_skip = source.pitch() / sizeof(ARGB32);
        for (int row = first_row; row < last_row; ++row) {
            memcpy(dst, src, sizeof(ARGB32) * clipped_rect.width());
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    if (source.format() == BitmapFormat::RGBA8888) {
        u32 const* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        size_t const src_skip = source.pitch() / sizeof(u32);
        for (int row = first_row; row < last_row; ++row) {
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

template<bool has_alpha_channel, typename GetPixel>
ALWAYS_INLINE static void do_draw_box_sampled_scaled_bitmap(Gfx::Bitmap& target, IntRect const& dst_rect, IntRect const& clipped_rect, Gfx::Bitmap const& source, FloatRect const& src_rect, GetPixel get_pixel, float opacity)
{
    float source_pixel_width = src_rect.width() / dst_rect.width();
    float source_pixel_height = src_rect.height() / dst_rect.height();
    float source_pixel_area = source_pixel_width * source_pixel_height;
    FloatRect const pixel_box = { 0.f, 0.f, 1.f, 1.f };

    for (int y = clipped_rect.top(); y < clipped_rect.bottom(); ++y) {
        auto* scanline = reinterpret_cast<Color*>(target.scanline(y));
        for (int x = clipped_rect.left(); x < clipped_rect.right(); ++x) {
            // Project the destination pixel in the source image
            FloatRect const source_box = {
                src_rect.left() + (x - dst_rect.x()) * source_pixel_width,
                src_rect.top() + (y - dst_rect.y()) * source_pixel_height,
                source_pixel_width,
                source_pixel_height,
            };
            IntRect enclosing_source_box = enclosing_int_rect(source_box).intersected(source.rect());

            // Sum the contribution of all source pixels inside the projected pixel
            float red_accumulator = 0.f;
            float green_accumulator = 0.f;
            float blue_accumulator = 0.f;
            float total_area = 0.f;
            for (int sy = enclosing_source_box.y(); sy < enclosing_source_box.bottom(); ++sy) {
                for (int sx = enclosing_source_box.x(); sx < enclosing_source_box.right(); ++sx) {
                    float area = source_box.intersected(pixel_box.translated(sx, sy)).size().area();

                    auto pixel = get_pixel(source, sx, sy);
                    area *= pixel.alpha() / 255.f;

                    red_accumulator += pixel.red() * area;
                    green_accumulator += pixel.green() * area;
                    blue_accumulator += pixel.blue() * area;
                    total_area += area;
                }
            }

            Color src_pixel = {
                round_to<u8>(min(red_accumulator / total_area, 255.f)),
                round_to<u8>(min(green_accumulator / total_area, 255.f)),
                round_to<u8>(min(blue_accumulator / total_area, 255.f)),
                round_to<u8>(min(total_area * 255.f / source_pixel_area * opacity, 255.f)),
            };

            if constexpr (has_alpha_channel)
                scanline[x] = scanline[x].blend(src_pixel);
            else
                scanline[x] = src_pixel;
        }
    }
}

template<bool has_alpha_channel, Painter::ScalingMode scaling_mode, typename GetPixel>
ALWAYS_INLINE static void do_draw_scaled_bitmap(Gfx::Bitmap& target, IntRect const& dst_rect, IntRect const& clipped_rect, Gfx::Bitmap const& source, FloatRect const& src_rect, GetPixel get_pixel, float opacity)
{
    auto int_src_rect = enclosing_int_rect(src_rect);
    auto clipped_src_rect = int_src_rect.intersected(source.rect());
    if (clipped_src_rect.is_empty())
        return;

    if constexpr (scaling_mode == Painter::ScalingMode::NearestNeighbor || scaling_mode == Painter::ScalingMode::SmoothPixels) {
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

    if constexpr (scaling_mode == Painter::ScalingMode::BoxSampling)
        return do_draw_box_sampled_scaled_bitmap<has_alpha_channel>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);

    bool has_opacity = opacity != 1.f;
    i64 shift = 1ll << 32;
    i64 fractional_mask = shift - 1;
    i64 bilinear_offset_x = (1ll << 31) * (src_rect.width() / dst_rect.width() - 1);
    i64 bilinear_offset_y = (1ll << 31) * (src_rect.height() / dst_rect.height() - 1);
    i64 hscale = src_rect.width() * shift / dst_rect.width();
    i64 vscale = src_rect.height() * shift / dst_rect.height();
    i64 src_left = src_rect.left() * shift;
    i64 src_top = src_rect.top() * shift;

    for (int y = clipped_rect.top(); y < clipped_rect.bottom(); ++y) {
        auto* scanline = reinterpret_cast<Color*>(target.scanline(y));
        auto desired_y = (y - dst_rect.y()) * vscale + src_top;

        for (int x = clipped_rect.left(); x < clipped_rect.right(); ++x) {
            auto desired_x = (x - dst_rect.x()) * hscale + src_left;

            Color src_pixel;
            if constexpr (scaling_mode == Painter::ScalingMode::BilinearBlend) {
                auto shifted_x = desired_x + bilinear_offset_x;
                auto shifted_y = desired_y + bilinear_offset_y;

                auto scaled_x0 = clamp(shifted_x >> 32, clipped_src_rect.left(), clipped_src_rect.right() - 1);
                auto scaled_x1 = clamp((shifted_x >> 32) + 1, clipped_src_rect.left(), clipped_src_rect.right() - 1);
                auto scaled_y0 = clamp(shifted_y >> 32, clipped_src_rect.top(), clipped_src_rect.bottom() - 1);
                auto scaled_y1 = clamp((shifted_y >> 32) + 1, clipped_src_rect.top(), clipped_src_rect.bottom() - 1);

                float x_ratio = (shifted_x & fractional_mask) / static_cast<float>(shift);
                float y_ratio = (shifted_y & fractional_mask) / static_cast<float>(shift);

                auto top_left = get_pixel(source, scaled_x0, scaled_y0);
                auto top_right = get_pixel(source, scaled_x1, scaled_y0);
                auto bottom_left = get_pixel(source, scaled_x0, scaled_y1);
                auto bottom_right = get_pixel(source, scaled_x1, scaled_y1);

                auto top = top_left.mixed_with(top_right, x_ratio);
                auto bottom = bottom_left.mixed_with(bottom_right, x_ratio);

                src_pixel = top.mixed_with(bottom, y_ratio);
            } else if constexpr (scaling_mode == Painter::ScalingMode::SmoothPixels) {
                auto scaled_x1 = clamp(desired_x >> 32, clipped_src_rect.left(), clipped_src_rect.right() - 1);
                auto scaled_x0 = clamp(scaled_x1 - 1, clipped_src_rect.left(), clipped_src_rect.right() - 1);
                auto scaled_y1 = clamp(desired_y >> 32, clipped_src_rect.top(), clipped_src_rect.bottom() - 1);
                auto scaled_y0 = clamp(scaled_y1 - 1, clipped_src_rect.top(), clipped_src_rect.bottom() - 1);

                float x_ratio = (desired_x & fractional_mask) / (float)shift;
                float y_ratio = (desired_y & fractional_mask) / (float)shift;

                float scaled_x_ratio = clamp(x_ratio * dst_rect.width() / (float)src_rect.width(), 0.f, 1.f);
                float scaled_y_ratio = clamp(y_ratio * dst_rect.height() / (float)src_rect.height(), 0.f, 1.f);

                auto top_left = get_pixel(source, scaled_x0, scaled_y0);
                auto top_right = get_pixel(source, scaled_x1, scaled_y0);
                auto bottom_left = get_pixel(source, scaled_x0, scaled_y1);
                auto bottom_right = get_pixel(source, scaled_x1, scaled_y1);

                auto top = top_left.mixed_with(top_right, scaled_x_ratio);
                auto bottom = bottom_left.mixed_with(bottom_right, scaled_x_ratio);

                src_pixel = top.mixed_with(bottom, scaled_y_ratio);
            } else {
                auto scaled_x = clamp(desired_x >> 32, clipped_src_rect.left(), clipped_src_rect.right() - 1);
                auto scaled_y = clamp(desired_y >> 32, clipped_src_rect.top(), clipped_src_rect.bottom() - 1);
                src_pixel = get_pixel(source, scaled_x, scaled_y);
            }

            if (has_opacity)
                src_pixel.set_alpha(src_pixel.alpha() * opacity);

            if constexpr (has_alpha_channel)
                scanline[x] = scanline[x].blend(src_pixel);
            else
                scanline[x] = src_pixel;
        }
    }
}

template<bool has_alpha_channel, typename GetPixel>
ALWAYS_INLINE static void do_draw_scaled_bitmap(Gfx::Bitmap& target, IntRect const& dst_rect, IntRect const& clipped_rect, Gfx::Bitmap const& source, FloatRect const& src_rect, GetPixel get_pixel, float opacity, Painter::ScalingMode scaling_mode)
{
    switch (scaling_mode) {
    case Painter::ScalingMode::NearestNeighbor:
        do_draw_scaled_bitmap<has_alpha_channel, Painter::ScalingMode::NearestNeighbor>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);
        break;
    case Painter::ScalingMode::SmoothPixels:
        do_draw_scaled_bitmap<has_alpha_channel, Painter::ScalingMode::SmoothPixels>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);
        break;
    case Painter::ScalingMode::BilinearBlend:
        do_draw_scaled_bitmap<has_alpha_channel, Painter::ScalingMode::BilinearBlend>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);
        break;
    case Painter::ScalingMode::BoxSampling:
        do_draw_scaled_bitmap<has_alpha_channel, Painter::ScalingMode::BoxSampling>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);
        break;
    case Painter::ScalingMode::None:
        do_draw_scaled_bitmap<has_alpha_channel, Painter::ScalingMode::None>(target, dst_rect, clipped_rect, source, src_rect, get_pixel, opacity);
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

    if (scaling_mode == ScalingMode::None) {
        IntRect clipped_draw_rect { (int)a_src_rect.location().x(), (int)a_src_rect.location().y(), a_dst_rect.size().width(), a_dst_rect.size().height() };
        return blit(a_dst_rect.location(), source, clipped_draw_rect, opacity);
    }

    auto dst_rect = to_physical(a_dst_rect);
    auto src_rect = a_src_rect * source.scale();
    auto clipped_rect = dst_rect.intersected(clip_rect() * scale());
    if (clipped_rect.is_empty())
        return;

    if (source.has_alpha_channel() || opacity != 1.0f) {
        switch (source.format()) {
        case BitmapFormat::BGRx8888:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::BGRx8888>, opacity, scaling_mode);
            break;
        case BitmapFormat::BGRA8888:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::BGRA8888>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::Indexed8>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed4:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::Indexed4>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed2:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::Indexed2>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed1:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::Indexed1>, opacity, scaling_mode);
            break;
        default:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::Invalid>, opacity, scaling_mode);
            break;
        }
    } else {
        switch (source.format()) {
        case BitmapFormat::BGRx8888:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::BGRx8888>, opacity, scaling_mode);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::Indexed8>, opacity, scaling_mode);
            break;
        default:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, Gfx::get_pixel<BitmapFormat::Invalid>, opacity, scaling_mode);
            break;
        }
    }
}

FLATTEN void Painter::draw_glyph(FloatPoint point, u32 code_point, Color color)
{
    draw_glyph(point, code_point, font(), color);
}

FLATTEN void Painter::draw_glyph(FloatPoint point, u32 code_point, Font const& font, Color color)
{
    auto top_left = point + FloatPoint(font.glyph_left_bearing(code_point), 0);
    auto glyph_position = Gfx::GlyphRasterPosition::get_nearest_fit_for(top_left);
    auto glyph = font.glyph(code_point, glyph_position.subpixel_offset);

    if (glyph.is_glyph_bitmap()) {
        draw_bitmap(top_left.to_type<int>(), glyph.glyph_bitmap(), color);
    } else if (glyph.is_color_bitmap()) {
        float scaled_width = glyph.advance();
        float ratio = static_cast<float>(glyph.bitmap()->height()) / static_cast<float>(glyph.bitmap()->width());
        float scaled_height = scaled_width * ratio;

        FloatRect rect(point.x(), point.y(), scaled_width, scaled_height);
        draw_scaled_bitmap(rect.to_rounded<int>(), *glyph.bitmap(), glyph.bitmap()->rect(), 1.0f, ScalingMode::BilinearBlend);
    } else if (color.alpha() != 255) {
        blit_filtered(glyph_position.blit_position, *glyph.bitmap(), glyph.bitmap()->rect(), [color](Color pixel) -> Color {
            return pixel.multiply(color);
        });
    } else {
        blit_filtered(glyph_position.blit_position, *glyph.bitmap(), glyph.bitmap()->rect(), [color](Color pixel) -> Color {
            return color.with_alpha(pixel.alpha());
        });
    }
}

void Painter::draw_emoji(IntPoint point, Gfx::Bitmap const& emoji, Font const& font)
{
    IntRect dst_rect {
        point.x(),
        point.y(),
        font.pixel_size_rounded_up() * emoji.width() / emoji.height(),
        font.pixel_size_rounded_up(),
    };
    draw_scaled_bitmap(dst_rect, emoji, emoji.rect());
}

void Painter::draw_glyph_or_emoji(FloatPoint point, u32 code_point, Font const& font, Color color)
{
    StringBuilder builder;
    builder.append_code_point(code_point);
    auto it = Utf8View { builder.string_view() }.begin();
    return draw_glyph_or_emoji(point, it, font, color);
}

void Painter::draw_glyph_or_emoji(FloatPoint point, Utf8CodePointIterator& it, Font const& font, Color color)
{
    u32 code_point = *it;
    auto next_code_point = it.peek(1);

    ScopeGuard consume_variation_selector = [&, initial_it = it] {
        static auto const variation_selector = Unicode::property_from_string("Variation_Selector"sv);
        if (!variation_selector.has_value())
            return;

        // If we advanced the iterator to consume an emoji sequence, don't look for another variation selector.
        if (initial_it != it)
            return;

        // Otherwise, discard one code point if it's a variation selector.
        if (next_code_point.has_value() && Unicode::code_point_has_property(*next_code_point, *variation_selector))
            ++it;
    };

    // NOTE: We don't check for emoji
    auto font_contains_glyph = font.contains_glyph(code_point);
    auto check_for_emoji = !font.has_color_bitmaps() && Unicode::could_be_start_of_emoji_sequence(it, font_contains_glyph ? Unicode::SequenceType::EmojiPresentation : Unicode::SequenceType::Any);

    // If the font contains the glyph, and we know it's not the start of an emoji, draw a text glyph.
    if (font_contains_glyph && !check_for_emoji) {
        draw_glyph(point, code_point, font, color);
        return;
    }

    // If we didn't find a text glyph, or have an emoji variation selector or regional indicator, try to draw an emoji glyph.
    if (auto const* emoji = Emoji::emoji_for_code_point_iterator(it)) {
        draw_emoji(point.to_type<int>(), *emoji, font);
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

void Painter::draw_glyph(IntPoint point, u32 code_point, Color color)
{
    draw_glyph(point.to_type<float>(), code_point, font(), color);
}

void Painter::draw_glyph(IntPoint point, u32 code_point, Font const& font, Color color)
{
    draw_glyph(point.to_type<float>(), code_point, font, color);
}

void Painter::draw_glyph_or_emoji(IntPoint point, u32 code_point, Font const& font, Color color)
{
    draw_glyph_or_emoji(point.to_type<float>(), code_point, font, color);
}

void Painter::draw_glyph_or_emoji(IntPoint point, Utf8CodePointIterator& it, Font const& font, Color color)
{
    draw_glyph_or_emoji(point.to_type<float>(), it, font, color);
}

template<typename DrawGlyphFunction>
void draw_text_line(FloatRect const& a_rect, Utf8View const& text, Font const& font, TextAlignment alignment, TextDirection direction, DrawGlyphFunction draw_glyph)
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
        rect.set_x(rect.right() - 1 - font.width(text));
        break;
    case TextAlignment::TopCenter:
    case TextAlignment::BottomCenter:
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

    auto point = rect.location();
    auto space_width = font.glyph_width(' ') + font.glyph_spacing();

    if (direction == TextDirection::RTL) {
        point.translate_by(rect.width(), 0); // Start drawing from the end
        space_width = -space_width;          // Draw spaces backwards
    }

    u32 last_code_point { 0 };
    for (auto it = text.begin(); it != text.end(); ++it) {
        auto code_point = *it;
        if (should_paint_as_space(code_point)) {
            point.translate_by(space_width, 0);
            last_code_point = code_point;
            continue;
        }

        auto kerning = font.glyphs_horizontal_kerning(last_code_point, code_point);
        if (kerning != 0.0f)
            point.translate_by(direction == TextDirection::LTR ? kerning : -kerning, 0);

        auto it_copy = it; // The callback function will advance the iterator, so create a copy for this lookup.
        FloatSize glyph_size(font.glyph_or_emoji_width(it_copy) + font.glyph_spacing(), font.pixel_size());

        if (direction == TextDirection::RTL)
            point.translate_by(-glyph_size.width(), 0); // If we are drawing right to left, we have to move backwards before drawing the glyph
        draw_glyph({ point, glyph_size }, it);
        if (direction == TextDirection::LTR)
            point.translate_by(glyph_size.width(), 0);
        // The callback function might have exhausted the iterator.
        if (it == text.end())
            break;
        last_code_point = code_point;
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
void Painter::do_draw_text(FloatRect const& rect, Utf8View const& text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping, DrawGlyphFunction draw_glyph)
{
    if (draw_text_get_length(text) == 0)
        return;

    TextLayout layout(font, text, rect);

    auto line_height = font.preferred_line_height();

    auto lines = layout.lines(elision, wrapping);
    auto bounding_rect = layout.bounding_rect(wrapping);

    bounding_rect.align_within(rect, alignment);

    for (size_t i = 0; i < lines.size(); ++i) {
        auto line = Utf8View { lines[i] };

        FloatRect line_rect { bounding_rect.x(), bounding_rect.y() + i * line_height, bounding_rect.width(), line_height };

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

void Painter::draw_text(FloatRect const& rect, StringView text, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    draw_text(rect, text, font(), alignment, color, elision, wrapping);
}

void Painter::draw_text(FloatRect const& rect, Utf32View const& text, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    draw_text(rect, text, font(), alignment, color, elision, wrapping);
}

void Painter::draw_text(FloatRect const& rect, StringView raw_text, Font const& font, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    Utf8View text { raw_text };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](FloatRect const& r, Utf8CodePointIterator& it) {
        draw_glyph_or_emoji(r.location(), it, font, color);
    });
}

void Painter::draw_text(FloatRect const& rect, Utf32View const& raw_text, Font const& font, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    // FIXME: UTF-32 should eventually be completely removed, but for the time
    // being some places might depend on it, so we do some internal conversion.
    StringBuilder builder;
    builder.append(raw_text);
    auto text = Utf8View { builder.string_view() };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](FloatRect const& r, Utf8CodePointIterator& it) {
        draw_glyph_or_emoji(r.location(), it, font, color);
    });
}

void Painter::draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)> draw_one_glyph, FloatRect const& rect, Utf8View const& text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](FloatRect const& r, Utf8CodePointIterator& it) {
        draw_one_glyph(r, it);
    });
}

void Painter::draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)> draw_one_glyph, FloatRect const& rect, StringView raw_text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    Utf8View text { raw_text };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](FloatRect const& r, Utf8CodePointIterator& it) {
        draw_one_glyph(r, it);
    });
}

void Painter::draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)> draw_one_glyph, FloatRect const& rect, Utf32View const& raw_text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    // FIXME: UTF-32 should eventually be completely removed, but for the time
    // being some places might depend on it, so we do some internal conversion.
    StringBuilder builder;
    builder.append(raw_text);
    auto text = Utf8View { builder.string_view() };
    do_draw_text(rect, text, font, alignment, elision, wrapping, [&](FloatRect const& r, Utf8CodePointIterator& it) {
        draw_one_glyph(r, it);
    });
}

void Painter::draw_text(IntRect const& rect, StringView text, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    draw_text(rect.to_type<float>(), text, font(), alignment, color, elision, wrapping);
}

void Painter::draw_text(IntRect const& rect, Utf32View const& text, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    draw_text(rect.to_type<float>(), text, font(), alignment, color, elision, wrapping);
}

void Painter::draw_text(IntRect const& rect, StringView raw_text, Font const& font, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    draw_text(rect.to_type<float>(), raw_text, font, alignment, color, elision, wrapping);
}

void Painter::draw_text(IntRect const& rect, Utf32View const& raw_text, Font const& font, TextAlignment alignment, Color color, TextElision elision, TextWrapping wrapping)
{
    return draw_text(rect.to_type<float>(), raw_text, font, alignment, color, elision, wrapping);
}

void Painter::draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)> draw_one_glyph, IntRect const& rect, Utf8View const& text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    return draw_text(move(draw_one_glyph), rect.to_type<float>(), text, font, alignment, elision, wrapping);
}

void Painter::draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)> draw_one_glyph, IntRect const& rect, StringView raw_text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    return draw_text(move(draw_one_glyph), rect.to_type<float>(), raw_text, font, alignment, elision, wrapping);
}

void Painter::draw_text(Function<void(FloatRect const&, Utf8CodePointIterator&)> draw_one_glyph, IntRect const& rect, Utf32View const& raw_text, Font const& font, TextAlignment alignment, TextElision elision, TextWrapping wrapping)
{
    return draw_text(move(draw_one_glyph), rect.to_type<float>(), raw_text, font, alignment, elision, wrapping);
}

void Painter::set_pixel(IntPoint p, Color color, bool blend)
{
    auto point = p;
    point.translate_by(state().translation);
    // Use the scale only to avoid clipping pixels set in drawing functions that handle
    // scaling and call set_pixel() -- do not scale the pixel.
    if (!clip_rect().contains(point / scale()))
        return;
    set_physical_pixel(point, color, blend);
}

void Painter::set_physical_pixel(IntPoint physical_point, Color color, bool blend)
{
    // This function should only be called after translation, clipping, etc has been handled elsewhere
    // if not use set_pixel().
    auto& dst = m_target->scanline(physical_point.y())[physical_point.x()];
    if (!blend || color.alpha() == 255)
        dst = color.value();
    else if (color.alpha())
        dst = color_for_format(target()->format(), dst).blend(color).value();
}

Optional<Color> Painter::get_pixel(IntPoint p)
{
    auto point = p;
    point.translate_by(state().translation);
    if (!clip_rect().contains(point / scale()))
        return {};
    return m_target->get_pixel(point);
}

ErrorOr<NonnullRefPtr<Bitmap>> Painter::get_region_bitmap(IntRect const& region, BitmapFormat format, Optional<IntRect&> actual_region)
{
    VERIFY(scale() == 1);
    auto bitmap_region = region.translated(state().translation).intersected(m_target->rect());
    if (actual_region.has_value())
        actual_region.value() = bitmap_region.translated(-state().translation);
    return m_target->cropped(bitmap_region, format);
}

ALWAYS_INLINE void Painter::set_physical_pixel_with_draw_op(u32& pixel, Color color)
{
    // This always sets a single physical pixel, independent of scale().
    // This should only be called by routines that already handle scale.

    switch (draw_op()) {
    case DrawOp::Copy:
        pixel = color.value();
        break;
    case DrawOp::Xor:
        pixel = color.xored(Color::from_argb(pixel)).value();
        break;
    case DrawOp::Invert:
        pixel = Color::from_argb(pixel).inverted().value();
        break;
    }
}

ALWAYS_INLINE void Painter::fill_physical_scanline_with_draw_op(int y, int x, int width, Color color)
{
    // This always draws a single physical scanline, independent of scale().
    // This should only be called by routines that already handle scale.
    auto dst_format = m_target->format();
    switch (draw_op()) {
    case DrawOp::Copy:
        fast_u32_fill(m_target->scanline(y) + x, color.value(), width);
        break;
    case DrawOp::Xor: {
        auto* pixel = m_target->scanline(y) + x;
        auto* end = pixel + width;
        while (pixel < end) {
            *pixel = color_for_format(dst_format, *pixel).xored(color).value();
            pixel++;
        }
        break;
    }
    case DrawOp::Invert: {
        auto* pixel = m_target->scanline(y) + x;
        auto* end = pixel + width;
        while (pixel < end) {
            *pixel = color_for_format(dst_format, *pixel).inverted().value();
            pixel++;
        }
        break;
    }
    }
}

void Painter::draw_physical_pixel(IntPoint physical_position, Color color, int thickness)
{
    // This always draws a single physical pixel, independent of scale().
    // This should only be called by routines that already handle scale
    // (including scaling thickness).
    VERIFY(draw_op() == DrawOp::Copy);

    if (thickness <= 0)
        return;

    if (thickness == 1) { // Implies scale() == 1.
        auto& pixel = m_target->scanline(physical_position.y())[physical_position.x()];
        return set_physical_pixel_with_draw_op(pixel, color_for_format(m_target->format(), pixel).blend(color));
    }

    IntRect rect { physical_position, { thickness, thickness } };
    rect.intersect(clip_rect() * scale());
    fill_physical_rect(rect, color);
}

void Painter::draw_line(IntPoint a_p1, IntPoint a_p2, Color color, int thickness, LineStyle style, Color alternate_color)
{
    if (clip_rect().is_empty())
        return;

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
        if (x < clip_rect.left() || x >= clip_rect.right())
            return;
        if (point1.y() > point2.y())
            swap(point1, point2);
        if (point1.y() >= clip_rect.bottom())
            return;
        if (point2.y() < clip_rect.top())
            return;
        int min_y = max(point1.y(), clip_rect.top());
        int max_y = min(point2.y(), clip_rect.bottom() - 1);
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
        if (y < clip_rect.top() || y >= clip_rect.bottom())
            return;
        if (point1.x() > point2.x())
            swap(point1, point2);
        if (point1.x() >= clip_rect.right())
            return;
        if (point2.x() < clip_rect.left())
            return;
        int min_x = max(point1.x(), clip_rect.left());
        int max_x = min(point2.x(), clip_rect.right() - 1);
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

    int const adx = abs(point2.x() - point1.x());
    int const ady = abs(point2.y() - point1.y());

    if (adx > ady) {
        if (point1.x() > point2.x())
            swap(point1, point2);
    } else {
        if (point1.y() > point2.y())
            swap(point1, point2);
    }

    int const dx = point2.x() - point1.x();
    int const dy = point2.y() - point1.y();
    int error = 0;

    size_t number_of_pixels_drawn = 0;

    auto draw_pixel_in_line = [&](int x, int y) {
        bool should_draw_line = true;
        if (style == LineStyle::Dotted && number_of_pixels_drawn % 2 == 1)
            should_draw_line = false;
        else if (style == LineStyle::Dashed && number_of_pixels_drawn % 6 >= 3)
            should_draw_line = false;

        if (should_draw_line)
            draw_physical_pixel({ x, y }, color, thickness);
        else if (!alternate_color_is_transparent)
            draw_physical_pixel({ x, y }, alternate_color, thickness);

        number_of_pixels_drawn++;
    };

    if (dx > dy) {
        int const y_step = dy == 0 ? 0 : (dy > 0 ? 1 : -1);
        int const delta_error = 2 * abs(dy);
        int y = point1.y();
        for (int x = point1.x(); x <= point2.x(); ++x) {
            if (clip_rect.contains(x, y))
                draw_pixel_in_line(x, y);
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
                draw_pixel_in_line(x, y);
            error += delta_error;
            if (error >= dy) {
                x += x_step;
                error -= 2 * dy;
            }
        }
    }
}

void Painter::draw_triangle_wave(IntPoint a_p1, IntPoint a_p2, Color color, int amplitude, int thickness)
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

static bool can_approximate_bezier_curve(FloatPoint p1, FloatPoint p2, FloatPoint control)
{
    constexpr float tolerance = 0.015f;

    auto p1x = 3 * control.x() - 2 * p1.x() - p2.x();
    auto p1y = 3 * control.y() - 2 * p1.y() - p2.y();
    auto p2x = 3 * control.x() - 2 * p2.x() - p1.x();
    auto p2y = 3 * control.y() - 2 * p2.y() - p1.y();

    p1x = p1x * p1x;
    p1y = p1y * p1y;
    p2x = p2x * p2x;
    p2y = p2y * p2y;

    auto error = max(p1x, p2x) + max(p1y, p2y);
    VERIFY(isfinite(error));

    return error <= tolerance;
}

// static
void Painter::for_each_line_segment_on_bezier_curve(FloatPoint control_point, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>& callback)
{
    struct SegmentDescriptor {
        FloatPoint control_point;
        FloatPoint p1;
        FloatPoint p2;
    };

    static constexpr auto split_quadratic_bezier_curve = [](FloatPoint original_control, FloatPoint p1, FloatPoint p2, auto& segments) {
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

void Painter::for_each_line_segment_on_bezier_curve(FloatPoint control_point, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>&& callback)
{
    for_each_line_segment_on_bezier_curve(control_point, p1, p2, callback);
}

void Painter::draw_quadratic_bezier_curve(IntPoint control_point, IntPoint p1, IntPoint p2, Color color, int thickness, LineStyle style)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (thickness <= 0)
        return;

    for_each_line_segment_on_bezier_curve(FloatPoint(control_point), FloatPoint(p1), FloatPoint(p2), [&](FloatPoint fp1, FloatPoint fp2) {
        draw_line(IntPoint(fp1.x(), fp1.y()), IntPoint(fp2.x(), fp2.y()), color, thickness, style);
    });
}

void Painter::for_each_line_segment_on_cubic_bezier_curve(FloatPoint control_point_0, FloatPoint control_point_1, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>&& callback)
{
    for_each_line_segment_on_cubic_bezier_curve(control_point_0, control_point_1, p1, p2, callback);
}

static bool can_approximate_cubic_bezier_curve(FloatPoint p1, FloatPoint p2, FloatPoint control_0, FloatPoint control_1)
{
    constexpr float tolerance = 0.015f;

    auto ax = 3 * control_0.x() - 2 * p1.x() - p2.x();
    auto ay = 3 * control_0.y() - 2 * p1.y() - p2.y();
    auto bx = 3 * control_1.x() - p1.x() - 2 * p2.x();
    auto by = 3 * control_1.y() - p1.y() - 2 * p2.y();

    ax *= ax;
    ay *= ay;
    bx *= bx;
    by *= by;

    auto error = max(ax, bx) + max(ay, by);
    VERIFY(isfinite(error));

    return error <= tolerance;
}

// static
void Painter::for_each_line_segment_on_cubic_bezier_curve(FloatPoint control_point_0, FloatPoint control_point_1, FloatPoint p1, FloatPoint p2, Function<void(FloatPoint, FloatPoint)>& callback)
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

    static constexpr auto split_cubic_bezier_curve = [](ControlPair const& original_controls, FloatPoint p1, FloatPoint p2, auto& segments) {
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

void Painter::draw_cubic_bezier_curve(IntPoint control_point_0, IntPoint control_point_1, IntPoint p1, IntPoint p2, Color color, int thickness, Painter::LineStyle style)
{
    for_each_line_segment_on_cubic_bezier_curve(FloatPoint(control_point_0), FloatPoint(control_point_1), FloatPoint(p1), FloatPoint(p2), [&](FloatPoint fp1, FloatPoint fp2) {
        draw_line(IntPoint(fp1.x(), fp1.y()), IntPoint(fp2.x(), fp2.y()), color, thickness, style);
    });
}

// static
void Painter::for_each_line_segment_on_elliptical_arc(FloatPoint p1, FloatPoint p2, FloatPoint center, FloatSize radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint, FloatPoint)>& callback)
{
    if (radii.width() <= 0 || radii.height() <= 0)
        return;

    auto start = p1;
    auto end = p2;

    bool start_swapped = false;
    if (theta_delta < 0) {
        swap(start, end);
        theta_1 = theta_1 + theta_delta;
        theta_delta = fabsf(theta_delta);
        start_swapped = true;
    }

    auto relative_start = start - center;

    auto a = radii.width();
    auto b = radii.height();

    // The segments are at most 1 long
    auto largest_radius = max(a, b);
    float theta_step = AK::atan2(1.f, (float)largest_radius);

    FloatPoint current_point = relative_start;
    FloatPoint next_point = { 0, 0 };

    float sin_x_axis, cos_x_axis;
    AK::sincos(x_axis_rotation, sin_x_axis, cos_x_axis);
    auto rotate_point = [sin_x_axis, cos_x_axis](FloatPoint& p) {
        auto original_x = p.x();
        auto original_y = p.y();

        p.set_x(original_x * cos_x_axis - original_y * sin_x_axis);
        p.set_y(original_x * sin_x_axis + original_y * cos_x_axis);
    };

    auto emit_point = [&](auto p0, auto p1) {
        // NOTE: If we swap the start/end we must swap the emitted points, so correct winding orders can be calculated.
        if (start_swapped)
            swap(p0, p1);
        callback(p0, p1);
    };

    for (float theta = theta_1; theta <= theta_1 + theta_delta; theta += theta_step) {
        float s, c;
        AK::sincos(theta, s, c);
        next_point.set_x(a * c);
        next_point.set_y(b * s);
        rotate_point(next_point);

        emit_point(current_point + center, next_point + center);

        current_point = next_point;
    }

    emit_point(current_point + center, end);
}

// static
void Painter::for_each_line_segment_on_elliptical_arc(FloatPoint p1, FloatPoint p2, FloatPoint center, FloatSize radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(FloatPoint, FloatPoint)>&& callback)
{
    for_each_line_segment_on_elliptical_arc(p1, p2, center, radii, x_axis_rotation, theta_1, theta_delta, callback);
}

void Painter::draw_elliptical_arc(IntPoint p1, IntPoint p2, IntPoint center, FloatSize radii, float x_axis_rotation, float theta_1, float theta_delta, Color color, int thickness, LineStyle style)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    if (thickness <= 0)
        return;

    for_each_line_segment_on_elliptical_arc(FloatPoint(p1), FloatPoint(p2), FloatPoint(center), radii, x_axis_rotation, theta_1, theta_delta, [&](FloatPoint fp1, FloatPoint fp2) {
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
    if (thickness <= 0)
        return;
    fill_path(path.stroke_to_fill(thickness), color);
}

void Painter::blit_disabled(IntPoint location, Gfx::Bitmap const& bitmap, IntRect const& rect, Palette const& palette)
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
    auto dst_right = dst_rect.right() - 1;
    auto dst_bottom = dst_rect.bottom() - 1;
    for (int tile_y = dst_rect.top(); tile_y < dst_bottom; tile_y += tile_height) {
        for (int tile_x = dst_rect.left(); tile_x < dst_right; tile_x += tile_width) {
            IntRect tile_src_rect = rect;
            auto tile_x_overflow = tile_x + tile_width - dst_right;
            if (tile_x_overflow > 0)
                tile_src_rect.set_width(tile_width - tile_x_overflow);
            auto tile_y_overflow = tile_y + tile_height - dst_bottom;
            if (tile_y_overflow > 0)
                tile_src_rect.set_height(tile_height - tile_y_overflow);
            blit(IntPoint(tile_x, tile_y), bitmap, tile_src_rect);
        }
    }
}

DeprecatedString parse_ampersand_string(StringView raw_text, Optional<size_t>* underline_offset)
{
    if (raw_text.is_empty())
        return DeprecatedString::empty();

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
    return builder.to_deprecated_string();
}

void Gfx::Painter::draw_ui_text(Gfx::IntRect const& rect, StringView text, Gfx::Font const& font, Gfx::TextAlignment text_alignment, Gfx::Color color)
{
    Optional<size_t> underline_offset;
    auto name_to_draw = parse_ampersand_string(text, &underline_offset);

    Gfx::IntRect text_rect { 0, 0, font.width_rounded_up(name_to_draw), font.pixel_size_rounded_up() };
    text_rect.align_within(rect, text_alignment);

    draw_text(text_rect, name_to_draw, font, text_alignment, color);

    if (underline_offset.has_value()) {
        Utf8View utf8_view { name_to_draw };
        float width = 0;
        for (auto it = utf8_view.begin(); it != utf8_view.end(); ++it) {
            if (utf8_view.byte_offset_of(it) >= underline_offset.value()) {
                int y = text_rect.bottom();
                int x1 = text_rect.left() + width;
                int x2 = x1 + font.glyph_or_emoji_width(it);
                draw_line({ x1, y }, { x2, y }, color);
                break;
            }
            width += font.glyph_or_emoji_width(it) + font.glyph_spacing();
        }
    }
}

void Painter::draw_text_run(IntPoint baseline_start, Utf8View const& string, Font const& font, Color color)
{
    draw_text_run(baseline_start.to_type<float>(), string, font, color);
}

void Painter::draw_text_run(FloatPoint baseline_start, Utf8View const& string, Font const& font, Color color)
{
    float space_width = font.glyph_width(' ') + font.glyph_spacing();

    u32 last_code_point = 0;

    auto point = baseline_start;
    point.translate_by(0, -font.pixel_metrics().ascent);

    for (auto code_point_iterator = string.begin(); code_point_iterator != string.end(); ++code_point_iterator) {
        auto code_point = *code_point_iterator;
        if (should_paint_as_space(code_point)) {
            point.translate_by(space_width, 0);
            last_code_point = code_point;
            continue;
        }

        auto kerning = font.glyphs_horizontal_kerning(last_code_point, code_point);
        if (kerning != 0.0f)
            point.translate_by(kerning, 0);

        auto it = code_point_iterator; // The callback function will advance the iterator, so create a copy for this lookup.
        auto glyph_width = font.glyph_or_emoji_width(it) + font.glyph_spacing();

        draw_glyph_or_emoji(point, code_point_iterator, font, color);

        point.translate_by(glyph_width, 0);
        last_code_point = code_point;
    }
}

void Painter::draw_scaled_bitmap_with_transform(IntRect const& dst_rect, Bitmap const& bitmap, FloatRect const& src_rect, AffineTransform const& transform, float opacity, Painter::ScalingMode scaling_mode)
{
    if (transform.is_identity_or_translation()) {
        translate(transform.e(), transform.f());
        draw_scaled_bitmap(dst_rect, bitmap, src_rect, opacity, scaling_mode);
        translate(-transform.e(), -transform.f());
    } else {
        // The painter has an affine transform, we have to draw through it!

        // FIXME: This is kinda inefficient.
        // What we currently do, roughly:
        // - Map the destination rect through the context's transform.
        // - Compute the bounding rect of the destination quad.
        // - For each point in the clipped bounding rect, reverse-map it to a point in the source image.
        //   - Sample the source image at the computed point.
        //   - Set or blend (depending on alpha values) one pixel in the canvas.
        //   - Loop.

        // FIXME: Painter should have an affine transform as part of its state and handle all of this instead.

        if (opacity == 0.0f)
            return;

        auto inverse_transform = transform.inverse();
        if (!inverse_transform.has_value())
            return;

        auto destination_quad = transform.map_to_quad(dst_rect.to_type<float>());
        auto destination_bounding_rect = destination_quad.bounding_rect().to_rounded<int>();
        auto source_rect = enclosing_int_rect(src_rect).intersected(bitmap.rect());

        Gfx::AffineTransform source_transform;
        source_transform.translate(src_rect.x(), src_rect.y());
        source_transform.scale(src_rect.width() / dst_rect.width(), src_rect.height() / dst_rect.height());
        source_transform.translate(-dst_rect.x(), -dst_rect.y());

        auto translated_dest_rect = destination_bounding_rect.translated(translation());
        auto clipped_bounding_rect = translated_dest_rect.intersected(clip_rect());
        if (clipped_bounding_rect.is_empty())
            return;

        auto sample_transform = source_transform.multiply(*inverse_transform);
        auto start_offset = destination_bounding_rect.location() + (clipped_bounding_rect.location() - translated_dest_rect.location());
        for (int y = 0; y < clipped_bounding_rect.height(); ++y) {
            for (int x = 0; x < clipped_bounding_rect.width(); ++x) {
                auto point = Gfx::IntPoint { x, y };
                auto sample_point = point + start_offset;
                auto source_point = sample_transform.map(sample_point).to_rounded<int>();
                if (!source_rect.contains(source_point))
                    continue;
                auto source_color = bitmap.get_pixel(source_point);
                if (source_color.alpha() == 0)
                    continue;
                if (opacity != 1.0f)
                    source_color = source_color.with_opacity(opacity);
                set_physical_pixel(point + clipped_bounding_rect.location(), source_color, true);
            }
        }
    }
}

void Painter::draw_signed_distance_field(IntRect const& dst_rect, Color color, Gfx::GrayscaleBitmap const& sdf, float smoothing)
{
    auto target_rect = dst_rect.translated(translation());
    auto clipped_rect = target_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    target_rect *= scale();
    clipped_rect *= scale();
    auto start_offset = clipped_rect.location() - target_rect.location();
    auto x_ratio = static_cast<float>(sdf.width() - 1) / (dst_rect.width() - 1);
    auto y_ratio = static_cast<float>(sdf.height() - 1) / (dst_rect.height() - 1);

    auto smooth_step = [](auto edge0, auto edge1, auto x) {
        x = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return x * x * (3 - 2 * x);
    };

    auto pixel_at = [&](unsigned x, unsigned y) -> u8 {
        // Returning 255 means this pixel is outside the shape.
        if (x >= sdf.width() || y >= sdf.height())
            return 255;
        return sdf.pixel_at(x, y);
    };

    for (int i = 0; i < clipped_rect.height(); ++i) {
        for (int j = 0; j < clipped_rect.width(); ++j) {
            auto point = IntPoint { j, i };
            auto sample_point = point + start_offset;
            auto target_x = static_cast<int>(x_ratio * sample_point.x());
            auto target_y = static_cast<int>(y_ratio * sample_point.y());
            auto target_fraction_x = (x_ratio * sample_point.x()) - target_x;
            auto target_fraction_y = (y_ratio * sample_point.y()) - target_y;

            auto a = pixel_at(target_x, target_y);
            auto b = pixel_at(target_x + 1, target_y);
            auto c = pixel_at(target_x, target_y + 1);
            auto d = pixel_at(target_x + 1, target_y + 1);

            float distance = (a * (1 - target_fraction_x) * (1 - target_fraction_y)
                                 + b * target_fraction_x * (1 - target_fraction_y)
                                 + c * (1 - target_fraction_x) * target_fraction_y
                                 + d * target_fraction_x * target_fraction_y)
                / 255.0f;

            u8 alpha = (1 - clamp(smooth_step(0.5f - smoothing, 0.5f + smoothing, distance), 0.0f, 1.0f)) * 255;
            set_physical_pixel(point + clipped_rect.location(), color.with_alpha(alpha), true);
        }
    }
}

}
