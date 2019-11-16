#include "Painter.h"
#include "Emoji.h"
#include "Font.h"
#include "GraphicsBitmap.h"
#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibDraw/CharacterBitmap.h>
#include <math.h>
#include <stdio.h>
#include <unistd.h>

#pragma GCC optimize("O3")

template<GraphicsBitmap::Format format = GraphicsBitmap::Format::Invalid>
static ALWAYS_INLINE Color get_pixel(const GraphicsBitmap& bitmap, int x, int y)
{
    if constexpr (format == GraphicsBitmap::Format::Indexed8)
        return bitmap.palette_color(bitmap.bits(y)[x]);
    if constexpr (format == GraphicsBitmap::Format::RGB32)
        return Color::from_rgb(bitmap.scanline(y)[x]);
    if constexpr (format == GraphicsBitmap::Format::RGBA32)
        return Color::from_rgba(bitmap.scanline(y)[x]);
    return bitmap.get_pixel(x, y);
}

Painter::Painter(GraphicsBitmap& bitmap)
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

void Painter::fill_rect(const Rect& a_rect, Color color)
{
    if (color.alpha() == 0)
        return;

    if (draw_op() != DrawOp::Copy) {
        fill_rect_with_draw_op(a_rect, color);
        return;
    }

    auto rect = a_rect.translated(translation()).intersected(clip_rect());
    if (rect.is_empty())
        return;

    ASSERT(m_target->rect().contains(rect));

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    if (color.alpha() == 0xff) {
        for (int i = rect.height() - 1; i >= 0; --i) {
            fast_u32_fill(dst, color.value(), rect.width());
            dst += dst_skip;
        }
        return;
    }

    for (int i = rect.height() - 1; i >= 0; --i) {
        for (int j = 0; j < rect.width(); ++j)
            dst[j] = Color::from_rgba(dst[j]).blend(color).value();
        dst += dst_skip;
    }
}

void Painter::fill_rect_with_gradient(const Rect& a_rect, Color gradient_start, Color gradient_end)
{
#ifdef NO_FPU
    return fill_rect(a_rect, gradient_start);
#endif
    auto rect = a_rect.translated(translation());
    auto clipped_rect = Rect::intersection(rect, clip_rect());
    if (clipped_rect.is_empty())
        return;

    int x_offset = clipped_rect.x() - rect.x();

    RGBA32* dst = m_target->scanline(clipped_rect.top()) + clipped_rect.left();
    const size_t dst_skip = m_target->pitch() / sizeof(RGBA32);

    float increment = (1.0 / ((rect.width()) / 255.0));

    int r2 = gradient_start.red();
    int g2 = gradient_start.green();
    int b2 = gradient_start.blue();
    int r1 = gradient_end.red();
    int g1 = gradient_end.green();
    int b1 = gradient_end.blue();

    for (int i = clipped_rect.height() - 1; i >= 0; --i) {
        float c = x_offset * increment;
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

void Painter::blit_scaled(const Rect& dst_rect_raw, const GraphicsBitmap& source, const Rect& src_rect, float hscale, float vscale)
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

void Painter::blit_with_opacity(const Point& position, const GraphicsBitmap& source, const Rect& src_rect, float opacity)
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

void Painter::blit_dimmed(const Point& position, const GraphicsBitmap& source, const Rect& src_rect)
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
                dst[x] = Color::from_rgba(src[x]).to_grayscale().lightened().value();
            else if (!alpha)
                continue;
            else
                dst[x] = Color::from_rgba(dst[x]).blend(Color::from_rgba(src[x]).to_grayscale().lightened()).value();
        }
        dst += dst_skip;
        src += src_skip;
    }
}

void Painter::draw_tiled_bitmap(const Rect& a_dst_rect, const GraphicsBitmap& source)
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

    if (source.format() == GraphicsBitmap::Format::RGB32 || source.format() == GraphicsBitmap::Format::RGBA32) {
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
    const GraphicsBitmap& source,
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

    if (source.format() == GraphicsBitmap::Format::RGB32 || source.format() == GraphicsBitmap::Format::RGBA32) {
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

void Painter::blit_with_alpha(const Point& position, const GraphicsBitmap& source, const Rect& src_rect)
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

void Painter::blit(const Point& position, const GraphicsBitmap& source, const Rect& src_rect, float opacity)
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

    if (source.format() == GraphicsBitmap::Format::RGB32 || source.format() == GraphicsBitmap::Format::RGBA32) {
        const RGBA32* src = source.scanline(src_rect.top() + first_row) + src_rect.left() + first_column;
        const size_t src_skip = source.pitch() / sizeof(RGBA32);
        for (int row = first_row; row <= last_row; ++row) {
            fast_u32_copy(dst, src, clipped_rect.width());
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    if (source.format() == GraphicsBitmap::Format::Indexed8) {
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
ALWAYS_INLINE static void do_draw_integer_scaled_bitmap(GraphicsBitmap& target, const Rect& dst_rect, const GraphicsBitmap& source, int hfactor, int vfactor, GetPixel get_pixel)
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
ALWAYS_INLINE static void do_draw_scaled_bitmap(GraphicsBitmap& target, const Rect& dst_rect, const Rect& clipped_rect, const GraphicsBitmap& source, const Rect& src_rect, int hscale, int vscale, GetPixel get_pixel)
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

void Painter::draw_scaled_bitmap(const Rect& a_dst_rect, const GraphicsBitmap& source, const Rect& src_rect)
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
        case GraphicsBitmap::Format::RGB32:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGB32>);
            break;
        case GraphicsBitmap::Format::RGBA32:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGBA32>);
            break;
        case GraphicsBitmap::Format::Indexed8:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Indexed8>);
            break;
        default:
            do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Invalid>);
            break;
        }
    } else {
        switch (source.format()) {
        case GraphicsBitmap::Format::RGB32:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGB32>);
            break;
        case GraphicsBitmap::Format::RGBA32:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGBA32>);
            break;
        case GraphicsBitmap::Format::Indexed8:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Indexed8>);
            break;
        default:
            do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Invalid>);
            break;
        }
    }
}

[[gnu::flatten]] void Painter::draw_glyph(const Point& point, char ch, Color color)
{
    draw_glyph(point, ch, font(), color);
}

[[gnu::flatten]] void Painter::draw_glyph(const Point& point, char ch, const Font& font, Color color)
{
    draw_bitmap(point, font.glyph_bitmap(ch), color);
}

void Painter::draw_emoji(const Point& point, const GraphicsBitmap& emoji, const Font& font)
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
    if (codepoint < 256) {
        // This looks like a regular character.
        draw_glyph(point, (char)codepoint, font, color);
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

void Painter::draw_text(const Rect& rect, const StringView& text, TextAlignment alignment, Color color, TextElision elision)
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
    Rect bounding_rect { 0, 0, 0, (lines.size() * line_height) - line_spacing };

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

    for (int i = 0; i < lines.size(); ++i) {
        auto& line = lines[i];
        Rect line_rect { bounding_rect.x(), bounding_rect.y() + i * line_height, bounding_rect.width(), line_height };
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

[[gnu::always_inline]] inline void Painter::set_pixel_with_draw_op(u32& pixel, const Color& color)
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
    fill_rect(rect, color);
}

void Painter::draw_line(const Point& p1, const Point& p2, Color color, int thickness)
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
        for (int y = min_y; y <= max_y; ++y)
            draw_pixel({ x, y }, color, thickness);
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
        for (int x = min_x; x <= max_x; ++x)
            draw_pixel({ x, y }, color, thickness);
        return;
    }

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
