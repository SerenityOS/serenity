/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGfx/Color.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/Event.h>
#include <WindowServer/MenuBar.h>
#include <WindowServer/MenuManager.h>
#include <WindowServer/Window.h>
#include <WindowServer/WindowSwitcher.h>
#include <WindowServer/WindowType.h>

namespace WindowServer {

class Screen;
class MouseEvent;
class Window;
class ClientConnection;
class WindowSwitcher;
class Button;

enum class ResizeDirection {
    None,
    Left,
    UpLeft,
    Up,
    UpRight,
    Right,
    DownRight,
    Down,
    DownLeft
};

class WindowManager : public Core::Object {
    C_OBJECT(WindowManager)

    friend class Compositor;
    friend class WindowFrame;
    friend class WindowSwitcher;

public:
    static WindowManager& the();

    explicit WindowManager(const Gfx::PaletteImpl&);
    virtual ~WindowManager() override;

    Palette palette() const { return Palette(*m_palette); }

    RefPtr<Core::ConfigFile> wm_config() const
    {
        return m_wm_config;
    }
    void reload_config(bool);

    void add_window(Window&);
    void remove_window(Window&);

    void notify_title_changed(Window&);
    void notify_rect_changed(Window&, const Gfx::Rect& oldRect, const Gfx::Rect& newRect);
    void notify_minimization_state_changed(Window&);
    void notify_opacity_changed(Window&);
    void notify_occlusion_state_changed(Window&);
    void notify_client_changed_app_menubar(ClientConnection&);

    Gfx::Rect maximized_window_rect(const Window&) const;

    ClientConnection* dnd_client() { return m_dnd_client.ptr(); }
    const String& dnd_text() const { return m_dnd_text; }
    const String& dnd_data_type() const { return m_dnd_data_type; }
    const String& dnd_data() const { return m_dnd_data; }
    const Gfx::Bitmap* dnd_bitmap() const { return m_dnd_bitmap; }
    Gfx::Rect dnd_rect() const;

    void start_dnd_drag(ClientConnection&, const String& text, Gfx::Bitmap*, const String& data_type, const String& data);
    void end_dnd_drag();

    Window* active_window() { return m_active_window.ptr(); }
    const ClientConnection* active_client() const;
    bool active_window_is_modal() const { return m_active_window && m_active_window->is_modal(); }

    Window* highlight_window() { return m_highlight_window.ptr(); }
    void set_highlight_window(Window*);

    void move_to_front_and_make_active(Window&);

    Gfx::Rect menubar_rect() const;

    const Cursor& active_cursor() const;
    const Cursor& arrow_cursor() const { return *m_arrow_cursor; }
    const Cursor& hand_cursor() const { return *m_hand_cursor; }
    const Cursor& resize_horizontally_cursor() const { return *m_resize_horizontally_cursor; }
    const Cursor& resize_vertically_cursor() const { return *m_resize_vertically_cursor; }
    const Cursor& resize_diagonally_tlbr_cursor() const { return *m_resize_diagonally_tlbr_cursor; }
    const Cursor& resize_diagonally_bltr_cursor() const { return *m_resize_diagonally_bltr_cursor; }
    const Cursor& i_beam_cursor() const { return *m_i_beam_cursor; }
    const Cursor& disallowed_cursor() const { return *m_disallowed_cursor; }
    const Cursor& move_cursor() const { return *m_move_cursor; }
    const Cursor& drag_cursor() const { return *m_drag_cursor; }

    void invalidate(const Window&);
    void invalidate(const Window&, const Gfx::Rect&);
    void invalidate(const Gfx::Rect&);
    void invalidate();
    void flush(const Gfx::Rect&);

    const Gfx::Font& font() const;
    const Gfx::Font& window_title_font() const;

    bool set_resolution(int width, int height);
    Gfx::Size resolution() const;

    void set_active_window(Window*);
    void set_hovered_button(Button*);

    Button* cursor_tracking_button() { return m_cursor_tracking_button.ptr(); }
    void set_cursor_tracking_button(Button*);

    void set_resize_candidate(Window&, ResizeDirection);
    void clear_resize_candidate();
    ResizeDirection resize_direction_of_window(const Window&);

    bool any_opaque_window_contains_rect(const Gfx::Rect&);
    bool any_opaque_window_above_this_one_contains_rect(const Window&, const Gfx::Rect&);

    void tell_wm_listeners_window_state_changed(Window&);
    void tell_wm_listeners_window_icon_changed(Window&);
    void tell_wm_listeners_window_rect_changed(Window&);

    void start_window_resize(Window&, const Gfx::Point&, MouseButton);
    void start_window_resize(Window&, const MouseEvent&);

    const Window* active_fullscreen_window() const { return (m_active_window && m_active_window->is_fullscreen()) ? m_active_window : nullptr; }
    Window* active_fullscreen_window() { return (m_active_window && m_active_window->is_fullscreen()) ? m_active_window : nullptr; }

    bool update_theme(String theme_path, String theme_name);

    void set_hovered_window(Window*);
    void deliver_mouse_event(Window& window, MouseEvent& event);

private:
    NonnullRefPtr<Cursor> get_cursor(const String& name);
    NonnullRefPtr<Cursor> get_cursor(const String& name, const Gfx::Point& hotspot);

    void process_mouse_event(MouseEvent&, Window*& hovered_window);
    void process_event_for_doubleclick(Window& window, MouseEvent& event);
    bool process_ongoing_window_resize(const MouseEvent&, Window*& hovered_window);
    bool process_ongoing_window_move(MouseEvent&, Window*& hovered_window);
    bool process_ongoing_drag(MouseEvent&, Window*& hovered_window);
    void start_window_move(Window&, const MouseEvent&);
    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_back_to_front(WindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_front_to_back(WindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_visible_window_from_front_to_back(Callback);
    template<typename Callback>
    IterationDecision for_each_visible_window_from_back_to_front(Callback);
    template<typename Callback>
    void for_each_window_listening_to_wm_events(Callback);
    template<typename Callback>
    void for_each_window(Callback);
    template<typename Callback>
    IterationDecision for_each_window_of_type_from_front_to_back(WindowType, Callback, bool ignore_highlight = false);

    virtual void event(Core::Event&) override;
    void paint_window_frame(const Window&);
    void tell_wm_listener_about_window(Window& listener, Window&);
    void tell_wm_listener_about_window_icon(Window& listener, Window&);
    void tell_wm_listener_about_window_rect(Window& listener, Window&);
    void pick_new_active_window();

    void recompute_occlusions();

    RefPtr<Cursor> m_arrow_cursor;
    RefPtr<Cursor> m_hand_cursor;
    RefPtr<Cursor> m_resize_horizontally_cursor;
    RefPtr<Cursor> m_resize_vertically_cursor;
    RefPtr<Cursor> m_resize_diagonally_tlbr_cursor;
    RefPtr<Cursor> m_resize_diagonally_bltr_cursor;
    RefPtr<Cursor> m_i_beam_cursor;
    RefPtr<Cursor> m_disallowed_cursor;
    RefPtr<Cursor> m_move_cursor;
    RefPtr<Cursor> m_drag_cursor;

    Color m_background_color;
    Color m_active_window_border_color;
    Color m_active_window_border_color2;
    Color m_active_window_title_color;
    Color m_inactive_window_border_color;
    Color m_inactive_window_border_color2;
    Color m_inactive_window_title_color;
    Color m_moving_window_border_color;
    Color m_moving_window_border_color2;
    Color m_moving_window_title_color;
    Color m_highlight_window_border_color;
    Color m_highlight_window_border_color2;
    Color m_highlight_window_title_color;

    InlineLinkedList<Window> m_windows_in_order;

    struct DoubleClickInfo {
        struct ClickMetadata {
            Core::ElapsedTimer clock;
            Gfx::Point last_position;
        };

        ClickMetadata& metadata_for_button(MouseButton);

        void reset()
        {
            m_left = {};
            m_right = {};
            m_middle = {};
        }

        WeakPtr<Window> m_clicked_window;

    private:
        ClickMetadata m_left;
        ClickMetadata m_right;
        ClickMetadata m_middle;
    };
    DoubleClickInfo m_double_click_info;
    int m_double_click_speed { 0 };
    int m_max_distance_for_double_click { 4 };

    WeakPtr<Window> m_active_window;
    WeakPtr<Window> m_hovered_window;
    WeakPtr<Window> m_highlight_window;
    WeakPtr<Window> m_active_input_window;

    WeakPtr<Window> m_move_window;
    Gfx::Point m_move_origin;
    Gfx::Point m_move_window_origin;

    WeakPtr<Window> m_resize_window;
    WeakPtr<Window> m_resize_candidate;
    MouseButton m_resizing_mouse_button { MouseButton::None };
    Gfx::Rect m_resize_window_original_rect;
    Gfx::Point m_resize_origin;
    ResizeDirection m_resize_direction { ResizeDirection::None };

    bool m_moved_or_resized_since_logo_keydown { false };

    u8 m_keyboard_modifiers { 0 };

    WindowSwitcher m_switcher;

    WeakPtr<Button> m_cursor_tracking_button;
    WeakPtr<Button> m_hovered_button;

    NonnullRefPtr<Gfx::PaletteImpl> m_palette;

    RefPtr<Core::ConfigFile> m_wm_config;

    WeakPtr<ClientConnection> m_dnd_client;
    String m_dnd_text;
    String m_dnd_data_type;
    String m_dnd_data;
    RefPtr<Gfx::Bitmap> m_dnd_bitmap;
};

template<typename Callback>
IterationDecision WindowManager::for_each_visible_window_of_type_from_back_to_front(WindowType type, Callback callback, bool ignore_highlight)
{
    bool do_highlight_window_at_end = false;
    for (auto& window : m_windows_in_order) {
        if (!window.is_visible())
            continue;
        if (window.is_minimized())
            continue;
        if (window.type() != type)
            continue;
        if (!ignore_highlight && m_highlight_window == &window) {
            do_highlight_window_at_end = true;
            continue;
        }
        if (callback(window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    if (do_highlight_window_at_end) {
        if (callback(*m_highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
IterationDecision WindowManager::for_each_visible_window_from_back_to_front(Callback callback)
{
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Normal, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Taskbar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Tooltip, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Menubar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Menu, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_visible_window_of_type_from_back_to_front(WindowType::WindowSwitcher, callback);
}

template<typename Callback>
IterationDecision WindowManager::for_each_visible_window_of_type_from_front_to_back(WindowType type, Callback callback, bool ignore_highlight)
{
    if (!ignore_highlight && m_highlight_window && m_highlight_window->type() == type && m_highlight_window->is_visible()) {
        if (callback(*m_highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->is_visible())
            continue;
        if (window->is_minimized())
            continue;
        if (window->type() != type)
            continue;
        if (!ignore_highlight && window == m_highlight_window)
            continue;
        if (callback(*window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
IterationDecision WindowManager::for_each_visible_window_from_front_to_back(Callback callback)
{
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::WindowSwitcher, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Menu, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Menubar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Taskbar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Tooltip, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_visible_window_of_type_from_front_to_back(WindowType::Normal, callback);
}

template<typename Callback>
void WindowManager::for_each_window_listening_to_wm_events(Callback callback)
{
    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->listens_to_wm_events())
            continue;
        if (callback(*window) == IterationDecision::Break)
            return;
    }
}

template<typename Callback>
void WindowManager::for_each_window(Callback callback)
{
    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (callback(*window) == IterationDecision::Break)
            return;
    }
}

template<typename Callback>
IterationDecision WindowManager::for_each_window_of_type_from_front_to_back(WindowType type, Callback callback, bool ignore_highlight)
{
    if (!ignore_highlight && m_highlight_window && m_highlight_window->type() == type && m_highlight_window->is_visible()) {
        if (callback(*m_highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (window->type() != type)
            continue;
        if (!ignore_highlight && window == m_highlight_window)
            continue;
        if (callback(*window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

}
