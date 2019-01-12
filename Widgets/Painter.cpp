#include "Painter.h"
#include "FrameBuffer.h"
#include "Widget.h"
#include "Font.h"
#include "Window.h"
#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>

Painter::Painter(GraphicsBitmap& bitmap)
{
    m_font = &Font::defaultFont();
    m_target = &bitmap;
    m_clipRect = { { 0, 0 }, bitmap.size() };
}

Painter::Painter(Widget& widget)
    : m_font(&widget.font())
{
    m_target = widget.backing();
    ASSERT(m_target);
    m_window = widget.window();
    m_translation.moveBy(widget.relativePosition());
    m_clipRect.setWidth(AbstractScreen::the().width());
    m_clipRect.setHeight(AbstractScreen::the().height());
}

Painter::~Painter()
{
}

void Painter::fillRect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = max(r.top(), m_clipRect.top()); y < min(r.bottom(), m_clipRect.bottom()); ++y) {
        auto* bits = m_target->scanline(y);
        for (int x = max(r.left(), m_clipRect.left()); x < min(r.right(), m_clipRect.right()); ++x) {
            bits[x] = color.value();
        }
    }
}

void Painter::drawRect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = max(r.top(), m_clipRect.top()); y < min(r.bottom(), m_clipRect.bottom()); ++y) {
        auto* bits = m_target->scanline(y);
        if (y == r.top() || y == (r.bottom() - 1)) {
            for (int x = max(r.left(), m_clipRect.left()); x < min(r.right(), m_clipRect.right()); ++x) {
                bits[x] = color.value();
            }
        } else {
            if (r.left() >= m_clipRect.left() && r.left() < m_clipRect.right())
                bits[r.left()] = color.value();
            if (r.right() >= m_clipRect.left() && r.right() < m_clipRect.right())
                bits[r.right() - 1] = color.value();
        }
    }
}

void Painter::drawBitmap(const Point& p, const CharacterBitmap& bitmap, Color color)
{
    Point point = p;
    point.moveBy(m_translation);
    for (unsigned row = 0; row < bitmap.height(); ++row) {
        int y = point.y() + row;
        if (y < m_clipRect.top() || y >= m_clipRect.bottom())
            break;
        auto* bits = m_target->scanline(y);
        for (unsigned j = 0; j < bitmap.width(); ++j) {
            int x = point.x() + j;
            if (x < m_clipRect.left() || x >= m_clipRect.right())
                break;
            char fc = bitmap.bits()[row * bitmap.width() + j];
            if (fc == '#')
                bits[x] = color.value();
        }
    }
}

void Painter::drawText(const Rect& rect, const String& text, TextAlignment alignment, Color color)
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
        drawBitmap({ x, y }, *bitmap, color);
    }
}

void Painter::drawPixel(const Point& p, Color color)
{
    auto point = p;
    point.moveBy(m_translation);
    if (!m_clipRect.contains(point))
        return;
    m_target->scanline(point.y())[point.x()] = color.value();
}

void Painter::set_pixel_with_draw_op(dword& pixel, const Color& color)
{
    if (m_draw_op == DrawOp::Copy)
        pixel = color.value();
    else if (m_draw_op == DrawOp::Xor)
        pixel ^= color.value();
}

void Painter::drawLine(const Point& p1, const Point& p2, Color color)
{
    auto point1 = p1;
    point1.moveBy(m_translation);

    auto point2 = p2;
    point2.moveBy(m_translation);

    // Special case: vertical line.
    if (point1.x() == point2.x()) {
        const int x = point1.x();
        if (x < m_clipRect.left() || x >= m_clipRect.right())
            return;
        if (point1.y() > point2.y())
            swap(point1, point2);
        for (int y = max(point1.y(), m_clipRect.top()); y <= min(point2.y(), m_clipRect.bottom()); ++y)
            set_pixel_with_draw_op(m_target->scanline(y)[x], color);
        return;
    }

    if (point1.x() > point2.x())
        swap(point1, point2);

    // Special case: horizontal line.
    if (point1.y() == point2.y()) {
        const int y = point1.y();
        if (y < m_clipRect.top() || y >= m_clipRect.bottom())
            return;
        if (point1.x() > point2.x())
            swap(point1, point2);
        auto* pixels = m_target->scanline(point1.y());
        for (int x = max(point1.x(), m_clipRect.left()); x <= min(point2.x(), m_clipRect.right()); ++x)
            set_pixel_with_draw_op(pixels[x], color);
        return;
    }

    // FIXME: Implement clipping below.
    ASSERT_NOT_REACHED();

#if 0
    const double dx = point2.x() - point1.x();
    const double dy = point2.y() - point1.y();
    const double deltaError = fabs(dy / dx);
    double error = 0;
    const double yStep = dy == 0 ? 0 : (dy > 0 ? 1 : -1);

    int y = point1.y();
    for (int x = point1.x(); x <= point2.x(); ++x) {
        m_target->scanline(y)[x] = color.value();
        error += deltaError;
        if (error >= 0.5) {
            y = (double)y + yStep;
            error -= 1.0;
        }
    }
#endif
}

void Painter::drawFocusRect(const Rect& rect)
{
    Rect focusRect = rect;
    focusRect.moveBy(1, 1);
    focusRect.setWidth(focusRect.width() - 2);
    focusRect.setHeight(focusRect.height() - 2);
    drawRect(focusRect, Color(96, 96, 192));
}

void Painter::blit(const Point& position, const GraphicsBitmap& source)
{
    Rect dst_rect(position, source.size());
    dst_rect.intersect(m_clipRect);

    for (int y = 0; y < dst_rect.height(); ++y) {
        auto* dst_scanline = m_target->scanline(position.y() + y);
        auto* src_scanline = source.scanline(y);
        memcpy(dst_scanline + dst_rect.x(), src_scanline + (dst_rect.x() - position.x()), dst_rect.width() * 4);
    }
}
