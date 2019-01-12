#include "Painter.h"
#include "Widget.h"
#include "Font.h"
#include "Window.h"
#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>

#define DEBUG_WIDGET_UNDERDRAW

Painter::Painter(GraphicsBitmap& bitmap)
{
    m_font = &Font::defaultFont();
    m_target = &bitmap;
    m_clip_rect = { { 0, 0 }, bitmap.size() };
}

Painter::Painter(Widget& widget)
    : m_font(&widget.font())
{
    m_target = widget.backing();
    ASSERT(m_target);
    m_window = widget.window();
    m_translation.moveBy(widget.relativePosition());
    // NOTE: m_clip_rect is in Window coordinates since we are painting into its backing store.
    m_clip_rect = widget.relativeRect();

#ifdef DEBUG_WIDGET_UNDERDRAW
    fill_rect(widget.rect(), Color::Red);
#endif
}

Painter::~Painter()
{
}

void Painter::fill_rect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = max(r.top(), m_clip_rect.top()); y <= min(r.bottom(), m_clip_rect.bottom()); ++y) {
        auto* bits = m_target->scanline(y);
        for (int x = max(r.left(), m_clip_rect.left()); x <= min(r.right(), m_clip_rect.right()); ++x) {
            bits[x] = color.value();
        }
    }
}

void Painter::draw_rect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = max(r.top(), m_clip_rect.top()); y <= min(r.bottom(), m_clip_rect.bottom()); ++y) {
        auto* bits = m_target->scanline(y);
        if (y == r.top() || y == r.bottom()) {
            for (int x = max(r.left(), m_clip_rect.left()); x <= min(r.right(), m_clip_rect.right()); ++x) {
                bits[x] = color.value();
            }
        } else {
            if (r.left() >= m_clip_rect.left() && r.left() < m_clip_rect.right())
                bits[r.left()] = color.value();
            if (r.right() >= m_clip_rect.left() && r.right() < m_clip_rect.right())
                bits[r.right()] = color.value();
        }
    }
}

void Painter::draw_bitmap(const Point& p, const CharacterBitmap& bitmap, Color color)
{
    Point point = p;
    point.moveBy(m_translation);
    for (unsigned row = 0; row < bitmap.height(); ++row) {
        int y = point.y() + row;
        if (y < m_clip_rect.top() || y > m_clip_rect.bottom())
            break;
        auto* bits = m_target->scanline(y);
        for (unsigned j = 0; j < bitmap.width(); ++j) {
            int x = point.x() + j;
            if (x < m_clip_rect.left() || x > m_clip_rect.right())
                break;
            char fc = bitmap.bits()[row * bitmap.width() + j];
            if (fc == '#')
                bits[x] = color.value();
        }
    }
}

void Painter::draw_text(const Rect& rect, const String& text, TextAlignment alignment, Color color)
{
    Point point;
    
    if (alignment == TextAlignment::TopLeft) {
        point = rect.location();
    } else if (alignment == TextAlignment::CenterLeft) {
        point = { rect.x(), rect.center().y() - (font().glyphHeight() / 2) };
    } else if (alignment == TextAlignment::Center) {
        int textWidth = text.length() * font().glyphWidth();
        point = rect.center();
        point.moveBy(-(textWidth / 2), -(font().glyphHeight() / 2));
    } else {
        ASSERT_NOT_REACHED();
    }

    for (unsigned i = 0; i < text.length(); ++i) {
        byte ch = text[i];
        if (ch == ' ')
            continue;
        auto* bitmap = font().glyphBitmap(ch);
        if (!bitmap) {
            printf("Font doesn't have 0x%02x ('%c')\n", ch, ch);
            ASSERT_NOT_REACHED();
        }
        int x = point.x() + i * font().glyphWidth();
        int y = point.y();
        draw_bitmap({ x, y }, *bitmap, color);
    }
}

void Painter::set_pixel(const Point& p, Color color)
{
    auto point = p;
    point.moveBy(m_translation);
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
    point1.moveBy(m_translation);

    auto point2 = p2;
    point2.moveBy(m_translation);

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
        for (int x = min_x; x <= max_x; ++x)
            set_pixel_with_draw_op(pixels[x], color);
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
    focus_rect.moveBy(1, 1);
    focus_rect.setWidth(focus_rect.width() - 2);
    focus_rect.setHeight(focus_rect.height() - 2);
    draw_rect(focus_rect, Color(96, 96, 192));
}

void Painter::blit(const Point& position, const GraphicsBitmap& source)
{
    Rect dst_rect(position, source.size());
    dst_rect.intersect(m_clip_rect);

    for (int y = 0; y < dst_rect.height(); ++y) {
        auto* dst_scanline = m_target->scanline(position.y() + y);
        auto* src_scanline = source.scanline(y);
        memcpy(dst_scanline + dst_rect.x(), src_scanline + (dst_rect.x() - position.x()), dst_rect.width() * 4);
    }
}
