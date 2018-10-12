#include "Painter.h"
#include "FrameBufferSDL.h"
#include "Widget.h"
#include "Font.h"
#include "Window.h"
#include <AK/Assertions.h>
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

    for (int y = r.top(); y < r.bottom(); ++y) {
        dword* bits = scanline(y);
        for (int x = r.left(); x < r.right(); ++x) {
            bits[x] = color.value();
        }
    }
}

void Painter::drawRect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = r.top(); y < r.bottom(); ++y) {
        dword* bits = scanline(y);
        if (y == r.top() || y == (r.bottom() - 1)) {
            for (int x = r.left(); x < r.right(); ++x) {
                bits[x] = color.value();
            }
        } else {
            bits[r.left()] = color.value();
            bits[r.right() - 1] = color.value();
        }
    }
}

void Painter::xorRect(const Rect& rect, Color color)
{
    Rect r = rect;
    r.moveBy(m_translation);

    for (int y = r.top(); y < r.bottom(); ++y) {
        dword* bits = scanline(y);
        if (y == r.top() || y == (r.bottom() - 1)) {
            for (int x = r.left(); x < r.right(); ++x) {
                bits[x] ^= color.value();
            }
        } else {
            bits[r.left()] ^= color.value();
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
        int x = point.x();
        dword* bits = scanline(y);
        for (unsigned j = 0; j < bitmap.width(); ++j) {
            char fc = bitmap.bits()[row * bitmap.width() + j];
            if (fc == '#')
                bits[x + j] = color.value();
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
        point.moveBy(-(textWidth / 2), -(m_font.glyphWidth() / 2));
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

