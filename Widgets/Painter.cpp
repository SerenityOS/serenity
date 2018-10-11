#include "Painter.h"
#include "FrameBufferSDL.h"
#include "Widget.h"
#include <AK/Assertions.h>
#include <SDL.h>

#if 0
#include "Peanut8x8.h"
#define FONT_NAMESPACE Peanut8x8
#else
#include "Peanut8x10.h"
#define FONT_NAMESPACE Peanut8x10
#endif

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
        point.moveBy(m_widget.x(), m_widget.y());;
    } else if (alignment == TextAlignment::Center) {
        int textWidth = text.length() * FONT_NAMESPACE::fontWidth;
        point = rect.center();
        point.moveBy(-(textWidth / 2), -(FONT_NAMESPACE::fontWidth / 2));
        point.moveBy(m_widget.x(), m_widget.y());
    } else {
        ASSERT_NOT_REACHED();
    }

    for (int row = 0; row < FONT_NAMESPACE::fontHeight; ++row) {
        int y = point.y() + row;
        dword* bits = scanline(y);
        for (unsigned i = 0; i < text.length(); ++i) {
            byte ch = text[i];
            if (ch == ' ')
                continue;
            if (ch < FONT_NAMESPACE::firstCharacter || ch > FONT_NAMESPACE::lastCharacter) {
                printf("Font doesn't have 0x%02x ('%c')\n", ch, ch);
                ASSERT_NOT_REACHED();
            }
            const char* fontCharacter = FONT_NAMESPACE::font[ch - FONT_NAMESPACE::firstCharacter];
            int x = point.x() + i * FONT_NAMESPACE::fontWidth;
            for (unsigned j = 0; j < FONT_NAMESPACE::fontWidth; ++j) {
                char fc = fontCharacter[row * FONT_NAMESPACE::fontWidth + j];
                if (fc == '#')
                    bits[x + j] = color.value();
            }
        }
    }
}

