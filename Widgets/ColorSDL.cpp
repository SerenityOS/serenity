#include "Color.h"
#include "FrameBufferSDL.h"

Color::Color(byte r, byte g, byte b)
{
    m_value = SDL_MapRGB(FrameBufferSDL::the().surface()->format, r, g, b);
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

    m_value = SDL_MapRGB(FrameBufferSDL::the().surface()->format, rgb.r, rgb.g, rgb.g);

}
