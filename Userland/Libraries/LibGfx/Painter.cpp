/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
#include <AK/Memory.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>
#include <math.h>
#include <stdio.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

namespace Gfx {

template<BitmapFormat format = BitmapFormat::Invalid>
ALWAYS_INLINE Color get_pixel(const Gfx::Bitmap& bitmap, int x, int y)
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
    state().font = &FontDatabase::default_font();
    state().clip_rect = { { 0, 0 }, bitmap.size() };
    state().scale = scale;
    m_clip_origin = state().clip_rect;
}

Painter::~Painter()
{
}

void Painter::fill_rect_with_draw_op(const IntRect& a_rect, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            set_physical_pixel_with_draw_op(dst[j], color);
        dst += dst_skip;
    }
}

void Painter::clear_rect(const IntRect& a_rect, Color color)
{
    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    VERIFY(m_target->rect().contains(rect));
    rect *= scale();

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        fast_u32_fill(dst, color.value(), rect.width());
        dst += dst_skip;
    }
}

void Painter::fill_physical_rect(const IntRect& physical_rect, Color color)
{
    // Callers must do clipping.
    RGBA32* dst = m_target->scanline(physical_rect.top()) + physical_rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = physical_rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < physical_rect.width(); ++j)
            dst[j] = Color::from_rgba(dst[j]).blend(color).value();
        dst += dst_skip;
    }
}

void Painter::fill_rect(const IntRect& a_rect, Color color)
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

void Painter::fill_rect_with_dither_pattern(const IntRect& a_rect, Color color_a, Color color_b)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = 0; i < rect.height(); ++i) {
        for (int j = 0; j < rect.width(); ++j) {
            bool checkboard_use_a = (i & 1) ^ (j & 1);
            if (checkboard_use_a && !color_a.alpha())
                continue;
            if (!checkboard_use_a && !color_b.alpha())
                continue;
            dst[j] = checkboard_use_a ? color_a.value() : color_b.value();
        }
        dst += dst_skip;
    }
}

void Painter::fill_rect_with_checkerboard(const IntRect& a_rect, const IntSize& cell_size, Color color_dark, Color color_light)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = 0; i < rect.height(); ++i) {
        for (int j = 0; j < rect.width(); ++j) {
            int cell_row = i / cell_size.height();
            int cell_col = j / cell_size.width();
            dst[j] = ((cell_row % 2) ^ (cell_col % 2)) ? color_light.value() : color_dark.value();
        }
        dst += dst_skip;
    }
}

void Painter::fill_rect_with_gradient(Orientation orientation, const IntRect& a_rect, Color gradient_start, Color gradient_end)
{
    if (gradient_start == gradient_end) {
        fill_rect(a_rect, gradient_start);
        return;
    }

#ifdef NO_FPU
    return fill_rect(a_rect, gradient_start);
#endif

    auto rect = to_physical(a_rect);
    auto clipped_rect = IntRect::intersection(rect, clip_rect() * scale());
    if (clipped_rect.is_empty())
        return;

    int offset = clipped_rect.primary_offset_for_orientation(orientation) - rect.primary_offset_for_orientation(orientation);

    RGBA32* dst = m_target->scanline(clipped_rect.top()) + clipped_rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

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

void Painter::fill_rect_with_gradient(const IntRect& a_rect, Color gradient_start, Color gradient_end)
{
    return fill_rect_with_gradient(Orientation::Horizontal, a_rect, gradient_start, gradient_end);
}

void Painter::fill_ellipse(const IntRect& a_rect, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    VERIFY(m_target->rect().contains(rect));

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left() + rect.width() / 2;
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = 0; i < rect.height(); i++) {
        double y = rect.height() * 0.5 - i;
        double x = rect.width() * sqrt(0.25 - y * y / rect.height() / rect.height());
        fast_u32_fill(dst - (int)x, color.value(), 2 * (int)x);
        dst += dst_skip;
    }
}

void Painter::draw_ellipse_intersecting(const IntRect& rect, Color color, int thickness)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    constexpr int number_samples = 100; // FIXME: dynamically work out the number of samples based upon the rect size
    double increment = M_PI / number_samples;

    auto ellipse_x = [&](double theta) -> int {
        return (cos(theta) * rect.width() / sqrt(2)) + rect.center().x();
    };

    auto ellipse_y = [&](double theta) -> int {
        return (sin(theta) * rect.height() / sqrt(2)) + rect.center().y();
    };

    for (float theta = 0; theta < 2 * M_PI; theta += increment) {
        draw_line({ ellipse_x(theta), ellipse_y(theta) }, { ellipse_x(theta + increment), ellipse_y(theta + increment) }, color, thickness);
    }
}

template<typename RectType, typename Callback>
static void for_each_pixel_around_rect_clockwise(const RectType& rect, Callback callback)
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

void Painter::draw_focus_rect(const IntRect& rect, Color color)
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

void Painter::draw_rect(const IntRect& a_rect, Color color, bool rough)
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

void Painter::draw_bitmap(const IntPoint& p, const CharacterBitmap& bitmap, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto rect = IntRect(p, bitmap.size()).translated(translation());
    auto clipped_rect = rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = clipped_rect.top() - rect.top();
    const int last_row = clipped_rect.bottom() - rect.top();
    const int first_column = clipped_rect.left() - rect.left();
    const int last_column = clipped_rect.right() - rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);
    const char* bitmap_row = &bitmap.bits()[first_row * bitmap.width() + first_column];
    const size_t bitmap_skip = bitmap.width();

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

void Painter::draw_bitmap(const IntPoint& p, const GlyphBitmap& bitmap, Color color)
{
    auto dst_rect = IntRect(p, bitmap.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    const int last_column = clipped_rect.right() - dst_rect.left();

    int scale = this->scale();
    RGBA32* dst = m_target->scanline(clipped_rect.y() * scale) + clipped_rect.x() * scale;
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (scale == 1) {
        for (int row = first_row; row <= last_row; ++row) {
            for (int j = 0; j <= (last_column - first_column); ++j) {
                if (bitmap.bit_at(j + first_column, row))
                    dst[j] = color.value();
            }
            dst += dst_skip;
        }
    } else {
        for (int row = first_row; row <= last_row; ++row) {
            for (int j = 0; j <= (last_column - first_column); ++j) {
                if (bitmap.bit_at((j + first_column), row)) {
                    for (int iy = 0; iy < scale; ++iy)
                        for (int ix = 0; ix < scale; ++ix)
                            dst[j * scale + ix + iy * dst_skip] = color.value();
                }
            }
            dst += dst_skip * scale;
        }
    }
}

void Painter::draw_triangle(const IntPoint& a, const IntPoint& b, const IntPoint& c, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    IntPoint p0(a);
    IntPoint p1(b);
    IntPoint p2(c);

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

    const RGBA32* src;
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

void Painter::blit_with_opacity(const IntPoint& position, const Gfx::Bitmap& source, const IntRect& a_src_rect, float opacity, bool apply_alpha)
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

    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    const int last_column = clipped_rect.right() - dst_rect.left();

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

void Painter::blit_filtered(const IntPoint& position, const Gfx::Bitmap& source, const IntRect& src_rect, Function<Color(Color)> filter)
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

    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    const int last_column = clipped_rect.right() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    int s = scale / source.scale();
    if (s == 1) {
        const RGBA32* src = source.scanline(safe_src_rect.top() + first_row) + safe_src_rect.left() + first_column;
        const size_t src_skip = source.pitch() / sizeof(RGBA32);

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
            const RGBA32* src = source.scanline(safe_src_rect.top() + row / s) + safe_src_rect.left() + first_column / s;
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

void Painter::blit_brightened(const IntPoint& position, const Gfx::Bitmap& source, const IntRect& src_rect)
{
    return blit_filtered(position, source, src_rect, [](Color src) {
        return src.lightened();
    });
}

void Painter::blit_dimmed(const IntPoint& position, const Gfx::Bitmap& source, const IntRect& src_rect)
{
    return blit_filtered(position, source, src_rect, [](Color src) {
        return src.to_grayscale().lightened();
    });
}

void Painter::draw_tiled_bitmap(const IntRect& a_dst_rect, const Gfx::Bitmap& source)
{
    VERIFY((source.scale() == 1 || source.scale() == scale()) && "draw_tiled_bitmap only supports integer upsampling");

    auto dst_rect = a_dst_rect.translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    int scale = this->scale();
    clipped_rect *= scale;
    dst_rect *= scale;

    const int first_row = (clipped_rect.top() - dst_rect.top());
    const int last_row = (clipped_rect.bottom() - dst_rect.top());
    const int first_column = (clipped_rect.left() - dst_rect.left());
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (source.format() == BitmapFormat::BGRx8888 || source.format() == BitmapFormat::BGRA8888) {
        int s = scale / source.scale();
        if (s == 1) {
            int x_start = first_column + a_dst_rect.left() * scale;
            for (int row = first_row; row <= last_row; ++row) {
                const RGBA32* sl = source.scanline((row + a_dst_rect.top() * scale) % source.physical_height());
                for (int x = x_start; x < clipped_rect.width() + x_start; ++x) {
                    dst[x - x_start] = sl[x % source.physical_width()];
                }
                dst += dst_skip;
            }
        } else {
            int x_start = first_column + a_dst_rect.left() * scale;
            for (int row = first_row; row <= last_row; ++row) {
                const RGBA32* sl = source.scanline(((row + a_dst_rect.top() * scale) / s) % source.physical_height());
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

void Painter::blit_offset(const IntPoint& a_position, const Gfx::Bitmap& source, const IntRect& a_src_rect, const IntPoint& offset)
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

void Painter::blit(const IntPoint& position, const Gfx::Bitmap& source, const IntRect& a_src_rect, float opacity, bool apply_alpha)
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

    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (source.format() == BitmapFormat::BGRx8888 || source.format() == BitmapFormat::BGRA8888) {
        const RGBA32* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        const size_t src_skip = source.pitch() / sizeof(RGBA32);
        for (int row = first_row; row <= last_row; ++row) {
            fast_u32_copy(dst, src, clipped_rect.width());
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    if (source.format() == BitmapFormat::RGBA8888) {
        const u32* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        const size_t src_skip = source.pitch() / sizeof(u32);
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
        const u8* src = source.scanline_u8(src_rect.top() + first_row) + src_rect.left() + first_column;
        const size_t src_skip = source.pitch();
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
ALWAYS_INLINE static void do_draw_integer_scaled_bitmap(Gfx::Bitmap& target, const IntRect& dst_rect, const IntRect& src_rect, const Gfx::Bitmap& source, int hfactor, int vfactor, GetPixel get_pixel, float opacity)
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
ALWAYS_INLINE static void do_draw_scaled_bitmap(Gfx::Bitmap& target, const IntRect& dst_rect, const IntRect& clipped_rect, const Gfx::Bitmap& source, const FloatRect& src_rect, GetPixel get_pixel, float opacity)
{
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

    bool has_opacity = opacity != 1.0f;
    int hscale = (src_rect.width() * (1 << 16)) / dst_rect.width();
    int vscale = (src_rect.height() * (1 << 16)) / dst_rect.height();
    int src_left = src_rect.left() * (1 << 16);
    int src_top = src_rect.top() * (1 << 16);

    for (int y = clipped_rect.top(); y <= clipped_rect.bottom(); ++y) {
        auto* scanline = (Color*)target.scanline(y);
        for (int x = clipped_rect.left(); x <= clipped_rect.right(); ++x) {
            auto scaled_x = ((x - dst_rect.x()) * hscale + src_left) >> 16;
            auto scaled_y = ((y - dst_rect.y()) * vscale + src_top) >> 16;
            auto src_pixel = get_pixel(source, scaled_x, scaled_y);
            if (has_opacity)
                src_pixel.set_alpha(src_pixel.alpha() * opacity);
            if constexpr (has_alpha_channel) {
                scanline[x] = scanline[x].blend(src_pixel);
            } else
                scanline[x] = src_pixel;
        }
    }
}

void Painter::draw_scaled_bitmap(const IntRect& a_dst_rect, const Gfx::Bitmap& source, const IntRect& a_src_rect, float opacity)
{
    draw_scaled_bitmap(a_dst_rect, source, FloatRect { a_src_rect }, opacity);
}

void Painter::draw_scaled_bitmap(const IntRect& a_dst_rect, const Gfx::Bitmap& source, const FloatRect& a_src_rect, float opacity)
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
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::BGRx8888>, opacity);
            break;
        case BitmapFormat::BGRA8888:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::BGRA8888>, opacity);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed8>, opacity);
            break;
        case BitmapFormat::Indexed4:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed4>, opacity);
            break;
        case BitmapFormat::Indexed2:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed2>, opacity);
            break;
        case BitmapFormat::Indexed1:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed1>, opacity);
            break;
        default:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Invalid>, opacity);
            break;
        }
    } else {
        switch (source.format()) {
        case BitmapFormat::BGRx8888:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::BGRx8888>, opacity);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Indexed8>, opacity);
            break;
        default:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, get_pixel<BitmapFormat::Invalid>, opacity);
            break;
        }
    }
}

FLATTEN void Painter::draw_glyph(const IntPoint& point, u32 code_point, Color color)
{
    draw_glyph(point, code_point, font(), color);
}

FLATTEN void Painter::draw_glyph(const IntPoint& point, u32 code_point, const Font& font, Color color)
{
    auto glyph = font.glyph(code_point);
    auto top_left = point + IntPoint(glyph.left_bearing(), font.glyph_height() - glyph.ascent());

    if (glyph.is_glyph_bitmap()) {
        draw_bitmap(top_left, glyph.glyph_bitmap(), color);
    } else {
        blit_filtered(top_left, *glyph.bitmap(), glyph.bitmap()->rect(), [color](Color pixel) -> Color {
            return pixel.multiply(color);
        });
    }
}

void Painter::draw_emoji(const IntPoint& point, const Gfx::Bitmap& emoji, const Font& font)
{
    if (!font.is_fixed_width())
        blit(point, emoji, emoji.rect());
    else {
        IntRect dst_rect {
            point.x(),
            point.y(),
            font.glyph_width('x'),
            font.glyph_height()
        };
        draw_scaled_bitmap(dst_rect, emoji, emoji.rect());
    }
}

void Painter::draw_glyph_or_emoji(const IntPoint& point, u32 code_point, const Font& font, Color color)
{
    if (font.contains_glyph(code_point)) {
        draw_glyph(point, code_point, font, color);
        return;
    }

    // Perhaps it's an emoji?
    auto* emoji = Emoji::emoji_for_code_point(code_point);
    if (emoji == nullptr) {
        dbgln_if(EMOJI_DEBUG, "Failed to find an emoji for code_point {}", code_point);
        draw_glyph(point, '?', font, color);
        return;
    }

    draw_emoji(point, *emoji, font);
}

static void apply_elision(Utf8View& final_text, String& elided_text, size_t offset)
{
    StringBuilder builder;
    builder.append(final_text.substring_view(0, offset).as_string());
    builder.append("...");
    elided_text = builder.to_string();
    final_text = Utf8View { elided_text };
}

static void apply_elision(Utf32View& final_text, Vector<u32>& elided_text, size_t offset)
{
    elided_text.append(final_text.code_points(), offset);
    elided_text.append('.');
    elided_text.append('.');
    elided_text.append('.');
    final_text = Utf32View { elided_text.data(), elided_text.size() };
}

template<typename TextType>
struct ElidedText {
};

template<>
struct ElidedText<Utf8View> {
    typedef String Type;
};

template<>
struct ElidedText<Utf32View> {
    typedef Vector<u32> Type;
};

template<typename TextType, typename DrawGlyphFunction>
void draw_text_line(const IntRect& a_rect, const TextType& text, const Font& font, TextAlignment alignment, TextElision elision, DrawGlyphFunction draw_glyph)
{
    auto rect = a_rect;
    TextType final_text(text);
    typename ElidedText<TextType>::Type elided_text;
    if (elision == TextElision::Right) {
        int text_width = font.width(final_text);
        if (font.width(final_text) > rect.width()) {
            int glyph_spacing = font.glyph_spacing();
            int new_width = font.width("...");
            if (new_width < text_width) {
                size_t offset = 0;
                for (auto it = text.begin(); it != text.end(); ++it) {
                    auto code_point = *it;
                    int glyph_width = font.glyph_or_emoji_width(code_point);
                    // NOTE: Glyph spacing should not be added after the last glyph on the line,
                    //       but since we are here because the last glyph does not actually fit on the line,
                    //       we don't have to worry about spacing.
                    int width_with_this_glyph_included = new_width + glyph_width + glyph_spacing;
                    if (width_with_this_glyph_included > rect.width())
                        break;
                    new_width += glyph_width + glyph_spacing;
                    offset = text.iterator_offset(it);
                }
                apply_elision(final_text, elided_text, offset);
            }
        }
    }

    switch (alignment) {
    case TextAlignment::TopLeft:
    case TextAlignment::CenterLeft:
        break;
    case TextAlignment::TopRight:
    case TextAlignment::CenterRight:
    case TextAlignment::BottomRight:
        rect.set_x(rect.right() - font.width(final_text));
        break;
    case TextAlignment::Center: {
        auto shrunken_rect = rect;
        shrunken_rect.set_width(font.width(final_text));
        shrunken_rect.center_within(rect);
        rect = shrunken_rect;
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    if (is_vertically_centered_text_alignment(alignment)) {
        int distance_from_baseline_to_bottom = (font.glyph_height() - 1) - font.baseline();
        rect.move_by(0, distance_from_baseline_to_bottom / 2);
    }

    auto point = rect.location();
    int space_width = font.glyph_width(' ') + font.glyph_spacing();

    for (u32 code_point : final_text) {
        if (code_point == ' ') {
            point.move_by(space_width, 0);
            continue;
        }
        IntSize glyph_size(font.glyph_or_emoji_width(code_point) + font.glyph_spacing(), font.glyph_height());
        draw_glyph({ point, glyph_size }, code_point);
        point.move_by(glyph_size.width(), 0);
    }
}

static inline size_t draw_text_iterator_offset(const Utf8View& text, const Utf8View::Iterator& it)
{
    return text.byte_offset_of(it);
}

static inline size_t draw_text_iterator_offset(const Utf32View& text, const Utf32View::Iterator& it)
{
    return it - text.begin();
}

static inline size_t draw_text_get_length(const Utf8View& text)
{
    return text.byte_length();
}

static inline size_t draw_text_get_length(const Utf32View& text)
{
    return text.length();
}

template<typename TextType, typename DrawGlyphFunction>
void do_draw_text(const IntRect& rect, const TextType& text, const Font& font, TextAlignment alignment, TextElision elision, DrawGlyphFunction draw_glyph)
{
    Vector<TextType, 32> lines;

    size_t start_of_current_line = 0;
    for (auto it = text.begin(); it != text.end(); ++it) {
        u32 code_point = *it;
        if (code_point == '\n') {
            auto offset = draw_text_iterator_offset(text, it);
            TextType line = text.substring_view(start_of_current_line, offset - start_of_current_line);
            lines.append(line);
            start_of_current_line = offset + 1;
        }
    }

    if (start_of_current_line != draw_text_get_length(text)) {
        TextType line = text.substring_view(start_of_current_line, draw_text_get_length(text) - start_of_current_line);
        lines.append(line);
    }

    static const int line_spacing = 4;
    int line_height = font.glyph_height() + line_spacing;
    IntRect bounding_rect { 0, 0, 0, (static_cast<int>(lines.size()) * line_height) - line_spacing };

    for (auto& line : lines) {
        auto line_width = font.width(line);
        if (line_width > bounding_rect.width())
            bounding_rect.set_width(line_width);
    }

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
    case TextAlignment::BottomRight:
        bounding_rect.set_location({ (rect.right() + 1) - bounding_rect.width(), (rect.bottom() + 1) - bounding_rect.height() });
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    for (size_t i = 0; i < lines.size(); ++i) {
        auto& line = lines[i];
        IntRect line_rect { bounding_rect.x(), bounding_rect.y() + static_cast<int>(i) * line_height, bounding_rect.width(), line_height };
        line_rect.intersect(rect);
        draw_text_line(line_rect, line, font, alignment, elision, draw_glyph);
    }
}

void Painter::draw_text(const IntRect& rect, const StringView& text, TextAlignment alignment, Color color, TextElision elision)
{
    draw_text(rect, text, font(), alignment, color, elision);
}

void Painter::draw_text(const IntRect& rect, const Utf32View& text, TextAlignment alignment, Color color, TextElision elision)
{
    draw_text(rect, text, font(), alignment, color, elision);
}

void Painter::draw_text(const IntRect& rect, const StringView& raw_text, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    Utf8View text { raw_text };
    do_draw_text(rect, Utf8View(text), font, alignment, elision, [&](const IntRect& r, u32 code_point) {
        draw_glyph_or_emoji(r.location(), code_point, font, color);
    });
}

void Painter::draw_text(const IntRect& rect, const Utf32View& text, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    do_draw_text(rect, text, font, alignment, elision, [&](const IntRect& r, u32 code_point) {
        draw_glyph_or_emoji(r.location(), code_point, font, color);
    });
}

void Painter::draw_text(Function<void(const IntRect&, u32)> draw_one_glyph, const IntRect& rect, const StringView& raw_text, const Font& font, TextAlignment alignment, TextElision elision)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    Utf8View text { raw_text };
    do_draw_text(rect, text, font, alignment, elision, [&](const IntRect& r, u32 code_point) {
        draw_one_glyph(r, code_point);
    });
}

void Painter::draw_text(Function<void(const IntRect&, u32)> draw_one_glyph, const IntRect& rect, const Utf8View& text, const Font& font, TextAlignment alignment, TextElision elision)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    do_draw_text(rect, text, font, alignment, elision, [&](const IntRect& r, u32 code_point) {
        draw_one_glyph(r, code_point);
    });
}

void Painter::draw_text(Function<void(const IntRect&, u32)> draw_one_glyph, const IntRect& rect, const Utf32View& text, const Font& font, TextAlignment alignment, TextElision elision)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    do_draw_text(rect, text, font, alignment, elision, [&](const IntRect& r, u32 code_point) {
        draw_one_glyph(r, code_point);
    });
}

void Painter::set_pixel(const IntPoint& p, Color color)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    auto point = p;
    point.move_by(state().translation);
    if (!clip_rect().contains(point))
        return;
    m_target->scanline(point.y())[point.x()] = color.value();
}

ALWAYS_INLINE void Painter::set_physical_pixel_with_draw_op(u32& pixel, const Color& color)
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

ALWAYS_INLINE void Painter::fill_physical_scanline_with_draw_op(int y, int x, int width, const Color& color)
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

void Painter::draw_physical_pixel(const IntPoint& physical_position, Color color, int thickness)
{
    // This always draws a single physical pixel, independent of scale().
    // This should only be called by routines that already handle scale
    // (including scaling thickness).
    VERIFY(draw_op() == DrawOp::Copy);

    if (thickness == 1) { // Implies scale() == 1.
        auto& pixel = m_target->scanline(physical_position.y())[physical_position.x()];
        return set_physical_pixel_with_draw_op(pixel, Color::from_rgba(pixel).blend(color));
    }

    IntRect rect { physical_position, { thickness, thickness } };
    rect.intersect(clip_rect() * scale());
    fill_physical_rect(rect, color);
}

void Painter::draw_line(const IntPoint& p1, const IntPoint& p2, Color color, int thickness, LineStyle style)
{
    if (color.alpha() == 0)
        return;

    auto clip_rect = this->clip_rect() * scale();

    auto point1 = to_physical(p1);
    auto point2 = to_physical(p2);
    thickness *= scale();

    // Special case: vertical line.
    if (point1.x() == point2.x()) {
        const int x = point1.x();
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
            }
        } else {
            for (int y = min_y; y <= max_y; y += thickness)
                draw_physical_pixel({ x, y }, color, thickness);
        }
        return;
    }

    // Special case: horizontal line.
    if (point1.y() == point2.y()) {
        const int y = point1.y();
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
            }
        } else {
            for (int x = min_x; x <= max_x; x += thickness)
                draw_physical_pixel({ x, y }, color, thickness);
        }
        return;
    }

    // FIXME: Implement dotted/dashed diagonal lines.
    VERIFY(style == LineStyle::Solid);

    const int adx = abs(point2.x() - point1.x());
    const int ady = abs(point2.y() - point1.y());

    if (adx > ady) {
        if (point1.x() > point2.x())
            swap(point1, point2);
    } else {
        if (point1.y() > point2.y())
            swap(point1, point2);
    }

    // FIXME: Implement clipping below.
    const int dx = point2.x() - point1.x();
    const int dy = point2.y() - point1.y();
    int error = 0;

    if (dx > dy) {
        const int y_step = dy == 0 ? 0 : (dy > 0 ? 1 : -1);
        const int delta_error = 2 * abs(dy);
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
        const int x_step = dx == 0 ? 0 : (dx > 0 ? 1 : -1);
        const int delta_error = 2 * abs(dx);
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

static void split_quadratic_bezier_curve(const FloatPoint& original_control, const FloatPoint& p1, const FloatPoint& p2, Function<void(const FloatPoint&, const FloatPoint&)>& callback)
{
    auto po1_midpoint = original_control + p1;
    po1_midpoint /= 2;

    auto po2_midpoint = original_control + p2;
    po2_midpoint /= 2;

    auto new_segment = po1_midpoint + po2_midpoint;
    new_segment /= 2;

    Painter::for_each_line_segment_on_bezier_curve(po1_midpoint, p1, new_segment, callback);
    Painter::for_each_line_segment_on_bezier_curve(po2_midpoint, new_segment, p2, callback);
}

static bool can_approximate_bezier_curve(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& control)
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
void Painter::for_each_line_segment_on_bezier_curve(const FloatPoint& control_point, const FloatPoint& p1, const FloatPoint& p2, Function<void(const FloatPoint&, const FloatPoint&)>& callback)
{
    if (can_approximate_bezier_curve(p1, p2, control_point)) {
        callback(p1, p2);
    } else {
        split_quadratic_bezier_curve(control_point, p1, p2, callback);
    }
}

void Painter::for_each_line_segment_on_bezier_curve(const FloatPoint& control_point, const FloatPoint& p1, const FloatPoint& p2, Function<void(const FloatPoint&, const FloatPoint&)>&& callback)
{
    for_each_line_segment_on_bezier_curve(control_point, p1, p2, callback);
}

static void split_elliptical_arc(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& center, const FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(const FloatPoint&, const FloatPoint&)>& callback)
{
    auto half_theta_delta = theta_delta / 2;
    auto theta_mid = theta_1 + half_theta_delta;

    auto xc = cosf(x_axis_rotation);
    auto xs = sinf(x_axis_rotation);
    auto tc = cosf(theta_1 + half_theta_delta);
    auto ts = sinf(theta_1 + half_theta_delta);

    auto x2 = xc * radii.x() * tc - xs * radii.y() * ts + center.x();
    auto y2 = xs * radii.x() * tc + xc * radii.y() * ts + center.y();

    FloatPoint mid_point = { x2, y2 };

    Painter::for_each_line_segment_on_elliptical_arc(p1, mid_point, center, radii, x_axis_rotation, theta_1, half_theta_delta, callback);
    Painter::for_each_line_segment_on_elliptical_arc(mid_point, p2, center, radii, x_axis_rotation, theta_mid, half_theta_delta, callback);
}

static bool can_approximate_elliptical_arc(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& center, const FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta)
{
    constexpr static float tolerance = 1;

    auto half_theta_delta = theta_delta / 2.0f;

    auto xc = cosf(x_axis_rotation);
    auto xs = sinf(x_axis_rotation);
    auto tc = cosf(theta_1 + half_theta_delta);
    auto ts = sinf(theta_1 + half_theta_delta);

    auto x2 = xc * radii.x() * tc - xs * radii.y() * ts + center.x();
    auto y2 = xs * radii.x() * tc + xc * radii.y() * ts + center.y();

    auto ellipse_mid_point = FloatPoint { x2, y2 };
    auto line_mid_point = p1 + (p2 - p1) / 2.0f;

    return ellipse_mid_point.distance_from(line_mid_point) < tolerance;
}

void Painter::draw_quadratic_bezier_curve(const IntPoint& control_point, const IntPoint& p1, const IntPoint& p2, Color color, int thickness, LineStyle style)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    for_each_line_segment_on_bezier_curve(FloatPoint(control_point), FloatPoint(p1), FloatPoint(p2), [&](const FloatPoint& fp1, const FloatPoint& fp2) {
        draw_line(IntPoint(fp1.x(), fp1.y()), IntPoint(fp2.x(), fp2.y()), color, thickness, style);
    });
}

// static
void Painter::for_each_line_segment_on_elliptical_arc(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& center, const FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(const FloatPoint&, const FloatPoint&)>& callback)
{
    if (can_approximate_elliptical_arc(p1, p2, center, radii, x_axis_rotation, theta_1, theta_delta)) {
        callback(p1, p2);
    } else {
        split_elliptical_arc(p1, p2, center, radii, x_axis_rotation, theta_1, theta_delta, callback);
    }
}

// static
void Painter::for_each_line_segment_on_elliptical_arc(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& center, const FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta, Function<void(const FloatPoint&, const FloatPoint&)>&& callback)
{
    for_each_line_segment_on_elliptical_arc(p1, p2, center, radii, x_axis_rotation, theta_1, theta_delta, callback);
}

void Painter::draw_elliptical_arc(const IntPoint& p1, const IntPoint& p2, const IntPoint& center, const FloatPoint& radii, float x_axis_rotation, float theta_1, float theta_delta, Color color, int thickness, LineStyle style)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    for_each_line_segment_on_elliptical_arc(FloatPoint(p1), FloatPoint(p2), FloatPoint(center), radii, x_axis_rotation, theta_1, theta_delta, [&](const FloatPoint& fp1, const FloatPoint& fp2) {
        draw_line(IntPoint(fp1.x(), fp1.y()), IntPoint(fp2.x(), fp2.y()), color, thickness, style);
    });
}

void Painter::add_clip_rect(const IntRect& rect)
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

void Painter::stroke_path(const Path& path, Color color, int thickness)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

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
            auto& through = static_cast<const QuadraticBezierCurveSegment&>(segment).through();
            draw_quadratic_bezier_curve(through.to_type<int>(), cursor.to_type<int>(), segment.point().to_type<int>(), color, thickness);
            cursor = segment.point();
            break;
        }
        case Segment::Type::EllipticalArcTo:
            auto& arc = static_cast<const EllipticalArcSegment&>(segment);
            draw_elliptical_arc(cursor.to_type<int>(), segment.point().to_type<int>(), arc.center().to_type<int>(), arc.radii(), arc.x_axis_rotation(), arc.theta_1(), arc.theta_delta(), color, thickness);
            cursor = segment.point();
            break;
        }
    }
}

[[maybe_unused]] static void approximately_place_on_int_grid(FloatPoint ffrom, FloatPoint fto, IntPoint& from, IntPoint& to, Optional<IntPoint> previous_to)
{
    auto diffs = fto - ffrom;
    // Truncate all first (round down).
    from = ffrom.to_type<int>();
    to = fto.to_type<int>();
    // There are 16 possible configurations, by deciding to round each
    // coord up or down (and there are four coords, from.x from.y to.x to.y)
    // we will simply choose one which most closely matches the correct slope
    // with the following heuristic:
    // - if the x diff is positive or zero (that is, a right-to-left slant), round 'from.x' up and 'to.x' down.
    // - if the x diff is negative         (that is, a left-to-right slant), round 'from.x' down and 'to.x' up.
    // Note that we do not need to touch the 'y' attribute, as that is our scanline.
    if (diffs.x() >= 0) {
        from.set_x(from.x() + 1);
    } else {
        to.set_x(to.x() + 1);
    }
    if (previous_to.has_value() && from.x() != previous_to.value().x()) // The points have to line up, since we're using these lines to fill a shape.
        from.set_x(previous_to.value().x());
}

void Painter::fill_path(Path& path, Color color, WindingRule winding_rule)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.

    const auto& segments = path.split_lines();

    if (segments.size() == 0)
        return;

    Vector<Path::SplitLineSegment> active_list;
    active_list.ensure_capacity(segments.size());

    // first, grab the segments for the very first scanline
    int first_y = path.bounding_box().bottom_right().y() + 1;
    int last_y = path.bounding_box().top_left().y() - 1;
    float scanline = first_y;

    size_t last_active_segment { 0 };

    for (auto& segment : segments) {
        if (segment.maximum_y != scanline)
            break;
        active_list.append(segment);
        ++last_active_segment;
    }

    auto is_inside_shape = [winding_rule](int winding_number) {
        if (winding_rule == WindingRule::Nonzero)
            return winding_number != 0;

        if (winding_rule == WindingRule::EvenOdd)
            return winding_number % 2 == 0;

        VERIFY_NOT_REACHED();
    };

    auto increment_winding = [winding_rule](int& winding_number, const IntPoint& from, const IntPoint& to) {
        if (winding_rule == WindingRule::EvenOdd) {
            ++winding_number;
            return;
        }

        if (winding_rule == WindingRule::Nonzero) {
            if (from.dy_relative_to(to) < 0)
                ++winding_number;
            else
                --winding_number;
            return;
        }

        VERIFY_NOT_REACHED();
    };

    while (scanline >= last_y) {
        Optional<IntPoint> previous_to;
        if (active_list.size()) {
            // sort the active list by 'x' from right to left
            quick_sort(active_list, [](const auto& line0, const auto& line1) {
                return line1.x < line0.x;
            });
#if FILL_PATH_DEBUG
            if ((int)scanline % 10 == 0) {
                draw_text(IntRect(active_list.last().x - 20, scanline, 20, 10), String::number((int)scanline));
            }
#endif

            if (active_list.size() > 1) {
                auto winding_number { 0 };
                for (size_t i = 1; i < active_list.size(); ++i) {
                    auto& previous = active_list[i - 1];
                    auto& current = active_list[i];

                    IntPoint from, to;
                    IntPoint truncated_from { previous.x, scanline };
                    IntPoint truncated_to { current.x, scanline };
                    approximately_place_on_int_grid({ previous.x, scanline }, { current.x, scanline }, from, to, previous_to);

                    if (is_inside_shape(winding_number)) {
                        // The points between this segment and the previous are
                        // inside the shape

                        dbgln_if(FILL_PATH_DEBUG, "y={}: {} at {}: {} -- {}", scanline, winding_number, i, from, to);
                        draw_line(from, to, color, 1);
                    }

                    auto is_passing_through_maxima = scanline == previous.maximum_y
                        || scanline == previous.minimum_y
                        || scanline == current.maximum_y
                        || scanline == current.minimum_y;

                    auto is_passing_through_vertex = false;

                    if (is_passing_through_maxima) {
                        is_passing_through_vertex = previous.x == current.x;
                    }

                    if (!is_passing_through_vertex || previous.inverse_slope * current.inverse_slope < 0)
                        increment_winding(winding_number, truncated_from, truncated_to);

                    // update the x coord
                    active_list[i - 1].x -= active_list[i - 1].inverse_slope;
                }
                active_list.last().x -= active_list.last().inverse_slope;
            } else {
                auto point = IntPoint(active_list[0].x, scanline);
                draw_line(point, point, color);

                // update the x coord
                active_list.first().x -= active_list.first().inverse_slope;
            }
        }

        --scanline;
        // remove any edge that goes out of bound from the active list
        for (size_t i = 0, count = active_list.size(); i < count; ++i) {
            if (scanline <= active_list[i].minimum_y) {
                active_list.remove(i);
                --count;
                --i;
            }
        }
        for (size_t j = last_active_segment; j < segments.size(); ++j, ++last_active_segment) {
            auto& segment = segments[j];
            if (segment.maximum_y < scanline)
                break;
            if (segment.minimum_y >= scanline)
                continue;

            active_list.append(segment);
        }
    }

#if FILL_PATH_DEBUG
    size_t i { 0 };
    for (auto& segment : segments) {
        draw_line(Point<int>(segment.from), Point<int>(segment.to), Color::from_hsv(i++ * 360.0 / segments.size(), 1.0, 1.0), 1);
    }
#endif
}

void Painter::blit_disabled(const IntPoint& location, const Gfx::Bitmap& bitmap, const IntRect& rect, const Palette& palette)
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

void Painter::blit_tiled(const IntRect& dst_rect, const Gfx::Bitmap& bitmap, const IntRect& rect)
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

void Gfx::Painter::draw_ui_text(const Gfx::IntRect& rect, const StringView& text, const Gfx::Font& font, Gfx::TextAlignment text_alignment, Gfx::Color color)
{
    auto parse_ampersand_string = [](const StringView& raw_text, Optional<size_t>& underline_offset) -> String {
        if (raw_text.is_empty())
            return String::empty();

        StringBuilder builder;

        for (size_t i = 0; i < raw_text.length(); ++i) {
            if (raw_text[i] == '&') {
                if (i != (raw_text.length() - 1) && raw_text[i + 1] == '&')
                    builder.append(raw_text[i]);
                else if (!underline_offset.has_value())
                    underline_offset = i;
                continue;
            }
            builder.append(raw_text[i]);
        }
        return builder.to_string();
    };

    Optional<size_t> underline_offset;
    auto name_to_draw = parse_ampersand_string(text, underline_offset);

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
                draw_line({ x1, y }, { x2, y }, Color::Black);
                break;
            }
            width += font.glyph_or_emoji_width(*it) + font.glyph_spacing();
        }
    }
}

}
