#include "FrameBufferSDL.h"
#include "GraphicsBitmap.h"
#include <AK/Assertions.h>

FrameBufferSDL* s_the = nullptr;

FrameBufferSDL& FrameBufferSDL::the()
{
    ASSERT(s_the);
    return *s_the;
}

FrameBufferSDL::FrameBufferSDL(unsigned width, unsigned height)
    : AbstractScreen(width, height)
{
    ASSERT(!s_the);
    s_the = this;
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

dword* FrameBufferSDL::scanline(int y)
{
    return (dword*)(((byte*)m_surface->pixels) + (y * m_surface->pitch));
}

void FrameBufferSDL::blit(const Point& position, GraphicsBitmap& bitmap)
{
    Rect dst_rect(position, bitmap.size());

    printf("blit at %d,%d %dx%d\n", dst_rect.x(), dst_rect.y(), dst_rect.width(), dst_rect.height());
    dst_rect.intersect(rect());
    printf("    -> intersection %d,%d %dx%d\n", dst_rect.x(), dst_rect.y(), dst_rect.width(), dst_rect.height());

    for (int y = 0; y < dst_rect.height(); ++y) {
        auto* framebuffer_scanline = scanline(position.y() + y);
        auto* bitmap_scanline = bitmap.scanline(y);
        memcpy(framebuffer_scanline + position.x(), bitmap_scanline, dst_rect.width() * 4);
    }
}

void FrameBufferSDL::flush()
{
    SDL_UpdateWindowSurface(m_window);
}
