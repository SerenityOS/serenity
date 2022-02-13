/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <LibCore/EventLoop.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Event.h>
#include <LibGUI/SDLServer.h>
#include <LibGUI/Window.h>
#include <SDL.h>

namespace GUI {

static Singleton<ConnectionToWindowServer> s_the;
ConnectionToWindowServer& ConnectionToWindowServer::the()
{
    return *s_the;
}

void ConnectionToWindowServer::paint(i32 window_id, Gfx::IntSize const& window_size, Vector<Gfx::IntRect> const& rects)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MultiPaintEvent>(rects, window_size));
}

void ConnectionToWindowServer::window_resized(i32 window_id, Gfx::IntRect const& new_rect)
{
    if (auto* window = Window::from_window_id(window_id)) {
        Core::EventLoop::current().post_event(*window, make<ResizeEvent>(new_rect.size()));
    }
}

static MouseButton to_mouse_button(u32 button)
{
    switch (button) {
    case 0:
        return MouseButton::None;
    case 1:
        return MouseButton::Primary;
    case 2:
        return MouseButton::Secondary;
    case 4:
        return MouseButton::Middle;
    case 8:
        return MouseButton::Backward;
    case 16:
        return MouseButton::Forward;
    default:
        VERIFY_NOT_REACHED();
        break;
    }
}

void ConnectionToWindowServer::mouse_move(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y, bool is_drag, Vector<String> const& mime_types)
{
    if (auto* window = Window::from_window_id(window_id)) {
        if (is_drag)
            Core::EventLoop::current().post_event(*window, make<DragEvent>(Event::DragMove, mouse_position, mime_types));
        else
            Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseMove, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
    }
}

void ConnectionToWindowServer::mouse_down(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDown, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
}

void ConnectionToWindowServer::mouse_up(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseUp, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
}

void ConnectionToWindowServer::async_create_window(i32 window_id, Gfx::IntRect const& rect,
    bool auto_position, bool, bool modal, bool, bool, bool resizable,
    bool fullscreen, bool frameless, bool, bool, float opacity,
    float, Gfx::IntSize const&, Gfx::IntSize const&,
    Gfx::IntSize const& minimum_size, Optional<Gfx::IntSize> const&, i32 type,
    String const& title, i32 parent_window_id, Gfx::IntRect const&)
{
    // FIXME: track child windows
    u32 flags = SDL_WINDOW_SHOWN;

    switch ((GUI::WindowType)type) {
    case GUI::WindowType::Tooltip:
        flags |= SDL_WINDOW_BORDERLESS;
        flags |= SDL_WINDOW_ALWAYS_ON_TOP;
        flags |= SDL_WINDOW_SKIP_TASKBAR;
        flags |= SDL_WINDOW_TOOLTIP;
        break;
    default:
        break;
    }

    if (fullscreen)
        flags |= SDL_WINDOW_FULLSCREEN;

    SDL_Window* window = SDL_CreateWindow(
        title.characters(),
        auto_position ? SDL_WINDOWPOS_CENTERED : rect.x(),
        auto_position ? SDL_WINDOWPOS_CENTERED : rect.y(),
        rect.width(),
        rect.height(),
        flags);

    if (frameless)
        SDL_SetWindowBordered(window, SDL_bool(!frameless));
    if (modal && parent_window_id != 0)
        SDL_SetWindowModalFor(window, SDLServer::the().window(parent_window_id));

    SDL_SetWindowMinimumSize(window, minimum_size.width(), minimum_size.height());
    SDL_SetWindowResizable(window, SDL_bool(resizable));
    SDL_SetWindowOpacity(window, opacity);
    SDL_SetSurfaceRLE(SDL_GetWindowSurface(window), 1);

    SDL_DisplayMode mode {
        SDL_PIXELFORMAT_RGBA32,
        rect.width(),
        rect.height(),
        0,
        nullptr
    };
    SDL_SetWindowDisplayMode(window, &mode);

    SDLServer::the().register_window(window_id, window);
}

Vector<i32>& ConnectionToWindowServer::destroy_window(i32 window_id)
{
    // FIXME: destroy child windows
    SDLServer::the().deregister_window(window_id);

    auto destroyed_window_ids = new Vector<i32>();
    destroyed_window_ids->append(window_id);
    return *destroyed_window_ids;
}

void ConnectionToWindowServer::async_set_window_title(i32 window_id, String const& title)
{
    SDLServer::the().set_window_title(window_id, title);
}

String ConnectionToWindowServer::get_window_title(i32 window_id)
{
    return SDLServer::the().get_window_title(window_id);
}

void ConnectionToWindowServer::async_did_finish_painting(i32 window_id, Vector<Gfx::IntRect> const&)
{
    // TODO: only update the rects that were actually painted
    // as per provided by the second argument

    auto sdl_window = SDLServer::the().window(window_id);
    auto window = Window::from_window_id(window_id);

    auto screen_surface = SDL_GetWindowSurface(sdl_window);

    auto bitmap = window->back_bitmap();
    if (!bitmap)
        return;

    int sdl_width, sdl_height;
    SDL_GetWindowSize(sdl_window, &sdl_width, &sdl_height);

    // If we resize the window fast enough there will be a size mismatch; let's handle that.
    if (bitmap->width() != sdl_width || bitmap->height() != sdl_height) {
        return;
    }

    SDL_LockSurface(screen_surface);
    SDL_memcpy(screen_surface->pixels, bitmap->data(), bitmap->size_in_bytes());
    SDL_UnlockSurface(screen_surface);
    SDL_UpdateWindowSurface(sdl_window);
}

void ConnectionToWindowServer::async_invalidate_rect(i32 window_id, Vector<Gfx::IntRect> const& rects, bool)
{
    auto window = Window::from_window_id(window_id);
    if (!window->is_visible())
        return;

    ConnectionToWindowServer::the().paint(window->window_id(), window->size(), rects);
}

void ConnectionToWindowServer::async_set_forced_shadow(i32, bool)
{
    // NOP
}

void ConnectionToWindowServer::async_refresh_system_theme()
{
    // NOP
}

void ConnectionToWindowServer::async_set_fullscreen(i32 window_id, bool fullscreen)
{
    SDL_SetWindowFullscreen(SDLServer::the().window(window_id), fullscreen ? SDL_WINDOW_FULLSCREEN_DESKTOP : 0);
}

void ConnectionToWindowServer::async_set_frameless(i32 window_id, bool frameless)
{
    SDL_SetWindowBordered(SDLServer::the().window(window_id), SDL_bool(!frameless));
}

void ConnectionToWindowServer::async_set_maximized(i32 window_id, bool maximized)
{
    if (maximized)
        SDL_MaximizeWindow(SDLServer::the().window(window_id));
    else
        SDL_RestoreWindow(SDLServer::the().window(window_id));
}

void ConnectionToWindowServer::async_set_window_opacity(i32 window_id, float opacity)
{
    SDL_SetWindowOpacity(SDLServer::the().window(window_id), opacity);
}

Gfx::IntPoint ConnectionToWindowServer::get_global_cursor_position()
{
    int x = 0, y = 0;
    SDL_GetGlobalMouseState(&x, &y);
    return Gfx::IntPoint(x, y);
}

Gfx::IntRect const& ConnectionToWindowServer::set_window_rect(i32 window_id, Gfx::IntRect const& rect)
{
    SDLServer::the().set_window_rect(window_id, rect);

    return rect;
}

Gfx::IntRect ConnectionToWindowServer::get_window_rect(i32 window_id)
{
    return SDLServer::the().get_window_rect(window_id);
}

void ConnectionToWindowServer::async_move_window_to_front(i32 window_id)
{
    SDL_RaiseWindow(SDLServer::the().window(window_id));
}

Gfx::IntRect ConnectionToWindowServer::get_applet_rect_on_screen(i32)
{
    return Gfx::IntRect(0, 0, 0, 0);
}

Gfx::IntSize ConnectionToWindowServer::get_window_minimum_size(i32 window_id)
{
    int width = 0, height = 0;
    SDL_GetWindowMinimumSize(SDLServer::the().window(window_id), &width, &height);
    return Gfx::IntSize(width, height);
}

void ConnectionToWindowServer::async_set_window_minimum_size(i32 window_id, Gfx::IntSize size)
{
    SDL_SetWindowMinimumSize(SDLServer::the().window(window_id), size.width(), size.height());
}

void ConnectionToWindowServer::async_set_window_resize_aspect_ratio(i32 window_id, Optional<Gfx::IntSize> const& resize_aspect_ratio)
{
    SDLServer::the().set_window_resize_aspect_ratio(window_id, resize_aspect_ratio);
}

}
