#include "FrameBuffer.h"
#include "GraphicsBitmap.h"
#include <AK/Assertions.h>

FrameBuffer* s_the;

void FrameBuffer::initialize()
{
    s_the = nullptr;
}

FrameBuffer& FrameBuffer::the()
{
    ASSERT(s_the);
    return *s_the;
}

FrameBuffer::FrameBuffer(unsigned width, unsigned height)
    : AbstractScreen(width, height)
{
    ASSERT(!s_the);
    s_the = this;
#ifdef USE_SDL
    initializeSDL();
#endif
}

FrameBuffer::FrameBuffer(RGBA32* data, unsigned width, unsigned height)
    : AbstractScreen(width, height)
#ifdef SERENITY
    , m_data(data)
#endif
{
    ASSERT(!s_the);
    s_the = this;
}


FrameBuffer::~FrameBuffer()
{
#ifdef USE_SDL
    SDL_DestroyWindow(m_window);
    m_surface = nullptr;
    m_window = nullptr;
    SDL_Quit();
#endif
}

void FrameBuffer::show()
{
}

#ifdef USE_SDL
void FrameBuffer::initializeSDL()
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
#endif

RGBA32* FrameBuffer::scanline(int y)
{
#ifdef USE_SDL
    return reinterpret_cast<RGBA32*>(((byte*)m_surface->pixels) + (y * m_surface->pitch));
#endif
#ifdef SERENITY
    unsigned pitch = sizeof(RGBA32) * width();
    return reinterpret_cast<RGBA32*>(((byte*)m_data) + (y * pitch));
#endif
}

void FrameBuffer::blit(const Point& position, GraphicsBitmap& bitmap)
{
    Rect dst_rect(position, bitmap.size());
    //printf("blit at %d,%d %dx%d\n", dst_rect.x(), dst_rect.y(), dst_rect.width(), dst_rect.height());
    dst_rect.intersect(rect());
    //printf("    -> intersection %d,%d %dx%d\n", dst_rect.x(), dst_rect.y(), dst_rect.width(), dst_rect.height());

    for (int y = 0; y < dst_rect.height(); ++y) {
        auto* framebuffer_scanline = scanline(position.y() + y);
        auto* bitmap_scanline = bitmap.scanline(y);
        memcpy(framebuffer_scanline + dst_rect.x(), bitmap_scanline + (dst_rect.x() - position.x()), dst_rect.width() * 4);
    }
}

void FrameBuffer::flush()
{
#ifdef USE_SDL
    SDL_UpdateWindowSurface(m_window);
#endif
}
