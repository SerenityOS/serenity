#include "Painter.h"
#include "FrameBufferSDL.h"
#include "Widget.h"
#include <AK/Assertions.h>
#include <SDL.h>
#include "Peanut8x8.h"

Painter::Painter(Widget& widget)
    : m_widget(widget)
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

void Painter::drawText(const Rect& rect, const String& text, TextAlignment alignment, const Color& color)
{
    Point point;
    
    if (alignment == TextAlignment::TopLeft) {
        point = rect.location();
        point.moveBy(m_widget.x(), m_widget.y());;
    } else if (alignment == TextAlignment::Center) {
        int textWidth = text.length() * Peanut8x8::fontWidth;
        point = rect.center();
        point.moveBy(-(textWidth / 2), -(Peanut8x8::fontWidth / 2));
        point.moveBy(m_widget.x(), m_widget.y());
    } else {
        ASSERT_NOT_REACHED();
    }

    for (int row = 0; row < Peanut8x8::fontHeight; ++row) {
        int y = point.y() + row;
        dword* bits = scanline(y);
        for (unsigned i = 0; i < text.length(); ++i) {
            byte ch = text[i];
            if (ch == ' ')
                continue;
            if (ch < Peanut8x8::firstCharacter || ch > Peanut8x8::lastCharacter) {
                printf("Font doesn't have 0x%02x ('%c')\n", ch, ch);
                ASSERT_NOT_REACHED();
            }
            const char* fontCharacter = Peanut8x8::font[ch - Peanut8x8::firstCharacter];
            int x = point.x() + i * Peanut8x8::fontWidth;
            for (unsigned j = 0; j < Peanut8x8::fontWidth; ++j) {
                char fc = fontCharacter[row * Peanut8x8::fontWidth + j];
                if (fc == '#')
                    bits[x + j] = color.value();
            }
        }
    }
}

