#include "Painter.h"
#include "FrameBufferSDL.h"
#include "Widget.h"
#include <AK/Assertions.h>
#include <SDL.h>

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

const char* peanut8x7[] {
    "  ####  "
    " #    # "
    " #    # "
    " ###### "
    " #    # "
    " #    # "
    "###  ###",

    " #####  "
    " #    # "
    " #    # "
    " ###### "
    " #    # "
    " #    # "
    " #####  ",

    "  ####  "
    " #    # "
    " #      "
    " #      "
    " #      "
    " #    # "
    "  ####  ",

    " #####  "
    " #    # "
    " #    # "
    " #    # "
    " #    # "
    " #    # "
    " #####  ",

    " ###### "
    " #    # "
    " #      "
    " ####   "
    " #      "
    " #    # "
    " ###### ",

    " ###### "
    " #    # "
    " #      "
    " ####   "
    " #      "
    " #      "
    " #      ",

    "  ####  "
    " #    # "
    " #      "
    " #   ###"
    " #    # "
    " #    # "
    "  ####  ",

    " #    # "
    " #    # "
    " #    # "
    " ###### "
    " #    # "
    " #    # "
    " #    # ",

    "  ###   "
    "   #    "
    "   #    "
    "   #    "
    "   #    "
    "   #    "
    "  ###   ",

    "  ##### "
    " #   #  "
    "     #  "
    "     #  "
    "     #  "
    " #   #  "
    "  ###   ",

    " ### ###"
    "  #   # "
    "  #  #  "
    "  ###   "
    "  #  #  "
    "  #   # "
    " ### ###",

    " ###    "
    "  #     "
    "  #     "
    "  #     "
    "  #     "
    "  #   # "
    " ###### ",

    " #    # "
    " ##  ## "
    " # ## # "
    " #    # "
    " #    # "
    " #    # "
    "###  ###",

    " #    # "
    " ##   # "
    " # #  # "
    " #  # # "
    " #   ## "
    " #    # "
    " #    # ",

    "  ####  "
    " #    # "
    " #    # "
    " #    # "
    " #    # "
    " #    # "
    "  ####  ",

    " #####  "
    "  #   # "
    "  #   # "
    "  ####  "
    "  #     "
    "  #     "
    " ###    ",

    "  ####  "
    " #    # "
    " #    # "
    " #    # "
    " #  # # "
    " #   #  "
    "  ### # ",

    " #####  "
    "  #   # "
    "  #   # "
    "  ####  "
    "  # #   "
    "  #  #  "
    " ###  # ",

    "  ####  "
    " #    # "
    " #      "
    "  ####  "
    "      # "
    " #    # "
    "  ####  ",

    " #####  "
    "#  #  # "
    "   #    "
    "   #    "
    "   #    "
    "   #    "
    "  ###   ",

    "###  ###"
    " #    # "
    " #    # "
    " #    # "
    " #    # "
    " #    # "
    "  ####  ",

    "### ### "
    " #   #  "
    " #   #  "
    "  # #   "
    "  # #   "
    "   #    "
    "   #    ",

    "###  ###"
    " #    # "
    " #    # "
    " #    # "
    " # ## # "
    " ##  ## "
    " #    # ",

    "##   ## "
    " #   #  "
    "  # #   "
    "   #    "
    "  # #   "
    " #   #  "
    "##   ## ",

    "##   ## "
    " #   #  "
    " #   #  "
    "  # #   "
    "   #    "
    "   #    "
    "  ###   ",

    " ###### "
    " #   #  "
    "    #   "
    "   #    "
    "  #     "
    " #    # "
    " ###### ",

    "  ####  "
    "  #     "
    "  #     "
    "  #     "
    "  #     "
    "  #     "
    "  ####  ",

    " #      "
    " ##     "
    "  ##    "
    "   ##   "
    "    ##  "
    "     ## "
    "      # ",

    "  ####  "
    "     #  "
    "     #  "
    "     #  "
    "     #  "
    "     #  "
    "  ####  ",

};

void Painter::drawText(const Point& point, const String& text, const Color& color)
{
    Point p = point;
    p.moveBy(m_widget.x(), m_widget.y());

    byte fontWidth = 8;
    byte fontHeight = 7;
    auto* font = peanut8x7;

    for (int row = 0; row < fontHeight; ++row) {
        int y = p.y() + row;
        dword* bits = scanline(y);
        for (unsigned i = 0; i < text.length(); ++i) {
            const char* fontCharacter = font[text[i] - 'A'];
            int x = p.x() + i * fontWidth;
            for (unsigned j = 0; j < fontWidth; ++j) {
                char fc = fontCharacter[row * fontWidth + j];
                if (fc == '#')
                    bits[x + j] = color.value();
            }
        }
    }
}

