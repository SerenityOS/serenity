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

#include <AK/InlineLinkedList.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Rect.h>
#include <WindowServer/WindowFrame.h>
#include <WindowServer/WindowType.h>

namespace WindowServer {

class ClientConnection;
class Cursor;
class Menu;
class MenuItem;
class MouseEvent;

enum WMEventMask {
    WindowRectChanges = 1 << 0,
    WindowStateChanges = 1 << 1,
    WindowIconChanges = 1 << 2,
    WindowRemovals = 1 << 3,
};

enum class WindowTileType {
    None = 0,
    Left,
    Right,
};

enum class PopupMenuItem {
    Minimize = 0,
    Maximize,
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

class Window final : public Core::Object
    , public InlineLinkedListNode<Window> {
    C_OBJECT(Window)
public:
    Window(ClientConnection&, WindowType, int window_id, bool modal, bool minimizable, bool frameless, bool resizable, bool fullscreen, bool accessory, Window* parent_window = nullptr);
    Window(Core::Object&, WindowType);
    virtual ~Window() override;

    void popup_window_menu(const Gfx::IntPoint&, WindowMenuDefaultAction);
    void window_menu_activate_default();
    void request_close();

    unsigned wm_event_mask() const { return m_wm_event_mask; }
    void set_wm_event_mask(unsigned mask) { m_wm_event_mask = mask; }

    bool is_minimized() const { return m_minimized; }
    void set_minimized(bool);

    bool is_minimizable() const { return m_minimizable; }
    void set_minimizable(bool);

    bool is_resizable() const { return m_resizable && !m_fullscreen; }
    void set_resizable(bool);

    bool is_maximized() const { return m_maximized; }
    void set_maximized(bool);

    bool is_fullscreen() const { return m_fullscreen; }
    void set_fullscreen(bool);

    WindowTileType tiled() const { return m_tiled; }
    void set_tiled(WindowTileType);

    bool is_occluded() const { return m_occluded; }
    void set_occluded(bool);

    bool is_movable() const
    {
        return m_type == WindowType::Normal;
    }

    WindowFrame& frame() { return m_frame; }
    const WindowFrame& frame() const { return m_frame; }

    Window* is_blocked_by_modal_window();

    bool listens_to_wm_events() const { return m_listens_to_wm_events; }

    ClientConnection* client() { return m_client; }
    const ClientConnection* client() const { return m_client; }

    WindowType type() const { return m_type; }
    int window_id() const { return m_window_id; }

    bool is_internal() const { return m_client_id == -1; }
    i32 client_id() const { return m_client_id; }

    String title() const { return m_title; }
    void set_title(const String&);

    float opacity() const { return m_opacity; }
    void set_opacity(float);

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    bool is_active() const;

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    bool is_modal() const;
    bool is_modal_dont_unparent() const { return m_modal && m_parent_window; }

    Gfx::IntRect rect() const { return m_rect; }
    void set_rect(const Gfx::IntRect&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }
    void set_rect_without_repaint(const Gfx::IntRect&);

    void set_taskbar_rect(const Gfx::IntRect&);
    const Gfx::IntRect& taskbar_rect() const { return m_taskbar_rect; }

    void move_to(const Gfx::IntPoint& position) { set_rect({ position, size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }

    void move_by(const Gfx::IntPoint& delta) { set_position_without_repaint(position().translated(delta)); }

    Gfx::IntPoint position() const { return m_rect.location(); }
    void set_position(const Gfx::IntPoint& position) { set_rect({ position.x(), position.y(), width(), height() }); }
    void set_position_without_repaint(const Gfx::IntPoint& position) { set_rect_without_repaint({ position.x(), position.y(), width(), height() }); }

    Gfx::IntSize size() const { return m_rect.size(); }

    void invalidate(bool with_frame = true);
    void invalidate(const Gfx::IntRect&, bool with_frame = false);
    bool invalidate_no_notify(const Gfx::IntRect& rect, bool with_frame = false);

    void prepare_dirty_rects();
    void clear_dirty_rects();
    Gfx::DisjointRectSet& dirty_rects() { return m_dirty_rects; }

    virtual void event(Core::Event&) override;

    // Only used by WindowType::MenuApplet. Perhaps it could be a Window subclass? I don't know.
    void set_rect_in_menubar(const Gfx::IntRect& rect) { m_rect_in_menubar = rect; }
    const Gfx::IntRect& rect_in_menubar() const { return m_rect_in_menubar; }

    const Gfx::Bitmap* backing_store() const { return m_backing_store.ptr(); }
    Gfx::Bitmap* backing_store() { return m_backing_store.ptr(); }

    void set_backing_store(RefPtr<Gfx::Bitmap>&& backing_store)
    {
        m_last_backing_store = move(m_backing_store);
        m_backing_store = move(backing_store);
    }

    void swap_backing_stores()
    {
        swap(m_backing_store, m_last_backing_store);
    }

    Gfx::Bitmap* last_backing_store() { return m_last_backing_store.ptr(); }

    void set_global_cursor_tracking_enabled(bool);
    void set_automatic_cursor_tracking_enabled(bool enabled) { m_automatic_cursor_tracking_enabled = enabled; }
    bool global_cursor_tracking() const { return m_global_cursor_tracking_enabled || m_automatic_cursor_tracking_enabled; }

    bool has_alpha_channel() const { return m_has_alpha_channel; }
    void set_has_alpha_channel(bool value) { m_has_alpha_channel = value; }

    Gfx::IntSize size_increment() const { return m_size_increment; }
    void set_size_increment(const Gfx::IntSize& increment) { m_size_increment = increment; }

    const Optional<Gfx::IntSize>& resize_aspect_ratio() const { return m_resize_aspect_ratio; }
    void set_resize_aspect_ratio(const Optional<Gfx::IntSize>& ratio) { m_resize_aspect_ratio = ratio; }

    Gfx::IntSize base_size() const { return m_base_size; }
    void set_base_size(const Gfx::IntSize& size) { m_base_size = size; }

    const Gfx::Bitmap& icon() const { return *m_icon; }
    void set_icon(NonnullRefPtr<Gfx::Bitmap>&& icon) { m_icon = move(icon); }

    void set_default_icon();

    const Cursor* cursor() const { return m_cursor.ptr(); }
    void set_cursor(RefPtr<Cursor> cursor) { m_cursor = move(cursor); }

    void request_update(const Gfx::IntRect&, bool ignore_occlusion = false);
    Gfx::DisjointRectSet take_pending_paint_rects() { return move(m_pending_paint_rects); }

    bool has_taskbar_rect() const { return m_have_taskbar_rect; };
    bool in_minimize_animation() const { return m_minimize_animation_step != -1; }
    int minimize_animation_index() const { return m_minimize_animation_step; }
    void step_minimize_animation() { m_minimize_animation_step += 1; }
    void start_minimize_animation();
    void end_minimize_animation() { m_minimize_animation_step = -1; }

    Gfx::IntRect tiled_rect(WindowTileType) const;
    void recalculate_rect();

    // For InlineLinkedList.
    // FIXME: Maybe make a ListHashSet and then WindowManager can just use that.
    Window* m_next { nullptr };
    Window* m_prev { nullptr };

    void detach_client(Badge<ClientConnection>);

    Window* parent_window() { return m_parent_window; }
    const Window* parent_window() const { return m_parent_window; }

    void set_parent_window(Window&);

    Vector<WeakPtr<Window>>& child_windows() { return m_child_windows; }
    const Vector<WeakPtr<Window>>& child_windows() const { return m_child_windows; }

    Vector<WeakPtr<Window>>& accessory_windows() { return m_accessory_windows; }
    const Vector<WeakPtr<Window>>& accessory_windows() const { return m_accessory_windows; }

    bool is_descendant_of(Window&) const;

    void set_accessory(bool accessory) { m_accessory = accessory; }
    bool is_accessory() const;
    bool is_accessory_of(Window&) const;

    void set_frameless(bool frameless) { m_frameless = frameless; }
    bool is_frameless() const { return m_frameless; }

    int progress() const { return m_progress; }
    void set_progress(int);

    bool is_destroyed() const { return m_destroyed; }
    void destroy();

    bool default_positioned() const { return m_default_positioned; }
    void set_default_positioned(bool p) { m_default_positioned = p; }

    bool is_invalidated() const { return m_invalidated; }

    bool is_opaque() const
    {
        if (opacity() < 1.0f)
            return false;
        if (has_alpha_channel())
            return false;
        return true;
    }

    Gfx::DisjointRectSet& opaque_rects() { return m_opaque_rects; }
    Gfx::DisjointRectSet& transparency_rects() { return m_transparency_rects; }
    Gfx::DisjointRectSet& transparency_wallpaper_rects() { return m_transparency_wallpaper_rects; }

private:
    void handle_mouse_event(const MouseEvent&);
    void update_menu_item_text(PopupMenuItem item);
    void update_menu_item_enabled(PopupMenuItem item);
    void add_child_window(Window&);
    void add_accessory_window(Window&);
    void ensure_window_menu();
    void modal_unparented();

    ClientConnection* m_client { nullptr };

    WeakPtr<Window> m_parent_window;
    Vector<WeakPtr<Window>> m_child_windows;
    Vector<WeakPtr<Window>> m_accessory_windows;

    String m_title;
    Gfx::IntRect m_rect;
    Gfx::IntRect m_saved_nonfullscreen_rect;
    Gfx::IntRect m_taskbar_rect;
    Gfx::DisjointRectSet m_dirty_rects;
    Gfx::DisjointRectSet m_opaque_rects;
    Gfx::DisjointRectSet m_transparency_rects;
    Gfx::DisjointRectSet m_transparency_wallpaper_rects;
    WindowType m_type { WindowType::Normal };
    bool m_global_cursor_tracking_enabled { false };
    bool m_automatic_cursor_tracking_enabled { false };
    bool m_visible { true };
    bool m_has_alpha_channel { false };
    bool m_modal { false };
    bool m_minimizable { false };
    bool m_frameless { false };
    bool m_resizable { false };
    Optional<Gfx::IntSize> m_resize_aspect_ratio {};
    bool m_listens_to_wm_events { false };
    bool m_minimized { false };
    bool m_maximized { false };
    bool m_fullscreen { false };
    bool m_accessory { false };
    bool m_destroyed { false };
    bool m_default_positioned { false };
    bool m_have_taskbar_rect { false };
    bool m_invalidated { true };
    bool m_invalidated_all { true };
    bool m_invalidated_frame { true };
    WindowTileType m_tiled { WindowTileType::None };
    Gfx::IntRect m_untiled_rect;
    bool m_occluded { false };
    RefPtr<Gfx::Bitmap> m_backing_store;
    RefPtr<Gfx::Bitmap> m_last_backing_store;
    int m_window_id { -1 };
    i32 m_client_id { -1 };
    float m_opacity { 1 };
    Gfx::IntSize m_size_increment;
    Gfx::IntSize m_base_size;
    NonnullRefPtr<Gfx::Bitmap> m_icon;
    RefPtr<Cursor> m_cursor;
    WindowFrame m_frame;
    unsigned m_wm_event_mask { 0 };
    Gfx::DisjointRectSet m_pending_paint_rects;
    Gfx::IntRect m_unmaximized_rect;
    Gfx::IntRect m_rect_in_menubar;
    RefPtr<Menu> m_window_menu;
    MenuItem* m_window_menu_minimize_item { nullptr };
    MenuItem* m_window_menu_maximize_item { nullptr };
    MenuItem* m_window_menu_close_item { nullptr };
    int m_minimize_animation_step { -1 };
    int m_progress { -1 };
};

}
