#include "FrameBufferSDL.h"
#include <AK/Assertions.h>

FrameBufferSDL::FrameBufferSDL(unsigned width, unsigned height)
    : AbstractScreen(width, height)
{
    initializeSDL();
}

FrameBufferSDL::~FrameBufferSDL()
{
    SDL_DestroyWindow(m_window);
    m_surface = nullptr;
    m_window = nullptr;

    SDL_Quit();
}

void FrameBufferSDL::show()
{
}

void FrameBufferSDL::initializeSDL()
{
    if (m_window)
        return;

    if (SDL_Init(SDL_INIT_VIDEO) < 0) {
        ASSERT_NOT_REACHED();
    }

    m_window = SDL_CreateWindow(
        "FrameBuffer",
        SDL_WINDOWPOS_UNDEFINED, 
        SDL_WINDOWPOS_UNDEFINED, 
        width(),
        height(),
        SDL_WINDOW_SHOWN);

    ASSERT(m_window);

    m_surface = SDL_GetWindowSurface(m_window);
    ASSERT(m_surface);

    SDL_FillRect(m_surface, nullptr, SDL_MapRGB(m_surface->format, 0xff, 0xff, 0xff));

    SDL_UpdateWindowSurface(m_window);
}

