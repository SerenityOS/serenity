#include "Painter.h"
#include "Font.h"
#include "GraphicsBitmap.h"
#include <SharedGraphics/CharacterBitmap.h>
#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <unistd.h>
#include <stdio.h>
#include <math.h>

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

    for (int i = rect.height() - 1; i >= 0; --i) {
        fast_dword_fill(dst, color.value(), rect.width());
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

    float increment = (1.0/((rect.width())/255.0));

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
                b1 / 255.0 * c + b2 / 255.0 * (255 - c)
            ).value();
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
        fast_dword_fill(m_target->scanline(rect.top()) + start_x, color.value(), width);
        ++min_y;
    }
    if (rect.bottom() >= clipped_rect.top() && rect.bottom() <= clipped_rect.bottom()) {
        int start_x = rough ? max(rect.x() + 1, clipped_rect.x()) : clipped_rect.x();
        int width = rough ? min(rect.width() - 2, clipped_rect.width()) : clipped_rect.width();
        fast_dword_fill(m_target->scanline(rect.bottom()) + start_x, color.value(), width);
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

void Painter::blit_with_opacity(const Point& position, const GraphicsBitmap& source, const Rect& src_rect, float opacity)
{
    ASSERT(!m_target->has_alpha_channel());

    if (!opacity)
        return;
    if (opacity >= 1.0f)
        return blit(position, source, src_rect);

    byte alpha = 255 * opacity;

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
            byte alpha = Color::from_rgba(src[x]).alpha();
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

void Painter::blit_tiled(const Point& position, const GraphicsBitmap& source, const Rect& src_rect)
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
            const RGBA32* sl = source.scanline((row + src_rect.top())
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
            byte alpha = Color::from_rgba(src[x]).alpha();
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
            fast_dword_copy(dst, src, clipped_rect.width());
            dst += dst_skip;
            src += src_skip;
        }
        return;
    }

    if (source.format() == GraphicsBitmap::Format::Indexed8) {
        const byte* src = source.bits(src_rect.top() + first_row) + src_rect.left() + first_column;
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
        case GraphicsBitmap::Format::RGB32: do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGB32>); break;
        case GraphicsBitmap::Format::RGBA32: do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGB32>); break;
        case GraphicsBitmap::Format::Indexed8: do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Indexed8>); break;
        default: do_draw_scaled_bitmap<true>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Invalid>); break;
        }
    } else {
        switch (source.format()) {
        case GraphicsBitmap::Format::RGB32: do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGB32>); break;
        case GraphicsBitmap::Format::RGBA32: do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::RGB32>); break;
        case GraphicsBitmap::Format::Indexed8: do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Indexed8>); break;
        default: do_draw_scaled_bitmap<false>(*m_target, dst_rect, clipped_rect, source, src_rect, hscale, vscale, get_pixel<GraphicsBitmap::Format::Invalid>); break;
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

void Painter::draw_text(const Rect& rect, const char* text, int length, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    String elided_text;
    if (elision == TextElision::Right) {
        int text_width = font.width(text, length);
        if (font.width(text, length) > rect.width()) {
            int glyph_spacing = font.glyph_spacing();
            int new_length = 0;
            int new_width = font.width("...");
            if (new_width < text_width) {
                for (int i = 0; i < length; ++i) {
                    int glyph_width = font.glyph_width(text[i]);
                    // NOTE: Glyph spacing should not be added after the last glyph on the line,
                    //       but since we are here because the last glyph does not actually fit on the line,
                    //       we don't have to worry about spacing.
                    int width_with_this_glyph_included = new_width + glyph_width + glyph_spacing;
                    if (width_with_this_glyph_included > rect.width())
                        break;
                    ++new_length;
                    new_width += glyph_width + glyph_spacing;
                }
                StringBuilder builder;
                builder.append(text, new_length);
                builder.append("...");
                elided_text = builder.to_string();
                text = elided_text.characters();
                length = elided_text.length();
            }
        }
    }

    Point point;

    if (alignment == TextAlignment::TopLeft) {
        point = rect.location();
    } else if (alignment == TextAlignment::CenterLeft) {
        point = { rect.x(), rect.center().y() - (font.glyph_height() / 2) };
    } else if (alignment == TextAlignment::CenterRight) {
        int text_width = font.width(text);
        point = { rect.right() - text_width, rect.center().y() - (font.glyph_height() / 2) };
    } else if (alignment == TextAlignment::Center) {
        int text_width = font.width(text);
        point = rect.center();
        point.move_by(-(text_width / 2), -(font.glyph_height() / 2));
    } else {
        ASSERT_NOT_REACHED();
    }

    int space_width = font.glyph_width(' ') + font.glyph_spacing();
    for (ssize_t i = 0; i < length; ++i) {
        char ch = text[i];
        if (ch == ' ') {
            point.move_by(space_width, 0);
            continue;
        }
        draw_glyph(point, ch, font, color);
        point.move_by(font.glyph_width(ch) + font.glyph_spacing(), 0);
    }
}

void Painter::draw_text(const Rect& rect, const String& text, TextAlignment alignment, Color color, TextElision elision)
{
    draw_text(rect, text.characters(), text.length(), alignment, color, elision);
}

void Painter::draw_text(const Rect& rect, const String& text, const Font& font, TextAlignment alignment, Color color, TextElision elision)
{
    draw_text(rect, text.characters(), text.length(), font, alignment, color, elision);
}

void Painter::draw_text(const Rect& rect, const char* text, int length, TextAlignment alignment, Color color, TextElision elision)
{
    draw_text(rect, text, length, font(), alignment, color, elision);
}

void Painter::set_pixel(const Point& p, Color color)
{
    auto point = p;
    point.move_by(state().translation);
    if (!clip_rect().contains(point))
        return;
    m_target->scanline(point.y())[point.x()] = color.value();
}

[[gnu::always_inline]] inline void Painter::set_pixel_with_draw_op(dword& pixel, const Color& color)
{
    if (draw_op() == DrawOp::Copy)
        pixel = color.value();
    else if (draw_op() == DrawOp::Xor)
        pixel ^= color.value();
}

void Painter::draw_line(const Point& p1, const Point& p2, Color color)
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
            set_pixel_with_draw_op(m_target->scanline(y)[x], color);
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
        auto* pixels = m_target->scanline(point1.y());
        if (draw_op() == DrawOp::Copy) {
            fast_dword_fill(pixels + min_x, color.value(), max_x - min_x + 1);
        } else {
            for (int x = min_x; x <= max_x; ++x)
                set_pixel_with_draw_op(pixels[x], color);
        }
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
                m_target->scanline(y)[x] = color.value();
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
                m_target->scanline(y)[x] = color.value();
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
