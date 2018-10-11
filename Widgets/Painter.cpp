#include "Painter.h"
#include "FrameBufferSDL.h"
#include "Widget.h"
#include "Font.h"
#include <AK/Assertions.h>
#include <SDL.h>

Painter::Painter(Widget& widget)
    : m_widget(widget)
    , m_font(Font::defaultFont())
{
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
    r.moveBy(m_widget.x(), m_widget.y());

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
    r.moveBy(m_widget.x(), m_widget.y());

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

void Painter::drawText(const Rect& rect, const String& text, TextAlignment alignment, const Color& color)
{
    Point point;
    
    if (alignment == TextAlignment::TopLeft) {
        point = rect.location();
        point.moveBy(m_widget.x(), m_widget.y());
    } else if (alignment == TextAlignment::Center) {
        int textWidth = text.length() * m_font.glyphWidth();
        point = rect.center();
        point.moveBy(-(textWidth / 2), -(m_font.glyphWidth() / 2));
        point.moveBy(m_widget.x(), m_widget.y());
    } else {
        ASSERT_NOT_REACHED();
    }

    for (int row = 0; row < m_font.glyphHeight(); ++row) {
        int y = point.y() + row;
        dword* bits = scanline(y);
        for (unsigned i = 0; i < text.length(); ++i) {
            byte ch = text[i];
            if (ch == ' ')
                continue;
            const char* glyph = m_font.glyph(ch);
            if (!ch) {
                printf("Font doesn't have 0x%02x ('%c')\n", ch, ch);
                ASSERT_NOT_REACHED();
            }
            int x = point.x() + i * m_font.glyphWidth();
            for (unsigned j = 0; j < m_font.glyphWidth(); ++j) {
                char fc = glyph[row * m_font.glyphWidth() + j];
                if (fc == '#')
                    bits[x + j] = color.value();
            }
        }
    }
}

