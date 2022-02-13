/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGUI/Window.h>
#include <LibGfx/Rect.h>
#include <SDL.h>

namespace GUI {

class SDLServer : public Core::Object {
    C_OBJECT(SDLServer)
public:
    static SDLServer& the();
    SDLServer();

    SDL_Window* window(u32 window_id) const { return m_windows.get(window_id).value(); }
    i32 get_window_from_sdl_id(u32);

    void quit();

    void register_window(i32, SDL_Window*);
    void deregister_window(i32);

    void set_window_title(i32, String);
    String get_window_title(i32);

    void set_window_rect(i32 window_id, Gfx::IntRect const& rect);
    Gfx::IntRect get_window_rect(i32 window_id);

    void set_window_resize_aspect_ratio(i32 window_id, Optional<Gfx::IntSize> const& resize_aspect_ratio);

private:
    RefPtr<Core::Timer> m_process_loop;
    HashMap<i32, SDL_Window*> m_windows;
};

}
