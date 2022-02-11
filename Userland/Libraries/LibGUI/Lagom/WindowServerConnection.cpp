/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
#include <AK/Singleton.h>
#include <LibGUI/Lagom/WindowServerConnection.h>
#include <LibGUI/Window.h>
#include <SDL.h>

namespace GUI {

static Singleton<WindowServerConnection> s_the;
WindowServerConnection& WindowServerConnection::the()
{
    return *s_the;
}

WindowServerConnection::WindowServerConnection() {
    SDL_Init(SDL_INIT_VIDEO);
    dbgln("!! init");
}

void WindowServerConnection::async_create_window(i32 window_id, Gfx::IntRect const& rect,
    bool auto_position, bool has_alpha_channel, bool modal, bool minimizable, bool closeable, bool resizable,
    bool fullscreen, bool frameless, bool forced_shadow, bool accessory, float opacity,
    float alpha_hit_threshold, Gfx::IntSize const& base_size, Gfx::IntSize const& size_increment,
    Gfx::IntSize const& minimum_size, Optional<Gfx::IntSize> const& resize_aspect_ratio, i32 type,
    String const& title, i32 parent_window_id, Gfx::IntRect const& launch_origin_rect)
{
    SDL_Window* window = SDL_CreateWindow(title.characters(), SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, rect.width(), rect.height(), SDL_WINDOW_SHOWN);
    SDL_Surface* screenSurface = NULL;
    screenSurface = SDL_GetWindowSurface(window);
    SDL_FillRect(screenSurface, NULL, SDL_MapRGB(screenSurface->format, 0xFF, 0xFF, 0xFF));

    SDL_UpdateWindowSurface(window);
}

}
#pragma GCC diagnostic pop
