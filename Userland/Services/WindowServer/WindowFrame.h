/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <LibCore/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/WindowTheme.h>
#include <WindowServer/HitTestResult.h>
#include <WindowServer/ResizeDirection.h>

namespace WindowServer {

class Button;
class Menu;
class MouseEvent;
class MultiScaleBitmaps;
class Screen;
class Window;

class WindowFrame {
public:
    class PerScaleRenderedCache {
        friend class WindowFrame;

    public:
        void paint(WindowFrame&, Gfx::Painter&, Gfx::IntRect const&);
        void render(WindowFrame&, Screen&);
        Optional<HitTestResult> hit_test(WindowFrame&, Gfx::IntPoint, Gfx::IntPoint);

    private:
        RefPtr<Gfx::Bitmap> m_top_bottom;
        RefPtr<Gfx::Bitmap> m_left_right;
        int m_bottom_y { 0 }; // y-offset in m_top_bottom for the bottom half
        int m_right_x { 0 };  // x-offset in m_left_right for the right half
        bool m_shadow_dirty { true };
        bool m_dirty { true };
    };
    friend class RenderedCache;

    static Gfx::IntRect frame_rect_for_window(Window&, Gfx::IntRect const&);

    static void reload_config();

    explicit WindowFrame(Window&);
    ~WindowFrame();

    void window_was_constructed(Badge<Window>);

    Window& window() { return m_window; }
    Window const& window() const { return m_window; }

    Gfx::IntRect rect() const;
    Gfx::IntRect render_rect() const;
    Gfx::IntRect unconstrained_render_rect() const;
    Gfx::DisjointIntRectSet opaque_render_rects() const;
    Gfx::DisjointIntRectSet transparent_render_rects() const;

    void paint(Screen&, Gfx::Painter&, Gfx::IntRect const&);
    void render(Screen&, Gfx::Painter&);
    PerScaleRenderedCache* render_to_cache(Screen&);

    void handle_mouse_event(MouseEvent const&);
    void handle_titlebar_mouse_event(MouseEvent const&);
    bool handle_titlebar_icon_mouse_event(MouseEvent const&);
    void handle_border_mouse_event(MouseEvent const&);

    void window_rect_changed(Gfx::IntRect const& old_rect, Gfx::IntRect const& new_rect);
    void invalidate_titlebar();
    void invalidate_menubar();
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

    bool is_opaque() const
    {
        return !has_alpha_channel();
    }

    void set_dirty(bool re_render_shadow = false)
    {
        for (auto& [_, cached] : m_rendered_cache) {
            cached->m_dirty = true;
            cached->m_shadow_dirty |= re_render_shadow;
        }
    }

    void theme_changed();

    Optional<HitTestResult> hit_test(Gfx::IntPoint);

    void open_menubar_menu(Menu&);

    Gfx::WindowTheme::WindowState window_state_for_theme() const;

private:
    void paint_notification_frame(Gfx::Painter&);
    void paint_normal_frame(Gfx::Painter&);
    void paint_menubar(Gfx::Painter&);
    MultiScaleBitmaps const* shadow_bitmap() const;
    Gfx::IntRect inflated_for_shadow(Gfx::IntRect const&) const;
    void latch_window_to_screen_edge(ResizeDirection);

    void handle_menubar_mouse_event(MouseEvent const&);
    void handle_menu_mouse_event(Menu&, MouseEvent const&);

    ByteString computed_title() const;

    Gfx::IntRect constrained_render_rect_to_screen(Gfx::IntRect const&) const;
    Gfx::IntRect leftmost_titlebar_button_rect() const;

    Window& m_window;
    Vector<NonnullOwnPtr<Button>> m_buttons;
    Button* m_close_button { nullptr };
    Button* m_maximize_button { nullptr };
    Button* m_minimize_button { nullptr };

    HashMap<int, NonnullOwnPtr<PerScaleRenderedCache>> m_rendered_cache;

    RefPtr<Core::Timer> m_flash_timer;
    size_t m_flash_counter { 0 };
    bool m_has_alpha_channel { false };
};

}
