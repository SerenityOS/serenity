/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
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
#include <WindowServer/ResizeDirection.h>
#include <WindowServer/ScreenLayout.h>
#include <WindowServer/SystemEffects.h>
#include <WindowServer/WMConnectionFromClient.h>
#include <WindowServer/WindowSwitcher.h>
#include <WindowServer/WindowType.h>

namespace WindowServer {

int const double_click_speed_max = 900;
int const double_click_speed_min = 100;

extern RefPtr<Core::ConfigFile> g_config;

class Screen;
class MouseEvent;
class Window;
class ConnectionFromClient;
class WindowSwitcher;
class Button;
class DndOverlay;
class WindowGeometryOverlay;
class TileWindowOverlay;

class WindowManager : public Core::EventReceiver {
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

    virtual ~WindowManager() override = default;

    Palette palette() const { return Palette(*m_palette); }

    RefPtr<Core::ConfigFile> config() const { return m_config; }
    void reload_config();

    void add_window(Window&);
    void remove_window(Window&);

    void notify_title_changed(Window&);
    void notify_rect_changed(Window&, Gfx::IntRect const& oldRect, Gfx::IntRect const& newRect);
    void notify_minimization_state_changed(Window&);
    void notify_opacity_changed(Window&);
    void notify_occlusion_state_changed(Window&);
    void notify_progress_changed(Window&);
    void notify_modified_changed(Window&);

    Gfx::IntRect tiled_window_rect(Window const&, Optional<Screen const&> = {}, WindowTileType tile_type = WindowTileType::Maximized) const;

    ConnectionFromClient const* dnd_client() const { return m_dnd_client.ptr(); }
    Core::MimeData const& dnd_mime_data() const { return *m_dnd_mime_data; }

    void start_dnd_drag(ConnectionFromClient&, ByteString const& text, Gfx::Bitmap const*, Core::MimeData const&);
    void end_dnd_drag();

    void set_accepts_drag(bool);

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

    Window* foremost_popup_window(WindowStack& stack = WindowManager::the().current_window_stack());
    void request_close_fragile_windows(WindowStack& stack = WindowManager::the().current_window_stack());

    ConnectionFromClient const* active_client() const;

    Window* window_with_active_menu() { return m_window_with_active_menu; }
    Window const* window_with_active_menu() const { return m_window_with_active_menu; }
    void set_window_with_active_menu(Window*);

    Window* highlight_window() { return m_highlight_window; }
    Window const* highlight_window() const { return m_highlight_window; }
    void set_highlight_window(Window*);

    void move_to_front_and_make_active(Window&);

    Gfx::IntRect desktop_rect(Screen const&) const;
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
    Cursor const& drag_copy_cursor() const { return *m_drag_copy_cursor; }
    Cursor const& wait_cursor() const { return *m_wait_cursor; }
    Cursor const& eyedropper_cursor() const { return *m_eyedropper_cursor; }
    Cursor const& zoom_cursor() const { return *m_zoom_cursor; }

    int cursor_highlight_radius() const { return m_cursor_highlight_radius; }
    Gfx::Color cursor_highlight_color() const { return m_cursor_highlight_color; }

    Gfx::Font const& font() const;
    Gfx::Font const& window_title_font() const;

    bool set_screen_layout(ScreenLayout&&, bool, ByteString&);
    ScreenLayout get_screen_layout() const;
    bool save_screen_layout(ByteString&);

    void set_acceleration_factor(double);
    void set_scroll_step_size(unsigned);
    void set_double_click_speed(int);
    int double_click_speed() const;
    void set_mouse_buttons_switched(bool);
    bool are_mouse_buttons_switched() const;
    void set_natural_scroll(bool);
    bool is_natural_scroll() const;

    void set_active_window(Window*);
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
    void tell_wms_applet_area_size_changed(Gfx::IntSize);
    void tell_wms_super_key_pressed();
    void tell_wms_super_space_key_pressed();
    void tell_wms_super_d_key_pressed();
    void tell_wms_super_digit_key_pressed(u8);
    void tell_wms_current_window_stack_changed();

    void check_hide_geometry_overlay(Window&);

    void start_window_resize(Window&, Gfx::IntPoint, MouseButton, ResizeDirection);
    void start_window_resize(Window&, MouseEvent const&, ResizeDirection);
    void start_window_move(Window&, MouseEvent const&);
    void start_window_move(Window&, Gfx::IntPoint);

    Window const* active_fullscreen_window() const
    {
        if (active_window() && active_window()->is_fullscreen())
            return active_window();
        return nullptr;
    }

    Window* active_fullscreen_window()
    {
        if (active_window() && active_window()->is_fullscreen())
            return active_window();
        return nullptr;
    }

    bool update_theme(ByteString theme_path, ByteString theme_name, bool keep_desktop_background, Optional<ByteString> const& color_scheme_path);
    void invalidate_after_theme_or_font_change();

    bool set_theme_override(Core::AnonymousBuffer const& theme_override);
    Optional<Core::AnonymousBuffer> get_theme_override() const;
    void clear_theme_override();
    bool is_theme_overridden() { return m_theme_overridden; }
    Optional<ByteString> get_preferred_color_scheme() { return m_preferred_color_scheme; }

    bool set_hovered_window(Window*);
    void deliver_mouse_event(Window&, MouseEvent const&);

    void did_popup_a_menu(Badge<Menu>);

    void system_menu_doubleclick(Window& window, MouseEvent const& event);
    bool is_menu_doubleclick(Window& window, MouseEvent const& event) const;

    void minimize_windows(Window&, bool);
    void hide_windows(Window&, bool);
    void maximize_windows(Window&, bool);
    void set_always_on_top(Window&, bool);

    template<typename Callback>
    Window* for_each_window_in_modal_chain(Window& window, Callback callback)
    {
        Function<Window*(Window&)> recurse = [&](Window& w) -> Window* {
            if (!w.is_modal()) {
                auto decision = callback(w);
                if (decision == IterationDecision::Break)
                    return &w;
            }
            for (auto& child : w.child_windows()) {
                if (!child || child->is_destroyed() || !child->is_modal())
                    continue;
                auto decision = callback(*child);
                if (auto* result = recurse(*child))
                    return result;
                if (decision == IterationDecision::Break)
                    return child;
            }
            return nullptr;
        };
        if (auto* modeless = window.modeless_ancestor())
            return recurse(*modeless);
        return nullptr;
    }
    bool is_window_in_modal_chain(Window& chain_window, Window& other_window);

    Gfx::IntPoint get_recommended_window_position(Gfx::IntPoint desired);

    void reload_icon_bitmaps_after_scale_change();

    void reevaluate_hover_state_for_window(Window* = nullptr);
    Window* hovered_window() const { return m_hovered_window.ptr(); }

    void switch_to_window_stack(WindowStack&, Window* = nullptr, bool show_overlay = true);
    void switch_to_window_stack(u32 row, u32 col, Window* carry = nullptr, bool show_overlay = true)
    {
        if (row < window_stack_rows() && col < window_stack_columns())
            switch_to_window_stack(*(*m_window_stacks[row])[col], carry, show_overlay);
    }

    size_t window_stack_rows() const { return m_window_stacks.size(); }
    size_t window_stack_columns() const { return m_window_stacks[0]->size(); }

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
            for (auto& stack : *row) {
                IterationDecision decision = f(*stack);
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
            return false;
        default:
            return true;
        }
    }

    static constexpr bool is_fragile_window_type(WindowType window_type)
    {
        switch (window_type) {
        case WindowType::Autocomplete:
        case WindowType::Popup:
        case WindowType::Tooltip:
            return true;
        default:
            return false;
        }
    }

    void did_switch_window_stack(Badge<Compositor>, WindowStack&, WindowStack&);

    template<typename Callback>
    IterationDecision for_each_visible_window_from_back_to_front(Callback, WindowStack* = nullptr);
    template<typename Callback>
    IterationDecision for_each_visible_window_from_front_to_back(Callback, WindowStack* = nullptr);

    MultiScaleBitmaps const* overlay_rect_shadow() const { return m_overlay_rect_shadow.ptr(); }

    void apply_cursor_theme(ByteString const& name);

    void set_cursor_highlight_radius(int radius);
    void set_cursor_highlight_color(Gfx::Color color);

    bool is_cursor_highlight_enabled() const { return m_cursor_highlight_radius > 0 && m_cursor_highlight_enabled; }

    void load_system_effects();
    void apply_system_effects(Vector<bool>, ShowGeometry, TileWindow);
    SystemEffects& system_effects() { return m_system_effects; }

    RefPtr<KeymapSwitcher> keymap_switcher() { return m_keymap_switcher; }

    Window* automatic_cursor_tracking_window() { return m_automatic_cursor_tracking_window; }
    Window const* automatic_cursor_tracking_window() const { return m_automatic_cursor_tracking_window; }
    void set_automatic_cursor_tracking_window(Window* window) { m_automatic_cursor_tracking_window = window; }

    u8 last_processed_buttons() { return m_last_processed_buttons; }

    TileWindowOverlay* get_tile_window_overlay(Window&) const;
    void start_tile_window_animation(Gfx::IntRect const&);
    void stop_tile_window_animation();

    void on_add_to_quick_launch(pid_t);

private:
    explicit WindowManager(Gfx::PaletteImpl&);

    void notify_new_active_window(Window&);
    void notify_previous_active_window(Window&);
    void notify_active_window_input_preempted();
    void notify_active_window_input_restored();

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
    void pick_new_active_window(Window*);

    bool sync_config_to_disk();

    [[nodiscard]] static WindowStack& get_rendering_window_stacks(WindowStack*&);

    RefPtr<Cursor const> m_hidden_cursor;
    RefPtr<Cursor const> m_arrow_cursor;
    RefPtr<Cursor const> m_hand_cursor;
    RefPtr<Cursor const> m_help_cursor;
    RefPtr<Cursor const> m_resize_horizontally_cursor;
    RefPtr<Cursor const> m_resize_vertically_cursor;
    RefPtr<Cursor const> m_resize_diagonally_tlbr_cursor;
    RefPtr<Cursor const> m_resize_diagonally_bltr_cursor;
    RefPtr<Cursor const> m_resize_column_cursor;
    RefPtr<Cursor const> m_resize_row_cursor;
    RefPtr<Cursor const> m_i_beam_cursor;
    RefPtr<Cursor const> m_disallowed_cursor;
    RefPtr<Cursor const> m_move_cursor;
    RefPtr<Cursor const> m_drag_cursor;
    RefPtr<Cursor const> m_drag_copy_cursor;
    RefPtr<Cursor const> m_wait_cursor;
    RefPtr<Cursor const> m_crosshair_cursor;
    RefPtr<Cursor const> m_eyedropper_cursor;
    RefPtr<Cursor const> m_zoom_cursor;
    int m_cursor_highlight_radius { 0 };
    Gfx::Color m_cursor_highlight_color;
    bool m_cursor_highlight_enabled { false };

    RefPtr<MultiScaleBitmaps> m_overlay_rect_shadow;

    // Setup 2 rows 1 column by default
    Vector<NonnullOwnPtr<Vector<NonnullOwnPtr<WindowStack>, default_window_stack_columns>>, default_window_stack_rows> m_window_stacks;
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

    Gfx::IntPoint to_floating_cursor_position(Gfx::IntPoint) const;

    void show_tile_window_overlay(Window&, Screen const&, WindowTileType);

    DoubleClickInfo m_double_click_info;
    int m_double_click_speed { 0 };
    int m_max_distance_for_double_click { 4 };
    bool m_previous_event_was_super_keydown { false };
    bool m_mouse_buttons_switched { false };
    bool m_natural_scroll { false };
    bool m_theme_overridden { false };
    Optional<ByteString> m_preferred_color_scheme { OptionalNone() };

    WeakPtr<Window> m_hovered_window;
    WeakPtr<Window> m_highlight_window;
    WeakPtr<Window> m_window_with_active_menu;
    WeakPtr<Window> m_automatic_cursor_tracking_window;

    OwnPtr<WindowGeometryOverlay> m_geometry_overlay;
    OwnPtr<TileWindowOverlay> m_tile_window_overlay;
    RefPtr<Animation> m_tile_window_overlay_animation;
    WeakPtr<Window> m_move_window;
    WindowTileType m_move_window_suggested_tile { WindowTileType::None };
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
    u8 m_last_processed_buttons { MouseButton::None };

    NonnullRefPtr<WindowSwitcher> m_switcher;
    NonnullRefPtr<KeymapSwitcher> m_keymap_switcher;

    WeakPtr<Button> m_cursor_tracking_button;
    WeakPtr<Button> m_hovered_button;

    NonnullRefPtr<Gfx::PaletteImpl> m_palette;

    RefPtr<Core::ConfigFile> m_config;

    OwnPtr<DndOverlay> m_dnd_overlay;
    WeakPtr<ConnectionFromClient> m_dnd_client;
    ByteString m_dnd_text;
    bool m_dnd_accepts_drag { false };

    RefPtr<Core::MimeData const> m_dnd_mime_data;

    WindowStack* m_switching_to_window_stack { nullptr };
    Vector<WeakPtr<Window>, 4> m_carry_window_to_new_stack;

    SystemEffects m_system_effects;
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
    if (for_each_window.template operator()<WindowType::Taskbar>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::AppletArea>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Notification>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Autocomplete>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Popup>() == IterationDecision::Break)
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
    if (for_each_window.template operator()<WindowType::Popup>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Autocomplete>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Notification>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::AppletArea>() == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_window.template operator()<WindowType::Taskbar>() == IterationDecision::Break)
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
    for (auto [_, connection] : connections) {
        if (callback(connection) == IterationDecision::Break)
            return;
    }
}

}
