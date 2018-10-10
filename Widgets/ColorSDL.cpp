#include "Color.h"
#include "FrameBufferSDL.h"

Color::Color(byte r, byte g, byte b)
{
    m_value = SDL_MapRGB(FrameBufferSDL::the().surface()->format, r, g, b);
}
