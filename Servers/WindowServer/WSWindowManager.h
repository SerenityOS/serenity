#pragma once

#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Color.h>
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/DisjointRectSet.h>
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>
#include <AK/HashMap.h>
#include "WSMenuBar.h"
#include <WindowServer/WSWindowSwitcher.h>
#include <WindowServer/WSWindowType.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WSCPUMonitor.h>
#include <LibCore/CElapsedTimer.h>

class WSAPIClientRequest;
class WSScreen;
class WSMenuBar;
class WSMouseEvent;
class WSClientWantsToPaintMessage;
class WSWindow;
class WSClientConnection;
class WSWindowSwitcher;
class GraphicsBitmap;
class WSButton;

enum class ResizeDirection { None, Left, UpLeft, Up, UpRight, Right, DownRight, Down, DownLeft };

class WSWindowManager : public CObject {
    friend class WSWindowFrame;
    friend class WSWindowSwitcher;
public:
    static WSWindowManager& the();

    WSWindowManager();
    virtual ~WSWindowManager() override;

    void add_window(WSWindow&);
    void remove_window(WSWindow&);

    void notify_title_changed(WSWindow&);
    void notify_rect_changed(WSWindow&, const Rect& oldRect, const Rect& newRect);
    void notify_minimization_state_changed(WSWindow&);
    void notify_client_changed_app_menubar(WSClientConnection&);

    Rect maximized_window_rect(const WSWindow&) const;

    WSWindow* active_window() { return m_active_window.ptr(); }
    const WSClientConnection* active_client() const;

    WSWindow* highlight_window() { return m_highlight_window.ptr(); }
    void set_highlight_window(WSWindow*);

    void move_to_front_and_make_active(WSWindow&);

    void invalidate_cursor();
    void draw_cursor();
    void draw_menubar();
    void draw_window_switcher();
    void draw_geometry_label();

    Rect menubar_rect() const;
    WSMenuBar* current_menubar() { return m_current_menubar.ptr(); }
    void set_current_menubar(WSMenuBar*);
    WSMenu* current_menu() { return m_current_menu.ptr(); }
    void set_current_menu(WSMenu*);

    void invalidate(const WSWindow&);
    void invalidate(const WSWindow&, const Rect&);
    void invalidate(const Rect&, bool should_schedule_compose_event = true);
    void invalidate();
    void recompose_immediately();
    void flush(const Rect&);

    const Font& font() const;
    const Font& window_title_font() const;
    const Font& menu_font() const;
    const Font& app_menu_font() const;

    void close_menu(WSMenu&);
    void close_menubar(WSMenuBar&);
    Color menu_selection_color() const { return m_menu_selection_color; }
    int menubar_menu_margin() const;

    void set_resolution(int width, int height);

    bool set_wallpaper(const String& path, Function<void(bool)>&& callback);
    String wallpaper_path() const { return m_wallpaper_path; }

    const WSCursor& active_cursor() const;
    Rect current_cursor_rect() const;

    const WSCursor& arrow_cursor() const { return *m_arrow_cursor; }
    const WSCursor& resize_horizontally_cursor() const { return *m_resize_horizontally_cursor; }
    const WSCursor& resize_vertically_cursor() const { return *m_resize_vertically_cursor; }
    const WSCursor& resize_diagonally_tlbr_cursor() const { return *m_resize_diagonally_tlbr_cursor; }
    const WSCursor& resize_diagonally_bltr_cursor() const { return *m_resize_diagonally_bltr_cursor; }
    const WSCursor& i_beam_cursor() const { return *m_i_beam_cursor; }
    const WSCursor& disallowed_cursor() const { return *m_disallowed_cursor; }
    const WSCursor& move_cursor() const { return *m_move_cursor; }

    void set_active_window(WSWindow*);
    void set_hovered_button(WSButton*);

    WSButton* cursor_tracking_button() { return m_cursor_tracking_button.ptr(); }
    void set_cursor_tracking_button(WSButton*);

    void set_resize_candidate(WSWindow&, ResizeDirection);
    void clear_resize_candidate();

    bool any_opaque_window_contains_rect(const Rect&);
    bool any_opaque_window_above_this_one_contains_rect(const WSWindow&, const Rect&);

    void tell_wm_listeners_window_state_changed(WSWindow&);
    void tell_wm_listeners_window_icon_changed(WSWindow&);
    void tell_wm_listeners_window_rect_changed(WSWindow&);

    void start_window_resize(WSWindow&, const Point&, MouseButton);
    void start_window_resize(WSWindow&, const WSMouseEvent&);

    const WSWindow* active_fullscreen_window() const { return (m_active_window && m_active_window->is_fullscreen()) ? m_active_window : nullptr; }
    WSWindow* active_fullscreen_window() { return (m_active_window && m_active_window->is_fullscreen()) ? m_active_window : nullptr; }

private:
    void process_mouse_event(WSMouseEvent&, WSWindow*& hovered_window);
    void process_event_for_doubleclick(WSWindow& window, WSMouseEvent& event);
    void deliver_mouse_event(WSWindow& window, WSMouseEvent& event);
    bool process_ongoing_window_resize(const WSMouseEvent&, WSWindow*& hovered_window);
    bool process_ongoing_window_drag(WSMouseEvent&, WSWindow*& hovered_window);
    void handle_menu_mouse_event(WSMenu&, const WSMouseEvent&);
    void handle_menubar_mouse_event(const WSMouseEvent&);
    void handle_close_button_mouse_event(WSWindow&, const WSMouseEvent&);
    void start_window_drag(WSWindow&, const WSMouseEvent&);
    void handle_client_request(const WSAPIClientRequest&);
    void set_hovered_window(WSWindow*);
    template<typename Callback> IterationDecision for_each_visible_window_of_type_from_back_to_front(WSWindowType, Callback, bool ignore_highlight = false);
    template<typename Callback> IterationDecision for_each_visible_window_of_type_from_front_to_back(WSWindowType, Callback, bool ignore_highlight = false);
    template<typename Callback> IterationDecision for_each_visible_window_from_front_to_back(Callback);
    template<typename Callback> IterationDecision for_each_visible_window_from_back_to_front(Callback);
    template<typename Callback> void for_each_window_listening_to_wm_events(Callback);
    template<typename Callback> void for_each_window(Callback);
    template<typename Callback> void for_each_active_menubar_menu(Callback);
    void close_current_menu();
    virtual void event(CEvent&) override;
    void compose();
    void paint_window_frame(const WSWindow&);
    void flip_buffers();
    void tick_clock();
    void tell_wm_listener_about_window(WSWindow& listener, WSWindow&);
    void tell_wm_listener_about_window_icon(WSWindow& listener, WSWindow&);
    void tell_wm_listener_about_window_rect(WSWindow& listener, WSWindow&);
    void pick_new_active_window();
    void finish_setting_wallpaper(const String& path, Retained<GraphicsBitmap>&&);

    WSScreen& m_screen;
    Rect m_screen_rect;

    Color m_background_color;
    Color m_active_window_border_color;
    Color m_active_window_border_color2;
    Color m_active_window_title_color;
    Color m_inactive_window_border_color;
    Color m_inactive_window_border_color2;
    Color m_inactive_window_title_color;
    Color m_dragging_window_border_color;
    Color m_dragging_window_border_color2;
    Color m_dragging_window_title_color;
    Color m_highlight_window_border_color;
    Color m_highlight_window_border_color2;
    Color m_highlight_window_title_color;

    HashMap<int, OwnPtr<WSWindow>> m_windows_by_id;
    HashTable<WSWindow*> m_windows;
    InlineLinkedList<WSWindow> m_windows_in_order;

    struct DoubleClickInfo
    {
        CElapsedTimer& click_clock(MouseButton);
        void reset() {
            m_left_click_clock = CElapsedTimer();
            m_right_click_clock = CElapsedTimer();
            m_middle_click_clock = CElapsedTimer();
        }

        WeakPtr<WSWindow> m_clicked_window;
        CElapsedTimer m_left_click_clock;
        CElapsedTimer m_right_click_clock;
        CElapsedTimer m_middle_click_clock;
    };
    DoubleClickInfo m_double_click_info;

    WeakPtr<WSWindow> m_active_window;
    WeakPtr<WSWindow> m_hovered_window;
    WeakPtr<WSWindow> m_highlight_window;

    WeakPtr<WSWindow> m_drag_window;
    Point m_drag_origin;
    Point m_drag_window_origin;

    WeakPtr<WSWindow> m_resize_window;
    WeakPtr<WSWindow> m_resize_candidate;
    MouseButton m_resizing_mouse_button { MouseButton::None };
    Rect m_resize_window_original_rect;
    Point m_resize_origin;
    ResizeDirection m_resize_direction { ResizeDirection::None };

    Rect m_last_cursor_rect;
    Rect m_last_geometry_label_rect;

    unsigned m_compose_count { 0 };
    unsigned m_flush_count { 0 };

    RetainPtr<GraphicsBitmap> m_front_bitmap;
    RetainPtr<GraphicsBitmap> m_back_bitmap;

    DisjointRectSet m_dirty_rects;

    bool m_pending_compose_event { false };

    RetainPtr<WSCursor> m_arrow_cursor;
    RetainPtr<WSCursor> m_resize_horizontally_cursor;
    RetainPtr<WSCursor> m_resize_vertically_cursor;
    RetainPtr<WSCursor> m_resize_diagonally_tlbr_cursor;
    RetainPtr<WSCursor> m_resize_diagonally_bltr_cursor;
    RetainPtr<WSCursor> m_i_beam_cursor;
    RetainPtr<WSCursor> m_disallowed_cursor;
    RetainPtr<WSCursor> m_move_cursor;

    OwnPtr<Painter> m_back_painter;
    OwnPtr<Painter> m_front_painter;

    String m_wallpaper_path;
    RetainPtr<GraphicsBitmap> m_wallpaper;

    bool m_flash_flush { false };
    bool m_buffers_are_flipped { false };

    byte m_keyboard_modifiers { 0 };

    OwnPtr<WSMenu> m_system_menu;
    Color m_menu_selection_color;
    WeakPtr<WSMenuBar> m_current_menubar;
    WeakPtr<WSMenu> m_current_menu;

    WSWindowSwitcher m_switcher;

    String m_username;
    WeakPtr<WSButton> m_cursor_tracking_button;
    WeakPtr<WSButton> m_hovered_button;

    WSCPUMonitor m_cpu_monitor;
};

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_of_type_from_back_to_front(WSWindowType type, Callback callback, bool ignore_highlight)
{
    bool do_highlight_window_at_end = false;
    for (auto* window = m_windows_in_order.head(); window; window = window->next()) {
        if (!window->is_visible())
            continue;
        if (window->is_minimized())
            continue;
        if (window->type() != type)
            continue;
        if (!ignore_highlight && m_highlight_window == window) {
            do_highlight_window_at_end = true;
            continue;
        }
        if (callback(*window) == IterationDecision::Abort)
            return IterationDecision::Abort;
    }
    if (do_highlight_window_at_end) {
        if (callback(*m_highlight_window) == IterationDecision::Abort)
            return IterationDecision::Abort;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_from_back_to_front(Callback callback)
{
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Normal, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Taskbar, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Tooltip, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Menu, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    return for_each_visible_window_of_type_from_back_to_front(WSWindowType::WindowSwitcher, callback);
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_of_type_from_front_to_back(WSWindowType type, Callback callback, bool ignore_highlight)
{
    if (!ignore_highlight && m_highlight_window && m_highlight_window->type() == type && m_highlight_window->is_visible()) {
        if (callback(*m_highlight_window) == IterationDecision::Abort)
            return IterationDecision::Abort;
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
        if (callback(*window) == IterationDecision::Abort)
            return IterationDecision::Abort;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_from_front_to_back(Callback callback)
{
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::WindowSwitcher, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Menu, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Taskbar, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Tooltip, callback) == IterationDecision::Abort)
        return IterationDecision::Abort;
    return for_each_visible_window_of_type_from_front_to_back(WSWindowType::Normal, callback);
}

template<typename Callback>
void WSWindowManager::for_each_window_listening_to_wm_events(Callback callback)
{
    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->listens_to_wm_events())
            continue;
        if (callback(*window) == IterationDecision::Abort)
            return;
    }
}

template<typename Callback>
void WSWindowManager::for_each_window(Callback callback)
{
    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (callback(*window) == IterationDecision::Abort)
            return;
    }
}
