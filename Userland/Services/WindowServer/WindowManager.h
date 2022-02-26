/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WindowStack.h"
#include <AK/HashMap.h>
#include <AK/HashTable.h>
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
#include <WindowServer/KeymapSwitcher.h>
#include <WindowServer/MenuManager.h>
#include <WindowServer/ScreenLayout.h>
#include <WindowServer/WMConnectionFromClient.h>
#include <WindowServer/WindowSwitcher.h>
#include <WindowServer/WindowType.h>

namespace WindowServer {

const int double_click_speed_max = 900;
const int double_click_speed_min = 100;

class Screen;
class MouseEvent;
class Window;
class ConnectionFromClient;
class WindowSwitcher;
class Button;
class DndOverlay;
class WindowGeometryOverlay;

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
    static constexpr size_t default_window_stack_rows = 2;
    static constexpr size_t default_window_stack_columns = 2;
    static_assert(default_window_stack_rows >= 1);
    static_assert(default_window_stack_columns >= 1);
    static constexpr unsigned max_window_stack_rows = 16;
    static constexpr unsigned max_window_stack_columns = 16;

    static WindowManager& the();

    virtual ~WindowManager() override;

    Palette palette() const { return Palette(*m_palette); }

    RefPtr<Core::ConfigFile> config() const { return m_config; }
    void reload_config();

    void add_window(Window&);
    void remove_window(Window&);

    void notify_title_changed(Window&);
    void notify_modal_unparented(Window&);
    void notify_rect_changed(Window&, Gfx::IntRect const& oldRect, Gfx::IntRect const& newRect);
    void notify_minimization_state_changed(Window&);
    void notify_opacity_changed(Window&);
    void notify_occlusion_state_changed(Window&);
    void notify_progress_changed(Window&);
    void notify_modified_changed(Window&);

    Gfx::IntRect tiled_window_rect(Window const&, WindowTileType tile_type = WindowTileType::Maximized, bool relative_to_window_screen = false) const;

    ConnectionFromClient const* dnd_client() const { return m_dnd_client.ptr(); }
    Core::MimeData const& dnd_mime_data() const { return *m_dnd_mime_data; }

    void start_dnd_drag(ConnectionFromClient&, String const& text, Gfx::Bitmap const*, Core::MimeData const&);
    void end_dnd_drag();

    Window* active_window()
    {
        VERIFY(m_current_window_stack);
        return m_current_window_stack->active_window();
    }
    Window const* active_window() const
    {
        VERIFY(m_current_window_stack);
        return m_current_window_stack->active_window();
    }

    Window* active_input_window()
    {
        VERIFY(m_current_window_stack);
        return m_current_window_stack->active_input_window();
    }
    Window const* active_input_window() const
    {
        VERIFY(m_current_window_stack);
        return m_current_window_stack->active_input_window();
    }

    ConnectionFromClient const* active_client() const;

    Window* window_with_active_menu() { return m_window_with_active_menu; }
    Window const* window_with_active_menu() const { return m_window_with_active_menu; }
    void set_window_with_active_menu(Window*);

    Window* highlight_window() { return m_highlight_window; }
    Window const* highlight_window() const { return m_highlight_window; }
    void set_highlight_window(Window*);

    void move_to_front_and_make_active(Window&);

    Gfx::IntRect desktop_rect(Screen&) const;
    Gfx::IntRect arena_rect_for_type(Screen&, WindowType) const;

    Cursor const& active_cursor() const;
    Cursor const& hidden_cursor() const { return *m_hidden_cursor; }
    Cursor const& arrow_cursor() const { return *m_arrow_cursor; }
    Cursor const& crosshair_cursor() const { return *m_crosshair_cursor; }
    Cursor const& hand_cursor() const { return *m_hand_cursor; }
    Cursor const& help_cursor() const { return *m_help_cursor; }
    Cursor const& resize_horizontally_cursor() const { return *m_resize_horizontally_cursor; }
    Cursor const& resize_vertically_cursor() const { return *m_resize_vertically_cursor; }
    Cursor const& resize_diagonally_tlbr_cursor() const { return *m_resize_diagonally_tlbr_cursor; }
    Cursor const& resize_diagonally_bltr_cursor() const { return *m_resize_diagonally_bltr_cursor; }
    Cursor const& resize_column_cursor() const { return *m_resize_column_cursor; }
    Cursor const& resize_row_cursor() const { return *m_resize_row_cursor; }
    Cursor const& i_beam_cursor() const { return *m_i_beam_cursor; }
    Cursor const& disallowed_cursor() const { return *m_disallowed_cursor; }
    Cursor const& move_cursor() const { return *m_move_cursor; }
    Cursor const& drag_cursor() const { return *m_drag_cursor; }
    Cursor const& wait_cursor() const { return *m_wait_cursor; }
    Cursor const& eyedropper_cursor() const { return *m_eyedropper_cursor; }
    Cursor const& zoom_cursor() const { return *m_zoom_cursor; }

    Gfx::Font const& font() const;
    Gfx::Font const& window_title_font() const;

    bool set_screen_layout(ScreenLayout&&, bool, String&);
    ScreenLayout get_screen_layout() const;
    bool save_screen_layout(String&);

    void set_acceleration_factor(double);
    void set_scroll_step_size(unsigned);
    void set_double_click_speed(int);
    int double_click_speed() const;
    void set_buttons_switched(bool);
    bool get_buttons_switched() const;

    Window* set_active_input_window(Window*);
    void restore_active_input_window(Window*);
    void set_active_window(Window*, bool make_input = true);
    void set_hovered_button(Button*);

    Button const* cursor_tracking_button() const { return m_cursor_tracking_button.ptr(); }
    void set_cursor_tracking_button(Button*);

    void set_resize_candidate(Window&, ResizeDirection);
    void clear_resize_candidate();
    ResizeDirection resize_direction_of_window(Window const&);

    void greet_window_manager(WMConnectionFromClient&);
    void tell_wms_window_state_changed(Window&);
    void tell_wms_window_icon_changed(Window&);
    void tell_wms_window_rect_changed(Window&);
    void tell_wms_screen_rects_changed();
    void tell_wms_applet_area_size_changed(Gfx::IntSize const&);
    void tell_wms_super_key_pressed();
    void tell_wms_super_space_key_pressed();
    void tell_wms_super_digit_key_pressed(u8);
    void tell_wms_current_window_stack_changed();

    bool is_active_window_or_accessory(Window&) const;

    void check_hide_geometry_overlay(Window&);

    void start_window_resize(Window&, Gfx::IntPoint const&, MouseButton);
    void start_window_resize(Window&, MouseEvent const&);
    void start_window_move(Window&, MouseEvent const&);
    void start_window_move(Window&, Gfx::IntPoint const&);

    Window const* active_fullscreen_window() const
    {
        if (active_window() && active_window()->is_fullscreen())
            return active_window();
        return nullptr;
    };

    Window* active_fullscreen_window()
    {
        if (active_window() && active_window()->is_fullscreen())
            return active_window();
        return nullptr;
    }

    bool update_theme(String theme_path, String theme_name);
    void invalidate_after_theme_or_font_change();

    bool set_hovered_window(Window*);
    void deliver_mouse_event(Window&, MouseEvent const&, bool process_double_click);

    void did_popup_a_menu(Badge<Menu>);

    void start_menu_doubleclick(Window& window, MouseEvent const& event);
    bool is_menu_doubleclick(Window& window, MouseEvent const& event) const;

    void minimize_windows(Window&, bool);
    void hide_windows(Window&, bool);
    void maximize_windows(Window&, bool);
    void set_always_on_top(Window&, bool);

    template<typename Function>
    IterationDecision for_each_window_in_modal_stack(Window& window, Function f)
    {
        auto* blocking_modal_window = window.blocking_modal_window();
        if (blocking_modal_window || window.is_modal()) {
            Vector<Window&> modal_stack;
            auto* modal_stack_top = blocking_modal_window ? blocking_modal_window : &window;
            for (auto* parent = modal_stack_top->parent_window(); parent; parent = parent->parent_window()) {
                auto* blocked_by = parent->blocking_modal_window();
                if (!blocked_by || (blocked_by != modal_stack_top && !modal_stack_top->is_descendant_of(*blocked_by)))
                    break;
                modal_stack.append(*parent);
                if (!parent->is_modal())
                    break;
            }
            if (!modal_stack.is_empty()) {
                for (size_t i = modal_stack.size(); i > 0; i--) {
                    IterationDecision decision = f(modal_stack[i - 1], false);
                    if (decision != IterationDecision::Continue)
                        return decision;
                }
            }
            return f(*modal_stack_top, true);
        } else {
            // Not a modal window stack, just "iterate" over this window
            return f(window, true);
        }
    }
    bool is_window_in_modal_stack(Window& window_in_modal_stack, Window& other_window);

    Gfx::IntPoint get_recommended_window_position(Gfx::IntPoint const& desired);

    void reload_icon_bitmaps_after_scale_change();

    void reevaluate_hover_state_for_window(Window* = nullptr);
    Window* hovered_window() const { return m_hovered_window.ptr(); }

    void switch_to_window_stack(WindowStack&, Window* = nullptr, bool show_overlay = true);
    void switch_to_window_stack(u32 row, u32 col, Window* carry = nullptr, bool show_overlay = true)
    {
        if (row < window_stack_rows() && col < window_stack_columns())
            switch_to_window_stack(m_window_stacks[row][col], carry, show_overlay);
    }

    size_t window_stack_rows() const { return m_window_stacks.size(); }
    size_t window_stack_columns() const { return m_window_stacks[0].size(); }

    bool apply_workspace_settings(unsigned rows, unsigned columns, bool save);

    WindowStack& current_window_stack()
    {
        VERIFY(m_current_window_stack);
        return *m_current_window_stack;
    }

    template<typename F>
    IterationDecision for_each_window_stack(F f)
    {
        for (auto& row : m_window_stacks) {
            for (auto& stack : row) {
                IterationDecision decision = f(stack);
                if (decision != IterationDecision::Continue)
                    return decision;
            }
        }
        return IterationDecision::Continue;
    }

    WindowStack& window_stack_for_window(Window&);

    static constexpr bool is_stationary_window_type(WindowType window_type)
    {
        switch (window_type) {
        case WindowType::Normal:
        case WindowType::ToolWindow:
        case WindowType::Tooltip:
            return false;
        default:
            return true;
        }
    }

    void did_switch_window_stack(Badge<Compositor>, WindowStack&, WindowStack&);

    template<typename Callback>
    IterationDecision for_each_visible_window_from_back_to_front(Callback, WindowStack* = nullptr);
    template<typename Callback>
    IterationDecision for_each_visible_window_from_front_to_back(Callback, WindowStack* = nullptr);

    MultiScaleBitmaps const* overlay_rect_shadow() const { return m_overlay_rect_shadow.ptr(); }

    void apply_cursor_theme(String const& name);

private:
    explicit WindowManager(Gfx::PaletteImpl const&);

    void notify_new_active_window(Window&);
    void notify_new_active_input_window(Window&);
    void notify_previous_active_window(Window&);
    void notify_previous_active_input_window(Window&);

    void process_mouse_event(MouseEvent&);
    void process_event_for_doubleclick(Window& window, MouseEvent& event);
    bool process_ongoing_window_resize(MouseEvent const&);
    bool process_ongoing_window_move(MouseEvent&);
    bool process_ongoing_drag(MouseEvent&);
    bool process_ongoing_active_input_mouse_event(MouseEvent const&);
    bool process_mouse_event_for_titlebar_buttons(MouseEvent const&);
    void process_mouse_event_for_window(HitTestResult&, MouseEvent const&);

    void process_key_event(KeyEvent&);

    template<typename Callback>
    void for_each_window_manager(Callback);

    virtual void event(Core::Event&) override;
    void tell_wm_about_window(WMConnectionFromClient& conn, Window&);
    void tell_wm_about_window_icon(WMConnectionFromClient& conn, Window&);
    void tell_wm_about_window_rect(WMConnectionFromClient& conn, Window&);
    void tell_wm_about_current_window_stack(WMConnectionFromClient&);
    bool pick_new_active_window(Window*);

    void do_move_to_front(Window&, bool, bool);

    [[nodiscard]] static WindowStack& get_rendering_window_stacks(WindowStack*&);

    RefPtr<Cursor> m_hidden_cursor;
    RefPtr<Cursor> m_arrow_cursor;
    RefPtr<Cursor> m_hand_cursor;
    RefPtr<Cursor> m_help_cursor;
    RefPtr<Cursor> m_resize_horizontally_cursor;
    RefPtr<Cursor> m_resize_vertically_cursor;
    RefPtr<Cursor> m_resize_diagonally_tlbr_cursor;
    RefPtr<Cursor> m_resize_diagonally_bltr_cursor;
    RefPtr<Cursor> m_resize_column_cursor;
    RefPtr<Cursor> m_resize_row_cursor;
    RefPtr<Cursor> m_i_beam_cursor;
    RefPtr<Cursor> m_disallowed_cursor;
    RefPtr<Cursor> m_move_cursor;
    RefPtr<Cursor> m_drag_cursor;
    RefPtr<Cursor> m_wait_cursor;
    RefPtr<Cursor> m_crosshair_cursor;
    RefPtr<Cursor> m_eyedropper_cursor;
    RefPtr<Cursor> m_zoom_cursor;

    RefPtr<MultiScaleBitmaps> m_overlay_rect_shadow;

    // Setup 2 rows 1 column by default
    NonnullOwnPtrVector<NonnullOwnPtrVector<WindowStack, default_window_stack_columns>, default_window_stack_rows> m_window_stacks;
    WindowStack* m_current_window_stack { nullptr };

    struct DoubleClickInfo {
        struct ClickMetadata {
            Core::ElapsedTimer clock;
            Gfx::IntPoint last_position;
        };

        ClickMetadata const& metadata_for_button(MouseButton) const;
        ClickMetadata& metadata_for_button(MouseButton);

        void reset()
        {
            m_primary = {};
            m_secondary = {};
            m_middle = {};
            m_backward = {};
            m_forward = {};
        }

        WeakPtr<Window> m_clicked_window;

    private:
        ClickMetadata m_primary;
        ClickMetadata m_secondary;
        ClickMetadata m_middle;
        ClickMetadata m_backward;
        ClickMetadata m_forward;
    };

    bool is_considered_doubleclick(MouseEvent const&, DoubleClickInfo::ClickMetadata const&) const;

    Gfx::IntPoint to_floating_cursor_position(Gfx::IntPoint const&) const;

    DoubleClickInfo m_double_click_info;
    int m_double_click_speed { 0 };
    int m_max_distance_for_double_click { 4 };
    bool m_previous_event_was_super_keydown { false };
    bool m_buttons_switched { false };

    WeakPtr<Window> m_hovered_window;
    WeakPtr<Window> m_highlight_window;
    WeakPtr<Window> m_window_with_active_menu;

    OwnPtr<WindowGeometryOverlay> m_geometry_overlay;
    WeakPtr<Window> m_move_window;
    Gfx::IntPoint m_move_origin;
    Gfx::IntPoint m_move_window_origin;
    Gfx::IntPoint m_move_window_cursor_position;
    Gfx::IntPoint m_mouse_down_origin;

    WeakPtr<Window> m_resize_window;
    WeakPtr<Window> m_resize_candidate;
    MouseButton m_resizing_mouse_button { MouseButton::None };
    Gfx::IntRect m_resize_window_original_rect;
    Gfx::IntPoint m_resize_origin;
    ResizeDirection m_resize_direction { ResizeDirection::None };

    u8 m_keyboard_modifiers { 0 };

    NonnullRefPtr<WindowSwitcher> m_switcher;
    NonnullRefPtr<KeymapSwitcher> m_keymap_switcher;

    WeakPtr<Button> m_cursor_tracking_button;
    WeakPtr<Button> m_hovered_button;

    NonnullRefPtr<Gfx::PaletteImpl> m_palette;

    RefPtr<Core::ConfigFile> m_config;

    OwnPtr<DndOverlay> m_dnd_overlay;
    WeakPtr<ConnectionFromClient> m_dnd_client;
    String m_dnd_text;

    RefPtr<Core::MimeData> m_dnd_mime_data;

    WindowStack* m_switching_to_window_stack { nullptr };
    Vector<WeakPtr<Window>, 4> m_carry_window_to_new_stack;
};

template<typename Callback>
inline IterationDecision WindowManager::for_each_visible_window_from_back_to_front(Callback callback, WindowStack* specific_stack)
{
    auto* window_stack = specific_stack;
    WindowStack* transitioning_to_window_stack = nullptr;
    if (!window_stack)
        window_stack = &get_rendering_window_stacks(transitioning_to_window_stack);
    auto for_each_window = [&]<WindowType window_type>() {
        if constexpr (is_stationary_window_type(window_type)) {
            auto& stationary_stack = window_stack->stationary_window_stack();
            return stationary_stack.for_each_visible_window_of_type_from_back_to_front(window_type, callback);
        } else {
            auto decision = window_stack->for_each_visible_window_of_type_from_back_to_front(window_type, callback);
            if (decision == IterationDecision::Continue && transitioning_to_window_stack)
                decision = transitioning_to_window_stack->for_each_visible_window_of_type_from_back_to_front(window_type, callback);
            return decision;
        }
    };
    if (for_each_window.template operator()<WindowType::Desktop>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Normal>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::ToolWindow>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Taskbar>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::AppletArea>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Notification>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Tooltip>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Menu>() == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_window.template operator()<WindowType::WindowSwitcher>();
}

template<typename Callback>
inline IterationDecision WindowManager::for_each_visible_window_from_front_to_back(Callback callback, WindowStack* specific_stack)
{
    auto* window_stack = specific_stack;
    WindowStack* transitioning_to_window_stack = nullptr;
    if (!window_stack)
        window_stack = &get_rendering_window_stacks(transitioning_to_window_stack);
    auto for_each_window = [&]<WindowType window_type>() {
        if constexpr (is_stationary_window_type(window_type)) {
            auto& stationary_stack = window_stack->stationary_window_stack();
            return stationary_stack.for_each_visible_window_of_type_from_front_to_back(window_type, callback);
        } else {
            auto decision = window_stack->for_each_visible_window_of_type_from_front_to_back(window_type, callback);
            if (decision == IterationDecision::Continue && transitioning_to_window_stack)
                decision = transitioning_to_window_stack->for_each_visible_window_of_type_from_front_to_back(window_type, callback);
            return decision;
        }
    };
    if (for_each_window.template operator()<WindowType::WindowSwitcher>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Menu>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Tooltip>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Notification>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::AppletArea>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Taskbar>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::ToolWindow>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Normal>() == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_window.template operator()<WindowType::Desktop>();
}

template<typename Callback>
void WindowManager::for_each_window_manager(Callback callback)
{
    auto& connections = WMConnectionFromClient::s_connections;

    // FIXME: this isn't really ordered... does it need to be?
    for (auto it = connections.begin(); it != connections.end(); ++it) {
        if (callback(*it->value) == IterationDecision::Break)
            return;
    }
}

}
