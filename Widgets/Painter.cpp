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

void Painter::fillRect(Rect rect, Color color)
{
    rect.moveBy(m_widget.x(), m_widget.y());

    SDL_Rect sdlRect;
    sdlRect.x = rect.x();
    sdlRect.y = rect.y();
    sdlRect.w = rect.width();
    sdlRect.h = rect.height();

    int rc = SDL_FillRect(FrameBufferSDL::the().surface(), &sdlRect, color.value());
    ASSERT(rc == 0);
}
