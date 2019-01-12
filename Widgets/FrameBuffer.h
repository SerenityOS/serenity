#pragma once

#include "AbstractScreen.h"
#include "Color.h"

#ifdef USE_SDL
#include <SDL.h>
#endif

class GraphicsBitmap;

class FrameBuffer final : public AbstractScreen {
public:
    FrameBuffer(unsigned width, unsigned height);
    FrameBuffer(RGBA32*, unsigned width, unsigned height);
    virtual ~FrameBuffer() override;

    void show();

#ifdef USE_SDL
    SDL_Surface* surface() { return m_surface; }
#endif

    static FrameBuffer& the();

    RGBA32* scanline(int y);

    void flush();

    static void initialize();

private:
#ifdef USE_SDL
    void initializeSDL();
    SDL_Window* m_window { nullptr };
    SDL_Surface* m_surface { nullptr };
#endif
#ifdef SERENITY
    RGBA32* m_data { nullptr };
#endif
};

