/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Rect.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/Size.h>

namespace GUI {

class ConnectionToWindowServer {
public:
    static ConnectionToWindowServer& the();
    ConnectionToWindowServer() = default;

    void window_resized(i32, Gfx::IntRect const&);
    void paint(i32, Gfx::IntSize const&, Vector<Gfx::IntRect> const&);
    void mouse_move(i32, Gfx::IntPoint const&, u32, u32, u32, i32, i32, bool, Vector<String> const&);
    void mouse_down(i32, Gfx::IntPoint const&, u32, u32, u32, i32, i32);
    void mouse_up(i32, Gfx::IntPoint const&, u32, u32, u32, i32, i32);

    void async_create_window(i32, Gfx::IntRect const&,
        bool, bool, bool, bool, bool, bool,
        bool, bool, bool, bool, float,
        float, Gfx::IntSize const&, Gfx::IntSize const&,
        Gfx::IntSize const&, Optional<Gfx::IntSize> const&, i32,
        String const&, i32, Gfx::IntRect const&);
    Vector<i32>& destroy_window(i32);

    void async_set_window_title(i32 window_id, String const& title);
    String get_window_title(i32 window_id);
    bool is_window_modified(i32) { return false; }
    void async_set_window_modified(i32, bool) { }

    void async_did_finish_painting(i32, Vector<Gfx::IntRect> const&);
    void async_invalidate_rect(i32, Vector<Gfx::IntRect> const&, bool);
    void async_set_forced_shadow(i32, bool);
    void async_refresh_system_theme();

    void async_set_fullscreen(i32, bool);
    void async_set_frameless(i32, bool);
    void async_set_maximized(i32, bool);
    void async_set_window_opacity(i32, float);
    void async_set_window_alpha_hit_threshold(i32, float) { }
    void async_set_window_has_alpha_channel(i32, bool) { }
    void set_window_backing_store(i32, i32, i32, int, i32, bool, Gfx::IntSize const&, bool) { }
    void async_set_window_base_size_and_size_increment(i32, Gfx::IntSize const&, Gfx::IntSize const&) { }
    void async_set_window_progress(i32, Optional<i32> const&) { }

    void async_add_menu(i32, i32) { }
    int async_create_menu(i32, String const&) { return -1; }
    void async_popup_menu(i32, Gfx::IntPoint const&) { }
    void async_destroy_menu(i32) { }
    void async_dismiss_menu(i32) { }
    void async_add_menu_separator(i32) { }
    void async_add_menu_item(i32, i32, i32, String const&, bool, bool, bool, bool, String const&, Gfx::ShareableBitmap const&, bool) { }
    void async_update_menu_item(i32, i32, i32, String const&, bool, bool, bool, bool, String const&) { }
    void async_remove_menu_item(i32, i32) { }
    void async_flash_menubar_menu(i32, i32) { }

    void async_set_window_cursor(i32, i32) { }
    void async_set_window_custom_cursor(i32, Gfx::ShareableBitmap const&) { }

    bool start_drag(String const&, HashMap<String, ByteBuffer> const&, Gfx::ShareableBitmap const&) { return false; }

    Gfx::IntPoint get_global_cursor_position();

    Gfx::IntRect const& set_window_rect(i32 window_id, Gfx::IntRect const& rect);
    Gfx::IntRect get_window_rect(i32 window_id);

    void async_move_window_to_front(i32 window_id);

    Gfx::IntRect get_applet_rect_on_screen(i32 window_id);

    Gfx::IntSize get_window_minimum_size(i32 window_id);
    void async_set_window_minimum_size(i32 window_id, Gfx::IntSize size);

    void async_set_window_resize_aspect_ratio(i32 window_id, Optional<Gfx::IntSize> const& resize_aspect_ratio);

    void async_set_window_icon_bitmap(i32, Gfx::ShareableBitmap const&) { }

    void async_start_window_resize(i32) { }

    bool is_maximized(i32) { return false; }
};

}
