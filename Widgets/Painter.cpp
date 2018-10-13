#include "Painter.h"
#include "FrameBufferSDL.h"
#include "Widget.h"
#include "Font.h"
#include "Window.h"
#include <AK/Assertions.h>
#include <AK/StdLib.h>
#include <SDL.h>

Painter::Painter(Widget& widget)
    : m_widget(widget)
    , m_font(Font::defaultFont())
{
    if (auto* window = widget.window()) {
        m_translation = window->position();
        m_translation.moveBy(widget.relativePosition());
    } else {
        m_translation.setX(widget.x());
        m_translation.setY(widget.y());
    }

    m_clipRect.setWidth(AbstractScreen::the().width());
    m_clipRect.setHeight(AbstractScreen::the().height());
}

Painter::~Painter()
{
    int rc = SDL_UpdateWindowSurface(FrameBufferSDL::the().window());
    ASSERT(rc == 0);
}

static dword* scanline(int y)
{
    auto& surface = *FrameBufferSDL::the().surface();
    return (dword*)(((byte*)surface.pixels) + (y * surface.pitch));
}

void Painter::fillRect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = max(r.top(), m_clipRect.top()); y < min(r.bottom(), m_clipRect.bottom()); ++y) {
        dword* bits = scanline(y);
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
        dword* bits = scanline(y);
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

void Painter::xorRect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = max(r.top(), m_clipRect.top()); y < min(r.bottom(), m_clipRect.bottom()); ++y) {
        dword* bits = scanline(y);
        if (y == r.top() || y == (r.bottom() - 1)) {
            for (int x = max(r.left(), m_clipRect.left()); x < min(r.right(), m_clipRect.right()); ++x) {
                bits[x] ^= color.value();
            }
        } else {
            if (r.left() >= m_clipRect.left() && r.left() < m_clipRect.right())
                bits[r.left()] ^= color.value();
            if (r.right() >= m_clipRect.left() && r.right() < m_clipRect.right())
                bits[r.right() - 1] ^= color.value();
        }
    }
}

void Painter::drawBitmap(const Point& p, const CBitmap& bitmap, Color color)
{
    Point point = p;
    point.moveBy(m_translation);
    for (unsigned row = 0; row < bitmap.height(); ++row) {
        int y = point.y() + row;
        if (y < m_clipRect.top() || y >= m_clipRect.bottom())
            break;
        dword* bits = scanline(y);
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
    } else if (alignment == TextAlignment::Center) {
        int textWidth = text.length() * m_font.glyphWidth();
        point = rect.center();
        point.moveBy(-(textWidth / 2), -(m_font.glyphHeight() / 2));
    } else {
        ASSERT_NOT_REACHED();
    }

    for (unsigned i = 0; i < text.length(); ++i) {
        byte ch = text[i];
        if (ch == ' ')
            continue;
        auto* bitmap = m_font.glyphBitmap(ch);
        if (!bitmap) {
            printf("Font doesn't have 0x%02x ('%c')\n", ch, ch);
            ASSERT_NOT_REACHED();
        }
        int x = point.x() + i * m_font.glyphWidth();
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
    scanline(point.y())[point.x()] = color.value();
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
            std::swap(point1, point2);
        for (int y = max(point1.y(), m_clipRect.top()); y <= min(point2.y(), m_clipRect.bottom()); ++y)
            scanline(y)[x] = color.value();
        return;
    }

    if (point1.x() > point2.x())
        std::swap(point1, point2);

    // Special case: horizontal line.
    if (point1.y() == point2.y()) {
        const int y = point1.y();
        if (y < m_clipRect.top() || y >= m_clipRect.bottom())
            return;
        if (point1.x() > point2.x())
            std::swap(point1, point2);
        auto* pixels = scanline(point1.y());
        for (int x = max(point1.x(), m_clipRect.left()); x <= min(point2.x(), m_clipRect.right()); ++x)
            pixels[x] = color.value();
        return;
    }

    // FIXME: Implement clipping below.
    ASSERT_NOT_REACHED();

    const double dx = point2.x() - point1.x();
    const double dy = point2.y() - point1.y();
    const double deltaError = fabs(dy / dx);
    double error = 0;
    const double yStep = dy == 0 ? 0 : (dy > 0 ? 1 : -1);

    int y = point1.y();
    for (int x = point1.x(); x <= point2.x(); ++x) {
        scanline(y)[x] = color.value();
        error += deltaError;
        if (error >= 0.5) {
            y = (double)y + yStep;
            error -= 1.0;
        }
    }
}

void Painter::drawFocusRect(const Rect& rect)
{
    Rect focusRect = rect;
    focusRect.moveBy(1, 1);
    focusRect.setWidth(focusRect.width() - 2);
    focusRect.setHeight(focusRect.height() - 2);
    drawRect(focusRect, Color(96, 96, 192));
}
