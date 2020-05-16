/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/Assertions.h>
#include <AK/Function.h>
#include <AK/Memory.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Path.h>
#include <math.h>
#include <stdio.h>
#include <unistd.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

namespace Gfx {

template<BitmapFormat format = BitmapFormat::Invalid>
ALWAYS_INLINE Color get_pixel(const Gfx::Bitmap& bitmap, int x, int y)
{
    if constexpr (format == BitmapFormat::Indexed8)
        return bitmap.palette_color(bitmap.bits(y)[x]);
    if constexpr (format == BitmapFormat::RGB32)
        return Color::from_rgb(bitmap.scanline(y)[x]);
    if constexpr (format == BitmapFormat::RGBA32)
        return Color::from_rgba(bitmap.scanline(y)[x]);
    return bitmap.get_pixel(x, y);
}

Painter::Painter(Gfx::Bitmap& bitmap)
    : m_target(bitmap)
{
    m_state_stack.append(State());
    state().font = &Font::default_font();
    state().clip_rect = { { 0, 0 }, bitmap.size() };
    m_clip_origin = state().clip_rect;
}

Painter::~Painter()
{
}

void Painter::fill_rect_with_draw_op(const Rect& a_rect, Color color)
{
    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            set_pixel_with_draw_op(dst[j], color);
        dst += dst_skip;
    }
}

void Painter::clear_rect(const Rect& a_rect, Color color)
{
    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    ASSERT(m_target->rect().contains(rect));

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        fast_u32_fill(dst, color.value(), rect.width());
        dst += dst_skip;
    }
}

void Painter::fill_rect(const Rect& a_rect, Color color)
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

    ASSERT(m_target->rect().contains(rect));

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            dst[j] = Color::from_rgba(dst[j]).blend(color).value();
        dst += dst_skip;
    }
}

void Painter::fill_rect_with_dither_pattern(const Rect& a_rect, Color color_a, Color color_b)
{
    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int i = 0; i < rect.height(); ++i) {
        for (int j = 0; j < rect.width(); ++j) {
            bool checkboard_use_a = (i & 1) ^ (j & 1);
            dst[j] = checkboard_use_a ? color_a.value() : color_b.value();
        }
        dst += dst_skip;
    }
}

void Painter::fill_rect_with_checkerboard(const Rect& a_rect, const Size& cell_size, Color color_dark, Color color_light)
{
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

void Painter::fill_rect_with_gradient(Orientation orientation, const Rect& a_rect, Color gradient_start, Color gradient_end)
{
#ifdef NO_FPU
    return fill_rect(a_rect, gradient_start);
#endif
    auto rect = a_rect.translated(translation());
    auto clipped_rect = Rect::intersection(rect, clip_rect());
    if (clipped_rect.is_empty())
        return;

    int offset = clipped_rect.primary_offset_for_orientation(orientation) - rect.primary_offset_for_orientation(orientation);

    RGBA32* dst = m_target->scanline(clipped_rect.top()) + clipped_rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    float increment = (1.0 / ((rect.primary_size_for_orientation(orientation)) / 255.0));

    int r2 = gradient_start.red();
    int g2 = gradient_start.green();
    int b2 = gradient_start.blue();
    int r1 = gradient_end.red();
    int g1 = gradient_end.green();
    int b1 = gradient_end.blue();

    if (orientation == Orientation::Horizontal) {
        for (int i = clipped_rect.height() - 1; i >= 0; --i) {
            float c = offset * increment;
            for (int j = 0; j < clipped_rect.width(); ++j) {
                dst[j] = Color(
                    r1 / 255.0 * c + r2 / 255.0 * (255 - c),
                    g1 / 255.0 * c + g2 / 255.0 * (255 - c),
                    b1 / 255.0 * c + b2 / 255.0 * (255 - c))
                             .value();
                c += increment;
            }
            dst += dst_skip;
        }
    } else {
        float c = offset * increment;
        for (int i = clipped_rect.height() - 1; i >= 0; --i) {
            Color color(
                r1 / 255.0 * c + r2 / 255.0 * (255 - c),
                g1 / 255.0 * c + g2 / 255.0 * (255 - c),
                b1 / 255.0 * c + b2 / 255.0 * (255 - c));
            for (int j = 0; j < clipped_rect.width(); ++j) {
                dst[j] = color.value();
            }
            c += increment;
            dst += dst_skip;
        }
    }
}

void Painter::fill_rect_with_gradient(const Rect& a_rect, Color gradient_start, Color gradient_end)
{
    return fill_rect_with_gradient(Orientation::Horizontal, a_rect, gradient_start, gradient_end);
}

void Painter::draw_ellipse_intersecting(const Rect& rect, Color color, int thickness)
{
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

void Painter::draw_rect(const Rect& a_rect, Color color, bool rough)
{
    Rect rect = a_rect.translated(translation());
    auto clipped_rect = rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    int min_y = clipped_rect.top();
    int max_y = clipped_rect.bottom();

    if (rect.top() >= clipped_rect.top() && rect.top() <= clipped_rect.bottom()) {
        int start_x = rough ? max(rect.x() + 1, clipped_rect.x()) : clipped_rect.x();
        int width = rough ? min(rect.width() - 2, clipped_rect.width()) : clipped_rect.width();
        fast_u32_fill(m_target->scanline(rect.top()) + start_x, color.value(), width);
        ++min_y;
    }
    if (rect.bottom() >= clipped_rect.top() && rect.bottom() <= clipped_rect.bottom()) {
        int start_x = rough ? max(rect.x() + 1, clipped_rect.x()) : clipped_rect.x();
        int width = rough ? min(rect.width() - 2, clipped_rect.width()) : clipped_rect.width();
        fast_u32_fill(m_target->scanline(rect.bottom()) + start_x, color.value(), width);
        --max_y;
    }

    bool draw_left_side = rect.left() >= clipped_rect.left();
    bool draw_right_side = rect.right() == clipped_rect.right();

    if (draw_left_side && draw_right_side) {
        // Specialized loop when drawing both sides.
        for (int y = min_y; y <= max_y; ++y) {
            auto* bits = m_target->scanline(y);
            bits[rect.left()] = color.value();
            bits[rect.right()] = color.value();
        }
    } else {
        for (int y = min_y; y <= max_y; ++y) {
            auto* bits = m_target->scanline(y);
            if (draw_left_side)
                bits[rect.left()] = color.value();
            if (draw_right_side)
                bits[rect.right()] = color.value();
        }
    }
}

void Painter::draw_bitmap(const Point& p, const CharacterBitmap& bitmap, Color color)
{
    auto rect = Rect(p, bitmap.size()).translated(translation());
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

void Painter::draw_bitmap(const Point& p, const GlyphBitmap& bitmap, Color color)
{
    auto dst_rect = Rect(p, bitmap.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    const int last_column = clipped_rect.right() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    for (int row = first_row; row <= last_row; ++row) {
        for (int j = 0; j <= (last_column - first_column); ++j) {
            if (bitmap.bit_at(j + first_column, row))
                dst[j] = color.value();
        }
        dst += dst_skip;
    }
}

void Painter::draw_triangle(const Point& a, const Point& b, const Point& c, Color color)
{
    RGBA32 rgba = color.value();

    Point p0(a);
    Point p1(b);
    Point p2(c);

    if (p0.y() > p1.y())
        swap(p0, p1);
    if (p0.y() > p2.y())
        swap(p0, p2);
    if (p1.y() > p2.y())
        swap(p1, p2);

    auto clip = clip_rect();
    if (p0.y() >= clip.bottom())
        return;
    if (p2.y() < clip.top())
        return;

    float dx01 = (float)(p1.x() - p0.x()) / (p1.y() - p0.y());
    float dx02 = (float)(p2.x() - p0.x()) / (p2.y() - p0.y());
    float dx12 = (float)(p2.x() - p1.x()) / (p2.y() - p1.y());

    float x01 = p0.x();
    float x02 = p0.x();

    int top = p0.y();
    if (top < clip.top()) {
        x01 += dx01 * (clip.top() - top);
        x02 += dx02 * (clip.top() - top);
        top = clip.top();
    }

    for (int y = top; y < p1.y() && y < clip.bottom(); ++y) {
        int start = x01 > x02 ? max((int)x02, clip.left()) : max((int)x01, clip.left());
        int end = x01 > x02 ? min((int)x01, clip.right()) : min((int)x02, clip.right());
        auto* scanline = m_target->scanline(y);
        for (int x = start; x < end; x++) {
            scanline[x] = rgba;
        }
        x01 += dx01;
        x02 += dx02;
    }

    x02 = p0.x() + dx02 * (p1.y() - p0.y());
    float x12 = p1.x();

    top = p1.y();
    if (top < clip.top()) {
        x02 += dx02 * (clip.top() - top);
        x12 += dx12 * (clip.top() - top);
        top = clip.top();
    }

    for (int y = top; y < p2.y() && y < clip.bottom(); ++y) {
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

void Painter::blit_scaled(const Rect& dst_rect_raw, const Gfx::Bitmap& source, const Rect& src_rect, float hscale, float vscale)
{
    auto dst_rect = Rect(dst_rect_raw.location(), dst_rect_raw.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = (clipped_rect.top() - dst_rect.top());
    const int last_row = (clipped_rect.bottom() - dst_rect.top());
    const int first_column = (clipped_rect.left() - dst_rect.left());
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    int x_start = first_column + src_rect.left();
    for (int row = first_row; row <= last_row; ++row) {
        int sr = (row + src_rect.top()) * vscale;
        if (sr >= source.size().height() || sr < 0) {
            dst += dst_skip;
            continue;
        }
        const RGBA32* sl = source.scanline(sr);
        for (int x = x_start; x < clipped_rect.width() + x_start; ++x) {
            int sx = x * hscale;
            if (sx < source.size().width() && sx >= 0)
                dst[x - x_start] = sl[sx];
        }
        dst += dst_skip;
    }
    return;
}

void Painter::blit_with_opacity(const Point& position, const Gfx::Bitmap& source, const Rect& src_rect, float opacity)
{
    ASSERT(!m_target->has_alpha_channel());

    if (!opacity)
        return;
    if (opacity >= 1.0f)
        return blit(position, source, src_rect);

    u8 alpha = 255 * opacity;

    Rect safe_src_rect = Rect::intersection(src_rect, source.rect());
    Rect dst_rect(position, safe_src_rect.size());
    dst_rect.move_by(state().translation);
    auto clipped_rect = Rect::intersection(dst_rect, clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    const int last_column = clipped_rect.right() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const RGBA32* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);
    const unsigned src_skip = source.pitch() / sizeof(RGBA32);

    for (int row = first_row; row <= last_row; ++row) {
        for (int x = 0; x <= (last_column - first_column); ++x) {
            Color src_color_with_alpha = Color::from_rgb(src[x]);
            src_color_with_alpha.set_alpha(alpha);
            Color dst_color = Color::from_rgb(dst[x]);
            dst[x] = dst_color.blend(src_color_with_alpha).value();
        }
        dst += dst_skip;
        src += src_skip;
    }
}

void Painter::blit_filtered(const Point& position, const Gfx::Bitmap& source, const Rect& src_rect, Function<Color(Color)> filter)
{
    Rect safe_src_rect = src_rect.intersected(source.rect());
    auto dst_rect = Rect(position, safe_src_rect.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    const int last_column = clipped_rect.right() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const RGBA32* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);
    const size_t src_skip = source.pitch() / sizeof(RGBA32);

    for (int row = first_row; row <= last_row; ++row) {
        for (int x = 0; x <= (last_column - first_column); ++x) {
            u8 alpha = Color::from_rgba(src[x]).alpha();
            if (alpha == 0xff)
                dst[x] = filter(Color::from_rgba(src[x])).value();
            else if (!alpha)
                continue;
            else
                dst[x] = Color::from_rgba(dst[x]).blend(filter(Color::from_rgba(src[x]))).value();
        }
        dst += dst_skip;
        src += src_skip;
    }
}

void Painter::blit_brightened(const Point& position, const Gfx::Bitmap& source, const Rect& src_rect)
{
    return blit_filtered(position, source, src_rect, [](Color src) {
        return src.lightened();
    });
}

void Painter::blit_dimmed(const Point& position, const Gfx::Bitmap& source, const Rect& src_rect)
{
    return blit_filtered(position, source, src_rect, [](Color src) {
        return src.to_grayscale().lightened();
    });
}

void Painter::draw_tiled_bitmap(const Rect& a_dst_rect, const Gfx::Bitmap& source)
{
    auto dst_rect = a_dst_rect.translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = (clipped_rect.top() - dst_rect.top());
    const int last_row = (clipped_rect.bottom() - dst_rect.top());
    const int first_column = (clipped_rect.left() - dst_rect.left());
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (source.format() == BitmapFormat::RGB32 || source.format() == BitmapFormat::RGBA32) {
        int x_start = first_column + a_dst_rect.left();
        for (int row = first_row; row <= last_row; ++row) {
            const RGBA32* sl = source.scanline((row + a_dst_rect.top())
                % source.size().height());
            for (int x = x_start; x < clipped_rect.width() + x_start; ++x) {
                dst[x - x_start] = sl[x % source.size().width()];
            }
            dst += dst_skip;
        }
        return;
    }

    ASSERT_NOT_REACHED();
}

void Painter::blit_offset(const Point& position,
    const Gfx::Bitmap& source,
    const Rect& src_rect,
    const Point& offset)
{
    auto dst_rect = Rect(position, src_rect.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = (clipped_rect.top() - dst_rect.top());
    const int last_row = (clipped_rect.bottom() - dst_rect.top());
    const int first_column = (clipped_rect.left() - dst_rect.left());
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (source.format() == BitmapFormat::RGB32 || source.format() == BitmapFormat::RGBA32) {
        int x_start = first_column + src_rect.left();
        for (int row = first_row; row <= last_row; ++row) {
            int sr = row - offset.y() + src_rect.top();
            if (sr >= source.size().height() || sr < 0) {
                dst += dst_skip;
                continue;
            }
            const RGBA32* sl = source.scanline(sr);
            for (int x = x_start; x < clipped_rect.width() + x_start; ++x) {
                int sx = x - offset.x();
                if (sx < source.size().width() && sx >= 0)
                    dst[x - x_start] = sl[sx];
            }
            dst += dst_skip;
        }
        return;
    }

    ASSERT_NOT_REACHED();
}

void Painter::blit_with_alpha(const Point& position, const Gfx::Bitmap& source, const Rect& src_rect)
{
    ASSERT(source.has_alpha_channel());
    Rect safe_src_rect = src_rect.intersected(source.rect());
    auto dst_rect = Rect(position, safe_src_rect.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    const int last_column = clipped_rect.right() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const RGBA32* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);
    const size_t src_skip = source.pitch() / sizeof(RGBA32);

    for (int row = first_row; row <= last_row; ++row) {
        for (int x = 0; x <= (last_column - first_column); ++x) {
            u8 alpha = Color::from_rgba(src[x]).alpha();
            if (alpha == 0xff)
                dst[x] = src[x];
            else if (!alpha)
                continue;
            else
                dst[x] = Color::from_rgba(dst[x]).blend(Color::from_rgba(src[x])).value();
        }
        dst += dst_skip;
        src += src_skip;
    }
}

void Painter::blit(const Point& position, const Gfx::Bitmap& source, const Rect& src_rect, float opacity)
{
    if (opacity < 1.0f)
        return blit_with_opacity(position, source, src_rect, opacity);
    if (source.has_alpha_channel())
        return blit_with_alpha(position, source, src_rect);
    auto safe_src_rect = src_rect.intersected(source.rect());
    ASSERT(source.rect().contains(safe_src_rect));
    auto dst_rect = Rect(position, safe_src_rect.size()).translated(translation());
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;
    const int first_row = clipped_rect.top() - dst_rect.top();
    const int last_row = clipped_rect.bottom() - dst_rect.top();
    const int first_column = clipped_rect.left() - dst_rect.left();
    RGBA32* dst = m_target->scanline(clipped_rect.y()) + clipped_rect.x();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (source.format() == BitmapFormat::RGB32 || source.format() == BitmapFormat::RGBA32) {
        const RGBA32* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        const size_t src_skip = source.pitch() / sizeof(RGBA32);
        for (int row = first_row; row <= last_row; ++row) {
            fast_u32_copy(dst, src, clipped_rect.width());
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    if (source.format() == BitmapFormat::Indexed8) {
        const u8* src = source.bits(src_rect.top() + first_row) + src_rect.left() + first_column;
        const size_t src_skip = source.pitch();
        for (int row = first_row; row <= last_row; ++row) {
            for (int i = 0; i < clipped_rect.width(); ++i)
                dst[i] = source.palette_color(src[i]).value();
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    ASSERT_NOT_REACHED();
}

template<bool has_alpha_channel, typename GetPixel>
ALWAYS_INLINE static void do_draw_integer_scaled_bitmap(Gfx::Bitmap& target, const Rect& dst_rect, const Gfx::Bitmap& source, int hfactor, int vfactor, GetPixel get_pixel)
{
    for (int y = source.rect().top(); y <= source.rect().bottom(); ++y) {
        int dst_y = dst_rect.y() + y * vfactor;
        for (int x = source.rect().left(); x <= source.rect().right(); ++x) {
            auto src_pixel = get_pixel(source, x, y);
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
ALWAYS_INLINE static void do_draw_scaled_bitmap(Gfx::Bitmap& target, const Rect& dst_rect, const Rect& clipped_rect, const Gfx::Bitmap& source, const Rect& src_rect, int hscale, int vscale, GetPixel get_pixel)
{
    if (dst_rect == clipped_rect && !(dst_rect.width() % src_rect.width()) && !(dst_rect.height() % src_rect.height())) {
        int hfactor = dst_rect.width() / src_rect.width();
        int vfactor = dst_rect.height() / src_rect.height();
        if (hfactor == 2 && vfactor == 2)
            return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, source, 2, 2, get_pixel);
        if (hfactor == 3 && vfactor == 3)
            return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, source, 3, 3, get_pixel);
        if (hfactor == 4 && vfactor == 4)
            return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, source, 4, 4, get_pixel);
        return do_draw_integer_scaled_bitmap<has_alpha_channel>(target, dst_rect, source, hfactor, vfactor, get_pixel);
    }

    for (int y = clipped_rect.top(); y <= clipped_rect.bottom(); ++y) {
        auto* scanline = (Color*)target.scanline(y);
        for (int x = clipped_rect.left(); x <= clipped_rect.right(); ++x) {
            auto scaled_x = ((x - dst_rect.x()) * hscale) >> 16;
            auto scaled_y = ((y - dst_rect.y()) * vscale) >> 16;
            auto src_pixel = get_pixel(source, scaled_x, scaled_y);

            if constexpr (has_alpha_channel) {
                scanline[x] = scanline[x].blend(src_pixel);
            } else
                scanline[x] = src_pixel;
        }
    }
}

void Painter::draw_scaled_bitmap(const Rect& a_dst_rect, const Gfx::Bitmap& source, const Rect& src_rect)
{
    auto dst_rect = a_dst_rect;
    if (dst_rect.size() == src_rect.size())
        return blit(dst_rect.location(), source, src_rect);

    auto safe_src_rect = src_rect.intersected(source.rect());
    ASSERT(source.rect().contains(safe_src_rect));
    dst_rect.move_by(state().translation);
    auto clipped_rect = dst_rect.intersected(clip_rect());
    if (clipped_rect.is_empty())
        return;

    int hscale = (src_rect.width() << 16) / dst_rect.width();
    int vscale = (src_rect.height() << 16) / dst_rect.height();

    if (source.has_alpha_channel()) {
        switch (source.format()) {
        case BitmapFormat::RGB32:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::RGB32>);
            break;
        case BitmapFormat::RGBA32:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::RGBA32>);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::Indexed8>);
            break;
        default:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::Invalid>);
            break;
        }
    } else {
        switch (source.format()) {
        case BitmapFormat::RGB32:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::RGB32>);
            break;
        case BitmapFormat::RGBA32:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::RGBA32>);
            break;
        case BitmapFormat::Indexed8:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::Indexed8>);
            break;
        default:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<BitmapFormat::Invalid>);
            break;
        }
    }
}

FLATTEN void Painter::draw_glyph(const Point& point, u32 codepoint, Color color)
{
    draw_glyph(point, codepoint, font(), color);
}

FLATTEN void Painter::draw_glyph(const Point& point, u32 codepoint, const Font& font, Color color)
{
    draw_bitmap(point, font.glyph_bitmap(codepoint), color);
}

void Painter::draw_emoji(const Point& point, const Gfx::Bitmap& emoji, const Font& font)
{
    if (!font.is_fixed_width())
        blit(point, emoji, emoji.rect());
    else {
        Rect dst_rect {
            point.x(),
            point.y(),
            font.glyph_width('x'),
            font.glyph_height()
        };
        draw_scaled_bitmap(dst_rect, emoji, emoji.rect());
    }
}

void Painter::draw_glyph_or_emoji(const Point& point, u32 codepoint, const Font& font, Color color)
{
    if (codepoint < (u32)font.glyph_count()) {
        // This looks like a regular character.
        draw_glyph(point, (size_t)codepoint, font, color);
        return;
    }

    // Perhaps it's an emoji?
    auto* emoji = Emoji::emoji_for_codepoint(codepoint);
    if (emoji == nullptr) {
#ifdef EMOJI_DEBUG
        dbg() << "Failed to find an emoji for codepoint " << codepoint;
#endif
        draw_glyph(point, '?', font, color);
        return;
    }

    draw_emoji(point, *emoji, font);
}

void Painter::draw_text_line(const Rect& a_rect, const Utf8View& text, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    auto rect = a_rect;
    Utf8View final_text(text);
    String elided_text;
    if (elision == TextElision::Right) {
        int text_width = font.width(final_text);
        if (font.width(final_text) > rect.width()) {
            int glyph_spacing = font.glyph_spacing();
            int byte_offset = 0;
            int new_width = font.width("...");
            if (new_width < text_width) {
                for (auto it = final_text.begin(); it != final_text.end(); ++it) {
                    u32 codepoint = *it;
                    int glyph_width = font.glyph_or_emoji_width(codepoint);
                    // NOTE: Glyph spacing should not be added after the last glyph on the line,
                    //       but since we are here because the last glyph does not actually fit on the line,
                    //       we don't have to worry about spacing.
                    int width_with_this_glyph_included = new_width + glyph_width + glyph_spacing;
                    if (width_with_this_glyph_included > rect.width())
                        break;
                    byte_offset = final_text.byte_offset_of(it);
                    new_width += glyph_width + glyph_spacing;
                }
                StringBuilder builder;
                builder.append(final_text.substring_view(0, byte_offset).as_string());
                builder.append("...");
                elided_text = builder.to_string();
                final_text = Utf8View { elided_text };
            }
        }
    }

    switch (alignment) {
    case TextAlignment::TopLeft:
    case TextAlignment::CenterLeft:
        break;
    case TextAlignment::TopRight:
    case TextAlignment::CenterRight:
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
        ASSERT_NOT_REACHED();
    }

    auto point = rect.location();
    int space_width = font.glyph_width(' ') + font.glyph_spacing();

    for (u32 codepoint : final_text) {
        if (codepoint == ' ') {
            point.move_by(space_width, 0);
            continue;
        }
        draw_glyph_or_emoji(point, codepoint, font, color);
        point.move_by(font.glyph_or_emoji_width(codepoint) + font.glyph_spacing(), 0);
    }
}

void Painter::draw_text_line(const Rect& a_rect, const Utf32View& text, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    auto rect = a_rect;
    Utf32View final_text(text);
    Vector<u32> elided_text;
    if (elision == TextElision::Right) {
        int text_width = font.width(final_text);
        if (font.width(final_text) > rect.width()) {
            int glyph_spacing = font.glyph_spacing();
            int new_width = font.width("...");
            if (new_width < text_width) {
                size_t i = 0;
                for (; i < text.length(); ++i) {
                    u32 codepoint = text.codepoints()[i];
                    int glyph_width = font.glyph_or_emoji_width(codepoint);
                    // NOTE: Glyph spacing should not be added after the last glyph on the line,
                    //       but since we are here because the last glyph does not actually fit on the line,
                    //       we don't have to worry about spacing.
                    int width_with_this_glyph_included = new_width + glyph_width + glyph_spacing;
                    if (width_with_this_glyph_included > rect.width())
                        break;
                    new_width += glyph_width + glyph_spacing;
                }
                elided_text.clear();
                elided_text.append(final_text.codepoints(), i);
                elided_text.append('.');
                elided_text.append('.');
                elided_text.append('.');
                final_text = Utf32View { elided_text.data(), elided_text.size() };
            }
        }
    }

    switch (alignment) {
    case TextAlignment::TopLeft:
    case TextAlignment::CenterLeft:
        break;
    case TextAlignment::TopRight:
    case TextAlignment::CenterRight:
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
        ASSERT_NOT_REACHED();
    }

    auto point = rect.location();
    int space_width = font.glyph_width(' ') + font.glyph_spacing();

    for (size_t i = 0; i < final_text.length(); ++i) {
        auto codepoint = final_text.codepoints()[i];
        if (codepoint == ' ') {
            point.move_by(space_width, 0);
            continue;
        }
        draw_glyph_or_emoji(point, codepoint, font, color);
        point.move_by(font.glyph_or_emoji_width(codepoint) + font.glyph_spacing(), 0);
    }
}

void Painter::draw_text(const Rect& rect, const StringView& text, TextAlignment alignment, Color color, TextElision elision)
{
    draw_text(rect, text, font(), alignment, color, elision);
}

void Painter::draw_text(const Rect& rect, const Utf32View& text, TextAlignment alignment, Color color, TextElision elision)
{
    draw_text(rect, text, font(), alignment, color, elision);
}

void Painter::draw_text(const Rect& rect, const StringView& raw_text, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    Utf8View text { raw_text };
    Vector<Utf8View, 32> lines;

    int start_of_current_line = 0;
    for (auto it = text.begin(); it != text.end(); ++it) {
        u32 codepoint = *it;
        if (codepoint == '\n') {
            int byte_offset = text.byte_offset_of(it);
            Utf8View line = text.substring_view(start_of_current_line, byte_offset - start_of_current_line);
            lines.append(line);
            start_of_current_line = byte_offset + 1;
        }
    }

    if (start_of_current_line != text.byte_length()) {
        Utf8View line = text.substring_view(start_of_current_line, text.byte_length() - start_of_current_line);
        lines.append(line);
    }

    static const int line_spacing = 4;
    int line_height = font.glyph_height() + line_spacing;
    Rect bounding_rect { 0, 0, 0, (static_cast<int>(lines.size()) * line_height) - line_spacing };

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
    default:
        ASSERT_NOT_REACHED();
    }

    for (size_t i = 0; i < lines.size(); ++i) {
        auto& line = lines[i];
        Rect line_rect { bounding_rect.x(), bounding_rect.y() + static_cast<int>(i) * line_height, bounding_rect.width(), line_height };
        line_rect.intersect(rect);
        draw_text_line(line_rect, line, font, alignment, color, elision);
    }
}

void Painter::draw_text(const Rect& rect, const Utf32View& text, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    Vector<Utf32View, 32> lines;

    size_t start_of_current_line = 0;
    for (size_t i = 0; i < text.length(); ++i) {
        u32 codepoint = text.codepoints()[i];
        if (codepoint == '\n') {
            Utf32View line = text.substring_view(start_of_current_line, i - start_of_current_line);
            lines.append(line);
            start_of_current_line = i + 1;
        }
    }

    if (start_of_current_line != text.length()) {
        Utf32View line = text.substring_view(start_of_current_line, text.length() - start_of_current_line);
        lines.append(line);
    }

    static const int line_spacing = 4;
    int line_height = font.glyph_height() + line_spacing;
    Rect bounding_rect { 0, 0, 0, (static_cast<int>(lines.size()) * line_height) - line_spacing };

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
    default:
        ASSERT_NOT_REACHED();
    }

    for (size_t i = 0; i < lines.size(); ++i) {
        auto& line = lines[i];
        Rect line_rect { bounding_rect.x(), bounding_rect.y() + static_cast<int>(i) * line_height, bounding_rect.width(), line_height };
        line_rect.intersect(rect);
        draw_text_line(line_rect, line, font, alignment, color, elision);
    }
}

void Painter::set_pixel(const Point& p, Color color)
{
    auto point = p;
    point.move_by(state().translation);
    if (!clip_rect().contains(point))
        return;
    m_target->scanline(point.y())[point.x()] = color.value();
}

ALWAYS_INLINE void Painter::set_pixel_with_draw_op(u32& pixel, const Color& color)
{
    if (draw_op() == DrawOp::Copy)
        pixel = color.value();
    else if (draw_op() == DrawOp::Xor)
        pixel ^= color.value();
}

void Painter::draw_pixel(const Point& position, Color color, int thickness)
{
    ASSERT(draw_op() == DrawOp::Copy);
    if (thickness == 1)
        return set_pixel_with_draw_op(m_target->scanline(position.y())[position.x()], color);
    Rect rect { position.translated(-(thickness / 2), -(thickness / 2)), { thickness, thickness } };
    fill_rect(rect.translated(-state().translation), color);
}

void Painter::draw_line(const Point& p1, const Point& p2, Color color, int thickness, LineStyle style)
{
    auto clip_rect = this->clip_rect();

    auto point1 = p1;
    point1.move_by(state().translation);

    auto point2 = p2;
    point2.move_by(state().translation);

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
                draw_pixel({ x, y }, color, thickness);
        } else if (style == LineStyle::Dashed) {
            for (int y = min_y; y <= max_y; y += thickness * 6) {
                draw_pixel({ x, y }, color, thickness);
                draw_pixel({ x, min(y + thickness, max_y) }, color, thickness);
                draw_pixel({ x, min(y + thickness * 2, max_y) }, color, thickness);
            }
        } else {
            for (int y = min_y; y <= max_y; ++y)
                draw_pixel({ x, y }, color, thickness);
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
                draw_pixel({ x, y }, color, thickness);
        } else if (style == LineStyle::Dashed) {
            for (int x = min_x; x <= max_x; x += thickness * 6) {
                draw_pixel({ x, y }, color, thickness);
                draw_pixel({ min(x + thickness, max_x), y }, color, thickness);
                draw_pixel({ min(x + thickness * 2, max_x), y }, color, thickness);
            }
        } else {
            for (int x = min_x; x <= max_x; ++x)
                draw_pixel({ x, y }, color, thickness);
        }
        return;
    }

    // FIXME: Implement dotted/dashed diagonal lines.
    ASSERT(style == LineStyle::Solid);

    const double adx = abs(point2.x() - point1.x());
    const double ady = abs(point2.y() - point1.y());

    if (adx > ady) {
        if (point1.x() > point2.x())
            swap(point1, point2);
    } else {
        if (point1.y() > point2.y())
            swap(point1, point2);
    }

    // FIXME: Implement clipping below.
    const double dx = point2.x() - point1.x();
    const double dy = point2.y() - point1.y();
    double error = 0;

    if (dx > dy) {
        const double y_step = dy == 0 ? 0 : (dy > 0 ? 1 : -1);
        const double delta_error = fabs(dy / dx);
        int y = point1.y();
        for (int x = point1.x(); x <= point2.x(); ++x) {
            if (clip_rect.contains(x, y))
                draw_pixel({ x, y }, color, thickness);
            error += delta_error;
            if (error >= 0.5) {
                y = (double)y + y_step;
                error -= 1.0;
            }
        }
    } else {
        const double x_step = dx == 0 ? 0 : (dx > 0 ? 1 : -1);
        const double delta_error = fabs(dx / dy);
        int x = point1.x();
        for (int y = point1.y(); y <= point2.y(); ++y) {
            if (clip_rect.contains(x, y))
                draw_pixel({ x, y }, color, thickness);
            error += delta_error;
            if (error >= 0.5) {
                x = (double)x + x_step;
                error -= 1.0;
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

void Painter::draw_quadratic_bezier_curve(const Point& control_point, const Point& p1, const Point& p2, Color color, int thickness, LineStyle style)
{
    for_each_line_segment_on_bezier_curve(FloatPoint(control_point.x(), control_point.y()), FloatPoint(p1.x(), p1.y()), FloatPoint(p2.x(), p2.y()), [&](const FloatPoint& p1, const FloatPoint& p2) {
        draw_line(Point(p1.x(), p1.y()), Point(p2.x(), p2.y()), color, thickness, style);
    });
}

void Painter::add_clip_rect(const Rect& rect)
{
    state().clip_rect.intersect(rect.translated(m_clip_origin.location()));
    state().clip_rect.intersect(m_target->rect());
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
    FloatPoint cursor;

    for (auto& segment : path.segments()) {
        switch (segment.type) {
        case Path::Segment::Type::Invalid:
            ASSERT_NOT_REACHED();
            break;
        case Path::Segment::Type::MoveTo:
            cursor = segment.point;
            break;
        case Path::Segment::Type::LineTo:
            draw_line(Point(cursor.x(), cursor.y()), Point(segment.point.x(), segment.point.y()), color, thickness);
            cursor = segment.point;
            break;
        case Path::Segment::Type::QuadraticBezierCurveTo:
            ASSERT(segment.through.has_value());
            draw_quadratic_bezier_curve(Point(segment.through.value().x(), segment.through.value().y()), Point(cursor.x(), cursor.y()), Point(segment.point.x(), segment.point.y()), color, thickness);
            cursor = segment.point;
            break;
        }
    }
}

//#define FILL_PATH_DEBUG

void Painter::fill_path(Path& path, Color color, WindingRule winding_rule)
{
    const auto& segments = path.split_lines();

    if (segments.size() == 0)
        return;

    Vector<Path::LineSegment> active_list;
    active_list.ensure_capacity(segments.size());

    // first, grab the segments for the very first scanline
    auto first_y = segments.first().maximum_y;
    auto last_y = segments.last().minimum_y;
    auto scanline = first_y;

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

        ASSERT_NOT_REACHED();
    };

    auto increment_winding = [winding_rule](int& winding_number, const Point& from, const Point& to) {
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

        ASSERT_NOT_REACHED();
    };

    while (scanline >= last_y) {
        if (active_list.size()) {
            // sort the active list by 'x' from right to left
            quick_sort(active_list, [](const auto& line0, const auto& line1) {
                return line1.x < line0.x;
            });
#ifdef FILL_PATH_DEBUG
            if ((int)scanline % 10 == 0) {
                draw_text(Rect(active_list.last().x - 20, scanline, 20, 10), String::format("%d", (int)scanline));
            }
#endif

            if (active_list.size() > 1) {
                auto winding_number { 0 };
                for (size_t i = 1; i < active_list.size(); ++i) {
                    auto& previous = active_list[i - 1];
                    auto& current = active_list[i];

                    int int_distance = fabs(current.x - previous.x);
                    Point from(previous.x, scanline);
                    Point to(current.x, scanline);

                    if (int_distance < 1) {
                        // the two lines intersect on an int grid
                        // so they should both be treated as a single line segment
                        goto skip_drawing;
                    }

                    if (int_distance == 1 && is_inside_shape(winding_number)) {
                        // The two lines form a singluar edge for the shape
                        // while they do not intersect, they connect together
                        goto skip_drawing;
                    }

                    if (is_inside_shape(winding_number)) {
                        // The points between this segment and the previous are
                        // inside the shape
#ifdef FILL_PATH_DEBUG
                        dbg() << "y=" << scanline << ": " << winding_number << " at " << i << ": " << from << " -- " << to;
#endif
                        draw_line(from, to, color, 1);
                    }

                skip_drawing:;

                    auto is_passing_through_maxima = scanline == previous.maximum_y
                        || scanline == previous.minimum_y
                        || scanline == current.maximum_y
                        || scanline == current.minimum_y;

                    auto is_passing_through_vertex = false;

                    if (is_passing_through_maxima) {
                        is_passing_through_vertex = previous.x == current.x;
                    }

                    if (!is_passing_through_vertex || previous.inverse_slope * current.inverse_slope < 0)
                        increment_winding(winding_number, from, to);

                    // update the x coord
                    active_list[i - 1].x -= active_list[i - 1].inverse_slope;
                }
                active_list.last().x -= active_list.last().inverse_slope;
            } else {
                auto point = Point(active_list[0].x, scanline);
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

#ifdef FILL_PATH_DEBUG
    size_t i { 0 };
    for (auto& segment : segments)
        draw_line(Point(segment.from.x(), segment.from.y()), Point(segment.to.x(), segment.to.y()), Color::from_hsv(++i / segments.size() * 255, 255, 255), 1);
#endif
}

}
