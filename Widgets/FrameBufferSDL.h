#pragma once

#include "AbstractScreen.h"
#include <SDL.h>

class FrameBufferSDL final : public AbstractScreen {
public:
    FrameBufferSDL(unsigned width, unsigned height);
    virtual ~FrameBufferSDL() override;

    void show();

private:
    void initializeSDL();

    SDL_Window* m_window { nullptr };
    SDL_Surface* m_surface { nullptr };
};

