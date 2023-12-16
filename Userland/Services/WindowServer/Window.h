/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "HitTestResult.h"
#include <AK/ByteString.h>
#include <AK/IntrusiveList.h>
#include <AK/WeakPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Rect.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/Menubar.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowFrame.h>
#include <WindowServer/WindowMode.h>
#include <WindowServer/WindowType.h>

namespace WindowServer {

class Animation;
class ConnectionFromClient;
class Cursor;
class KeyEvent;
class Menu;
class MenuItem;
class MouseEvent;
class WindowStack;

enum WMEventMask {
    WindowRectChanges = 1 << 0,
    WindowStateChanges = 1 << 1,
    WindowIconChanges = 1 << 2,
    WindowRemovals = 1 << 3,
    WorkspaceChanges = 1 << 4,
    KeymapChanged = 1 << 5,
};

enum class WindowTileType {
    None = 0,
    Left,
    Right,
    Top,
    Bottom,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    VerticallyMaximized,
    HorizontallyMaximized,
    Maximized,
};

enum class WindowMenuAction {
    MinimizeOrUnminimize = 0,
    MaximizeOrRestore,
    ToggleMenubarVisibility,
    ToggleWindowRollUp,
    Close,
    Move,
    ToggleAlwaysOnTop,
    AddToQuickLaunch,
};

enum class WindowMenuDefaultAction {
    None = 0,
    BasedOnWindowState,
    Close,
    Minimize,
    Unminimize,
    Maximize,
    Restore
};

enum class WindowMinimizedState : u32 {
    None = 0,
    Minimized,
    Hidden,
};

class Window final : public Core::EventReceiver {
    C_OBJECT(Window);

public:
    virtual ~Window() override;

    bool is_modified() const { return m_modified; }
    void set_modified(bool);

    void popup_window_menu(Gfx::IntPoint, WindowMenuDefaultAction);
    void handle_window_menu_action(WindowMenuAction);
    void window_menu_activate_default();
    void request_close();

    bool is_minimized() const { return m_minimized_state != WindowMinimizedState::None; }
    void set_minimized(bool);
    bool is_hidden() const { return m_minimized_state == WindowMinimizedState::Hidden; }
    void set_hidden(bool);
    WindowMinimizedState minimized_state() const { return m_minimized_state; }

    bool is_minimizable() const { return m_type == WindowType::Normal && m_minimizable; }
    void set_minimizable(bool);

    bool is_closeable() const { return m_closeable; }
    void set_closeable(bool);

    bool is_resizable() const { return m_type != WindowType::Popup && m_resizable && !m_fullscreen; }
    void set_resizable(bool);

    bool is_maximized() const { return m_tile_type == WindowTileType::Maximized; }
    void set_maximized(bool);

    bool is_always_on_top() const { return m_always_on_top; }
    void set_always_on_top(bool);

    bool is_fullscreen() const { return m_fullscreen; }
    void set_fullscreen(bool);

    WindowTileType tile_type() const { return m_tile_type; }
    bool is_tiled() const { return m_tile_type != WindowTileType::None; }
    void set_tiled(WindowTileType, Optional<Screen const&> = {});
    WindowTileType tile_type_based_on_rect(Gfx::IntRect const&) const;
    void check_untile_due_to_resize(Gfx::IntRect const&);
    bool set_untiled();

    Gfx::IntRect floating_rect() const { return m_floating_rect; }
    void set_floating_rect(Gfx::IntRect rect) { m_floating_rect = rect; }

    void set_forced_shadow(bool b) { m_forced_shadow = b; }
    bool has_forced_shadow() const { return m_forced_shadow; }

    bool is_occluded() const { return m_occluded; }
    void set_occluded(bool);

    bool is_movable() const { return m_type == WindowType::Normal; }

    WindowFrame& frame() { return m_frame; }
    WindowFrame const& frame() const { return m_frame; }

    ConnectionFromClient* client() { return m_client; }
    ConnectionFromClient const* client() const { return m_client; }

    WindowType type() const { return m_type; }
    int window_id() const { return m_window_id; }

    bool is_internal() const { return m_client_id == -1; }
    i32 client_id() const { return m_client_id; }

    ByteString title() const { return m_title; }
    void set_title(ByteString const&);

    ByteString computed_title() const;

    void set_hit_testing_enabled(bool value)
    {
        m_hit_testing_enabled = value;
    }
    float alpha_hit_threshold() const { return m_alpha_hit_threshold; }
    void set_alpha_hit_threshold(float threshold)
    {
        m_alpha_hit_threshold = threshold;
    }

    Optional<HitTestResult> hit_test(Gfx::IntPoint, bool include_frame = true);

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    bool is_active() const;

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    bool is_modal() const { return m_mode != WindowMode::Modeless; }
    bool is_passive() { return m_mode == WindowMode::Passive; }
    bool is_rendering_above() { return m_mode == WindowMode::RenderAbove; }

    bool is_blocking() const { return m_mode == WindowMode::Blocking; }
    Window* blocking_modal_window();

    WindowMode mode() const { return m_mode; }
    Window* modeless_ancestor();

    Gfx::IntRect rect() const { return m_rect; }
    void set_rect(Gfx::IntRect const&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }
    void set_rect_without_repaint(Gfx::IntRect const&);
    bool apply_minimum_size(Gfx::IntRect&);

    Gfx::IntSize minimum_size() const { return m_minimum_size; }
    void set_minimum_size(Gfx::IntSize);
    void set_minimum_size(int width, int height) { set_minimum_size({ width, height }); }

    void set_taskbar_rect(Gfx::IntRect const&);
    Gfx::IntRect const& taskbar_rect() const { return m_taskbar_rect; }

    void move_to(Gfx::IntPoint position) { set_rect({ position, size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }

    void move_by(Gfx::IntPoint delta) { set_position_without_repaint(position().translated(delta)); }

    Gfx::IntPoint position() const { return m_rect.location(); }
    void set_position(Gfx::IntPoint position) { set_rect({ position.x(), position.y(), width(), height() }); }
    void set_position_without_repaint(Gfx::IntPoint position) { set_rect_without_repaint({ position.x(), position.y(), width(), height() }); }

    Gfx::IntSize size() const { return m_rect.size(); }

    void invalidate(bool with_frame = true, bool re_render_frame = false);
    void invalidate(Gfx::IntRect const&, bool invalidate_frame = false);
    void invalidate_menubar();
    bool invalidate_no_notify(Gfx::IntRect const& rect, bool invalidate_frame = false);
    void invalidate_last_rendered_screen_rects();
    void invalidate_last_rendered_screen_rects_now();
    [[nodiscard]] bool should_invalidate_last_rendered_screen_rects() { return exchange(m_invalidate_last_render_rects, false); }

    void refresh_client_size();

    void prepare_dirty_rects();
    void clear_dirty_rects();
    Gfx::DisjointIntRectSet& dirty_rects() { return m_dirty_rects; }

    // Only used by WindowType::Applet. Perhaps it could be a Window subclass? I don't know.
    void set_rect_in_applet_area(Gfx::IntRect const& rect) { m_rect_in_applet_area = rect; }
    Gfx::IntRect const& rect_in_applet_area() const { return m_rect_in_applet_area; }

    Gfx::Bitmap const* backing_store() const { return m_backing_store.ptr(); }
    Gfx::Bitmap* backing_store() { return m_backing_store.ptr(); }

    void set_backing_store(RefPtr<Gfx::Bitmap> backing_store, i32 serial)
    {
        m_last_backing_store = move(m_backing_store);
        m_backing_store = move(backing_store);

        m_last_backing_store_serial = m_backing_store_serial;
        m_backing_store_serial = serial;
    }

    Gfx::IntSize backing_store_visible_size() const { return m_backing_store_visible_size; }
    void set_backing_store_visible_size(Gfx::IntSize visible_size) { m_backing_store_visible_size = visible_size; }

    void swap_backing_stores()
    {
        swap(m_backing_store, m_last_backing_store);
        swap(m_backing_store_serial, m_last_backing_store_serial);
    }

    Gfx::Bitmap* last_backing_store() { return m_last_backing_store.ptr(); }
    i32 last_backing_store_serial() const { return m_last_backing_store_serial; }

    void set_automatic_cursor_tracking_enabled(bool enabled) { m_automatic_cursor_tracking_enabled = enabled; }
    bool is_automatic_cursor_tracking() const { return m_automatic_cursor_tracking_enabled; }

    bool has_alpha_channel() const { return m_has_alpha_channel; }
    void set_has_alpha_channel(bool value);

    Gfx::IntSize size_increment() const { return m_size_increment; }
    void set_size_increment(Gfx::IntSize increment) { m_size_increment = increment; }

    Optional<Gfx::IntSize> const& resize_aspect_ratio() const { return m_resize_aspect_ratio; }
    void set_resize_aspect_ratio(Optional<Gfx::IntSize> const& ratio)
    {
        // "Tiled" means that we take up a chunk of space relative to the screen.
        // The screen can change, so "tiled" and "fixed aspect ratio" are mutually exclusive.
        // Similarly for "maximized" and "fixed aspect ratio".
        // In order to resolve this, undo those properties first:
        set_untiled();
        set_maximized(false);
        m_resize_aspect_ratio = ratio;
    }

    Gfx::IntSize base_size() const { return m_base_size; }
    void set_base_size(Gfx::IntSize size) { m_base_size = size; }

    Gfx::Bitmap const& icon() const { return *m_icon; }
    void set_icon(NonnullRefPtr<Gfx::Bitmap const>&& icon) { m_icon = move(icon); }

    void set_default_icon();

    Cursor const* cursor() const { return (m_cursor_override ? m_cursor_override : m_cursor).ptr(); }
    void set_cursor(RefPtr<Cursor const> cursor) { m_cursor = move(cursor); }
    void set_cursor_override(RefPtr<Cursor const> cursor) { m_cursor_override = move(cursor); }
    void remove_cursor_override() { m_cursor_override = nullptr; }

    void request_update(Gfx::IntRect const&, bool ignore_occlusion = false);
    Gfx::DisjointIntRectSet take_pending_paint_rects() { return move(m_pending_paint_rects); }

    void start_minimize_animation();

    void start_launch_animation(Gfx::IntRect const&);

    void recalculate_rect();

    IntrusiveListNode<Window> m_list_node;

    void detach_client(Badge<ConnectionFromClient>);

    Window* parent_window() { return m_parent_window; }
    Window const* parent_window() const { return m_parent_window; }

    void set_parent_window(Window&);

    Vector<WeakPtr<Window>>& child_windows() { return m_child_windows; }
    Vector<WeakPtr<Window>> const& child_windows() const { return m_child_windows; }

    bool is_descendant_of(Window&) const;

    void set_frameless(bool);
    bool is_frameless() const { return m_frameless; }

    bool should_show_menubar() const { return m_should_show_menubar; }

    Optional<int> progress() const { return m_progress; }
    void set_progress(Optional<int>);

    bool is_destroyed() const { return m_destroyed; }
    void destroy();

    bool is_default_positioned() const { return m_default_positioned; }
    void set_default_positioned(bool p) { m_default_positioned = p; }

    bool is_opaque() const
    {
        return !has_alpha_channel();
    }

    Gfx::DisjointIntRectSet& opaque_rects() { return m_opaque_rects; }
    Gfx::DisjointIntRectSet& transparency_rects() { return m_transparency_rects; }
    Gfx::DisjointIntRectSet& transparency_wallpaper_rects() { return m_transparency_wallpaper_rects; }
    // The affected transparency rects are the rectangles of other windows (above or below)
    // that also need to be marked dirty whenever a window's dirty rect in a transparency
    // area needs to be rendered
    auto& affected_transparency_rects() { return m_affected_transparency_rects; }

    Menubar& menubar() { return m_menubar; }
    Menubar const& menubar() const { return m_menubar; }

    void add_menu(Menu& menu);

    WindowStack& window_stack()
    {
        VERIFY(m_window_stack);
        return *m_window_stack;
    }
    WindowStack const& window_stack() const
    {
        VERIFY(m_window_stack);
        return *m_window_stack;
    }
    bool is_on_any_window_stack(Badge<WindowStack>) const { return m_window_stack != nullptr; }
    void set_window_stack(Badge<WindowStack>, WindowStack* stack) { m_window_stack = stack; }

    Vector<Screen*, default_screen_count> const& screens() const { return m_screens; }
    Vector<Screen*, default_screen_count>& screens() { return m_screens; }

    void set_moving_to_another_stack(bool value) { m_moving_to_another_stack = value; }
    bool is_moving_to_another_stack() const { return m_moving_to_another_stack; }

    void add_stealing_for_client(i32 client_id) { m_stealable_by_client_ids.append(move(client_id)); }
    void remove_stealing_for_client(i32 client_id)
    {
        m_stealable_by_client_ids.remove_all_matching([client_id](i32 approved_client_id) {
            return approved_client_id == client_id;
        });
    }
    void remove_all_stealing() { m_stealable_by_client_ids.clear(); }
    bool is_stealable_by_client(i32 client_id) const { return m_stealable_by_client_ids.contains_slow(client_id); }

    void send_resize_event_to_client();
    void send_move_event_to_client();

private:
    Window(ConnectionFromClient&, WindowType, WindowMode, int window_id, int process_id, bool minimizable, bool closeable, bool frameless, bool resizable, bool fullscreen, Window* parent_window = nullptr);
    Window(Core::EventReceiver&, WindowType);

    virtual void event(Core::Event&) override;
    void handle_mouse_event(MouseEvent const&);
    void handle_keydown_event(KeyEvent const&);
    void add_child_window(Window&);
    void ensure_window_menu();
    void update_window_menu_items();
    void tile_type_changed(Optional<Screen const&> = {});
    ErrorOr<Optional<ByteString>> compute_title_username(ConnectionFromClient* client);

    void exit_roll_up_mode();

    ConnectionFromClient* m_client { nullptr };

    WeakPtr<Window> m_parent_window;
    Vector<WeakPtr<Window>> m_child_windows;

    Menubar m_menubar;

    ByteString m_title;
    Optional<ByteString> m_title_username;
    Gfx::IntRect m_rect;
    Gfx::IntRect m_saved_before_roll_up_rect;
    Gfx::IntRect m_saved_nonfullscreen_rect;
    Gfx::IntRect m_taskbar_rect;
    Vector<Screen*, default_screen_count> m_screens;
    Gfx::DisjointIntRectSet m_dirty_rects;
    Gfx::DisjointIntRectSet m_opaque_rects;
    Gfx::DisjointIntRectSet m_transparency_rects;
    Gfx::DisjointIntRectSet m_transparency_wallpaper_rects;
    HashMap<Window*, Gfx::DisjointIntRectSet> m_affected_transparency_rects;
    WindowType m_type { WindowType::Normal };
    WindowMode m_mode { WindowMode::Modeless };
    bool m_automatic_cursor_tracking_enabled { false };
    bool m_visible { true };
    bool m_has_alpha_channel { false };
    bool m_minimizable { false };
    bool m_closeable { false };
    bool m_frameless { false };
    bool m_forced_shadow { false };
    bool m_resizable { false };
    Optional<Gfx::IntSize> m_resize_aspect_ratio {};
    WindowMinimizedState m_minimized_state { WindowMinimizedState::None };
    bool m_fullscreen { false };
    bool m_destroyed { false };
    bool m_default_positioned { false };
    bool m_invalidated { true };
    bool m_invalidated_all { true };
    bool m_invalidated_frame { true };
    bool m_hit_testing_enabled { true };
    bool m_modified { false };
    bool m_always_on_top { false };
    bool m_moving_to_another_stack { false };
    bool m_invalidate_last_render_rects { false };
    Vector<i32> m_stealable_by_client_ids;
    WindowTileType m_tile_type { WindowTileType::None };
    Gfx::IntRect m_floating_rect;
    bool m_occluded { false };
    RefPtr<Gfx::Bitmap> m_backing_store;
    RefPtr<Gfx::Bitmap> m_last_backing_store;
    Gfx::IntSize m_backing_store_visible_size {};
    Gfx::IntSize m_backup_backing_store_visible_size {};
    i32 m_backing_store_serial { -1 };
    i32 m_last_backing_store_serial { -1 };
    int m_window_id { -1 };
    i32 m_client_id { -1 };
    float m_alpha_hit_threshold { 0.0f };
    Gfx::IntSize m_size_increment;
    Gfx::IntSize m_base_size;
    Gfx::IntSize m_minimum_size { 0, 0 };
    NonnullRefPtr<Gfx::Bitmap const> m_icon;
    RefPtr<Cursor const> m_cursor;
    RefPtr<Cursor const> m_cursor_override;
    WindowFrame m_frame;
    Gfx::DisjointIntRectSet m_pending_paint_rects;
    Gfx::IntRect m_rect_in_applet_area;
    RefPtr<Menu> m_window_menu;
    MenuItem* m_window_menu_minimize_item { nullptr };
    MenuItem* m_window_menu_maximize_item { nullptr };
    MenuItem* m_window_menu_move_item { nullptr };
    MenuItem* m_window_menu_close_item { nullptr };
    MenuItem* m_window_menu_roll_up_item { nullptr };
    MenuItem* m_window_menu_always_on_top_item { nullptr };
    MenuItem* m_window_menu_menubar_visibility_item { nullptr };
    MenuItem* m_window_menu_add_to_quick_launch_item { nullptr };
    Optional<int> m_progress;
    bool m_should_show_menubar { true };
    bool m_should_show_window_content { true };
    WindowStack* m_window_stack { nullptr };
    RefPtr<Animation> m_animation;

    Optional<pid_t> m_process_id {};

public:
    using List = IntrusiveList<&Window::m_list_node>;
};

}
