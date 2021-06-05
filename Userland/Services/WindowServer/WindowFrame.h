/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/RefPtr.h>
#include <LibCore/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/WindowTheme.h>

namespace WindowServer {

class Button;
class Menu;
class MouseEvent;
class Window;

class WindowFrame {
public:
    static void reload_config();

    explicit WindowFrame(Window&);
    ~WindowFrame();

    Gfx::IntRect rect() const;
    Gfx::IntRect render_rect() const;
    Gfx::DisjointRectSet opaque_render_rects() const;
    Gfx::DisjointRectSet transparent_render_rects() const;
    void paint(Gfx::Painter&, const Gfx::IntRect&);
    void render(Gfx::Painter&);
    void render_to_cache();
    void on_mouse_event(const MouseEvent&);
    void notify_window_rect_changed(const Gfx::IntRect& old_rect, const Gfx::IntRect& new_rect);
    void invalidate_titlebar();
    void invalidate(Gfx::IntRect relative_rect);
    void invalidate();

    Gfx::IntRect titlebar_rect() const;
    Gfx::IntRect titlebar_icon_rect() const;
    Gfx::IntRect titlebar_text_rect() const;

    Gfx::IntRect menubar_rect() const;
    int menu_row_count() const;

    void did_set_maximized(Badge<Window>, bool);

    void layout_buttons();
    void set_button_icons();

    void start_flash_animation();

    bool has_alpha_channel() const { return m_has_alpha_channel; }
    void set_has_alpha_channel(bool value) { m_has_alpha_channel = value; }
    bool has_shadow() const;

    void set_opacity(float);
    float opacity() const { return m_opacity; }

    bool is_opaque() const
    {
        if (opacity() < 1.0f)
            return false;
        if (has_alpha_channel())
            return false;
        return true;
    }

    void set_dirty(bool re_render_shadow = false)
    {
        m_dirty = true;
        m_shadow_dirty |= re_render_shadow;
    }

    void theme_changed();

    bool hit_test(const Gfx::IntPoint&) const;

    void open_menubar_menu(Menu&);

private:
    void paint_simple_rect_shadow(Gfx::Painter&, const Gfx::IntRect&, const Gfx::Bitmap&) const;
    void paint_notification_frame(Gfx::Painter&);
    void paint_normal_frame(Gfx::Painter&);
    void paint_tool_window_frame(Gfx::Painter&);
    void paint_menubar(Gfx::Painter&);
    Gfx::Bitmap* window_shadow() const;
    Gfx::IntRect inflated_for_shadow(const Gfx::IntRect&) const;
    Gfx::Bitmap* inflate_for_shadow(Gfx::IntRect&, Gfx::IntPoint&) const;

    void handle_menubar_mouse_event(const MouseEvent&);
    void handle_menu_mouse_event(Menu&, const MouseEvent&);

    Gfx::WindowTheme::WindowState window_state_for_theme() const;
    String computed_title() const;

    Window& m_window;
    NonnullOwnPtrVector<Button> m_buttons;
    Button* m_close_button { nullptr };
    Button* m_maximize_button { nullptr };
    Button* m_minimize_button { nullptr };

    Gfx::IntPoint m_shadow_offset {};

    RefPtr<Gfx::Bitmap> m_top_bottom;
    RefPtr<Gfx::Bitmap> m_left_right;
    int m_bottom_y { 0 }; // y-offset in m_top_bottom for the bottom half
    int m_right_x { 0 };  // x-offset in m_left_right for the right half

    RefPtr<Core::Timer> m_flash_timer;
    size_t m_flash_counter { 0 };
    float m_opacity { 1 };
    bool m_has_alpha_channel { false };
    bool m_shadow_dirty { false };
    bool m_dirty { false };
};

}
