#include "Color.h"
#include "FrameBuffer.h"

Color::Color(byte r, byte g, byte b)
{
#ifdef USE_SDL
    m_value = SDL_MapRGB(FrameBuffer::the().surface()->format, r, g, b);
#else
#error FIXME: Implement
#endif
}

Color::Color(NamedColor named)
{
    struct {
        byte r;
        byte g;
        byte b;
    } rgb;

    switch (named) {
    case Black: rgb = { 0, 0, 0 }; break;
    case White: rgb = { 255, 255, 255 }; break;
    case Red: rgb = { 255, 0, 0}; break;
    case Green: rgb = { 0, 255, 0}; break;
    case Blue: rgb = { 0, 0, 255}; break;
    case DarkGray: rgb = { 64, 64, 64 }; break;
    case MidGray: rgb = { 127, 127, 127 }; break;
    case LightGray: rgb = { 192, 192, 192 }; break;
    default: ASSERT_NOT_REACHED(); break;
    }

#ifdef USE_SDL
    m_value = SDL_MapRGB(FrameBuffer::the().surface()->format, rgb.r, rgb.g, rgb.g);
#else
#error FIXME: Implement
#endif
}
