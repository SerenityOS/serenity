#pragma once

#include "AbstractScreen.h"
#include <SDL.h>

class GraphicsBitmap;

class FrameBufferSDL final : public AbstractScreen {
public:
    FrameBufferSDL(unsigned width, unsigned height);
    virtual ~FrameBufferSDL() override;

    void show();

    SDL_Surface* surface() { return m_surface; }
    SDL_Window* window() { return m_window; }

    static FrameBufferSDL& the();

    dword* scanline(int y);

    void blit(const Point&, GraphicsBitmap&);
    void flush();

private:
    void initializeSDL();

    SDL_Window* m_window { nullptr };
    SDL_Surface* m_surface { nullptr };
};

