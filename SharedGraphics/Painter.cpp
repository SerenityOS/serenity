#include "Painter.h"
#include "Font.h"
#include "GraphicsBitmap.h"
#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>

#ifdef LIBGUI
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibC/gui.h>
#include <LibC/stdio.h>
#endif

#define DEBUG_WIDGET_UNDERDRAW

Painter::Painter(GraphicsBitmap& bitmap)
{
    m_font = &Font::default_font();
    m_target = &bitmap;
    m_clip_rect = { { 0, 0 }, bitmap.size() };
}

#ifdef LIBGUI
Painter::Painter(GWidget& widget)
    : m_font(&widget.font())
{
    GUI_WindowBackingStoreInfo backing;
    int rc = gui_get_window_backing_store(widget.window()->window_id(), &backing);
    if (rc < 0) {
        perror("gui_get_window_backing_store");
        exit(1);
    }
    m_backing_store_id = backing.backing_store_id;
    m_target = GraphicsBitmap::create_wrapper(backing.size, backing.pixels);
    ASSERT(m_target);
    m_window = widget.window();
    m_translation.move_by(widget.relative_position());
    // NOTE: m_clip_rect is in Window coordinates since we are painting into its backing store.
    m_clip_rect = widget.relative_rect();
    m_clip_rect.intersect(m_target->rect());

#ifdef DEBUG_WIDGET_UNDERDRAW
    // If the widget is not opaque, let's not mess it up with debugging color.
    if (widget.fill_with_background_color() && m_window->main_widget() != &widget)
        fill_rect(widget.rect(), Color::Red);
#endif
}
#endif

Painter::~Painter()
{
#ifdef LIBGUI
    m_target = nullptr;
    int rc = gui_release_window_backing_store(m_backing_store_id);
    ASSERT(rc == 0);
#endif
}

void Painter::fill_rect(const Rect& a_rect, Color color)
{
    auto rect = a_rect;
    rect.move_by(m_translation);
    rect.intersect(m_clip_rect);

    if (rect.is_empty())
        return;

    RGBA32* dst = m_target->scanline(rect.top()) + rect.left();
    const unsigned dst_skip = m_target->width();

    for (int i = rect.height() - 1; i >= 0; --i) {
        fast_dword_fill(dst, color.value(), rect.width());
        dst += dst_skip;
    }
}

void Painter::draw_rect(const Rect& a_rect, Color color)
{
    Rect rect = a_rect;
    rect.move_by(m_translation);

    auto clipped_rect = Rect::intersection(rect, m_clip_rect);
    if (clipped_rect.is_empty())
        return;

    int min_y = clipped_rect.top();
    int max_y = clipped_rect.bottom();

    if (rect.top() >= clipped_rect.top() && rect.top() <= clipped_rect.bottom()) {
        fast_dword_fill(m_target->scanline(rect.top()) + clipped_rect.left(), color.value(), clipped_rect.width());
        ++min_y;
    }
    if (rect.bottom() >= clipped_rect.top() && rect.bottom() <= clipped_rect.bottom()) {
        fast_dword_fill(m_target->scanline(rect.bottom()) + clipped_rect.left(), color.value(), clipped_rect.width());
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
    Rect rect { p, bitmap.size() };
    rect.move_by(m_translation);
    auto clipped_rect = Rect::intersection(rect, m_clip_rect);
    const int first_row = clipped_rect.top() - rect.top();
    const int last_row = clipped_rect.bottom() - rect.top();
    const int first_column = clipped_rect.left() - rect.left();
    const int last_column = clipped_rect.right() - rect.left();
    RGBA32* dst = m_target->scanline(rect.y() + first_row) + rect.x();
    const size_t dst_skip = m_target->width();
    const char* bitmap_row = &bitmap.bits()[first_row];
    const size_t bitmap_skip = bitmap.width();

    for (int row = first_row; row <= last_row; ++row) {
        for (int j = first_column; j <= last_column; ++j) {
            char fc = bitmap_row[j];
            if (fc == '#')
                dst[j] = color.value();
        }
        bitmap_row += bitmap_skip;
        dst += dst_skip;
    }
}

void Painter::draw_glyph(const Point& point, char ch, Color color)
{
    auto* bitmap = font().glyph_bitmap(ch);
    if (!bitmap) {
        dbgprintf("Font doesn't have 0x%b ('%c')\n", (byte)ch, ch);
        bitmap = font().error_bitmap();
    }
    int x = point.x();
    int y = point.y();
    draw_bitmap({ x, y }, *bitmap, color);
}

void Painter::draw_text(const Rect& rect, const String& text, TextAlignment alignment, Color color)
{
    Point point;
    
    if (alignment == TextAlignment::TopLeft) {
        point = rect.location();
    } else if (alignment == TextAlignment::CenterLeft) {
        point = { rect.x(), rect.center().y() - (font().glyph_height() / 2) };
    } else if (alignment == TextAlignment::CenterRight) {
        int text_width = text.length() * font().glyph_width();
        point = { rect.right() - text_width, rect.center().y() - (font().glyph_height() / 2) };
    } else if (alignment == TextAlignment::Center) {
        int textWidth = text.length() * font().glyph_width();
        point = rect.center();
        point.move_by(-(textWidth / 2), -(font().glyph_height() / 2));
    } else {
        ASSERT_NOT_REACHED();
    }

    for (unsigned i = 0; i < text.length(); ++i, point.move_by(font().glyph_width(), 0)) {
        byte ch = text[i];
        if (ch == ' ')
            continue;
        draw_glyph(point, ch, color);
    }
}

void Painter::set_pixel(const Point& p, Color color)
{
    auto point = p;
    point.move_by(m_translation);
    if (!m_clip_rect.contains(point))
        return;
    m_target->scanline(point.y())[point.x()] = color.value();
}

ALWAYS_INLINE void Painter::set_pixel_with_draw_op(dword& pixel, const Color& color)
{
    if (m_draw_op == DrawOp::Copy)
        pixel = color.value();
    else if (m_draw_op == DrawOp::Xor)
        pixel ^= color.value();
}

void Painter::draw_line(const Point& p1, const Point& p2, Color color)
{
    auto point1 = p1;
    point1.move_by(m_translation);

    auto point2 = p2;
    point2.move_by(m_translation);

    // Special case: vertical line.
    if (point1.x() == point2.x()) {
        const int x = point1.x();
        if (x < m_clip_rect.left() || x > m_clip_rect.right())
            return;
        if (point1.y() > point2.y())
            swap(point1, point2);
        int min_y = max(point1.y(), m_clip_rect.top());
        int max_y = min(point2.y(), m_clip_rect.bottom());
        for (int y = min_y; y <= max_y; ++y)
            set_pixel_with_draw_op(m_target->scanline(y)[x], color);
        return;
    }

    if (point1.x() > point2.x())
        swap(point1, point2);

    // Special case: horizontal line.
    if (point1.y() == point2.y()) {
        const int y = point1.y();
        if (y < m_clip_rect.top() || y > m_clip_rect.bottom())
            return;
        if (point1.x() > point2.x())
            swap(point1, point2);
        int min_x = max(point1.x(), m_clip_rect.left());
        int max_x = min(point2.x(), m_clip_rect.right());
        auto* pixels = m_target->scanline(point1.y());
        if (m_draw_op == DrawOp::Copy) {
            fast_dword_fill(pixels + min_x, color.value(), max_x - min_x + 1);
        } else {
            for (int x = min_x; x <= max_x; ++x)
                set_pixel_with_draw_op(pixels[x], color);
        }
        return;
    }

    // FIXME: Implement clipping below.
    ASSERT_NOT_REACHED();

#if 0
    const double dx = point2.x() - point1.x();
    const double dy = point2.y() - point1.y();
    const double delta_error = fabs(dy / dx);
    double error = 0;
    const double yStep = dy == 0 ? 0 : (dy > 0 ? 1 : -1);

    int y = point1.y();
    for (int x = point1.x(); x <= point2.x(); ++x) {
        m_target->scanline(y)[x] = color.value();
        error += delta_error;
        if (error >= 0.5) {
            y = (double)y + yStep;
            error -= 1.0;
        }
    }
#endif
}

void Painter::draw_focus_rect(const Rect& rect)
{
    Rect focus_rect = rect;
    focus_rect.move_by(1, 1);
    focus_rect.set_width(focus_rect.width() - 2);
    focus_rect.set_height(focus_rect.height() - 2);
    draw_rect(focus_rect, Color(96, 96, 192));
}

void Painter::blit(const Point& position, const GraphicsBitmap& source, const Rect& src_rect)
{
    Rect dst_rect(position, src_rect.size());
    dst_rect.intersect(m_clip_rect);

    RGBA32* dst = m_target->scanline(dst_rect.y()) + dst_rect.x();
    const RGBA32* src = source.scanline(src_rect.top()) + src_rect.left();

    const unsigned dst_skip = m_target->width();
    const unsigned src_skip = source.width();

    for (int i = dst_rect.height() - 1; i >= 0; --i) {
        fast_dword_copy(dst, src, dst_rect.width());
        dst += dst_skip;
        src += src_skip;
    }
}

void Painter::set_clip_rect(const Rect& rect)
{
    m_clip_rect = Rect::intersection(rect, m_target->rect());
}

void Painter::clear_clip_rect()
{
    m_clip_rect = m_target->rect();
}
