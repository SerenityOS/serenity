#pragma once

#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CElapsedTimer.h>
#include <LibDraw/Color.h>
#include <LibDraw/DisjointRectSet.h>
#include <LibDraw/Painter.h>
#include <LibDraw/Rect.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WSMenuBar.h>
#include <WindowServer/WSMenuManager.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowSwitcher.h>
#include <WindowServer/WSWindowType.h>

class WSScreen;
class WSMenuBar;
class WSMouseEvent;
class WSClientWantsToPaintMessage;
class WSWindow;
class WSClientConnection;
class WSWindowSwitcher;
class GraphicsBitmap;
class WSButton;

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

class WSWindowManager : public CObject {
    C_OBJECT(WSWindowManager)

    friend class WSCompositor;
    friend class WSWindowFrame;
    friend class WSWindowSwitcher;

public:
    static WSWindowManager& the();

    WSWindowManager();
    virtual ~WSWindowManager() override;

    RefPtr<CConfigFile> wm_config() const { return m_wm_config; }
    void reload_config(bool);

    void add_window(WSWindow&);
    void remove_window(WSWindow&);

    void notify_title_changed(WSWindow&);
    void notify_rect_changed(WSWindow&, const Rect& oldRect, const Rect& newRect);
    void notify_minimization_state_changed(WSWindow&);
    void notify_client_changed_app_menubar(WSClientConnection&);

    Rect maximized_window_rect(const WSWindow&) const;

    WSClientConnection* dnd_client() { return m_dnd_client.ptr(); }
    const String& dnd_text() const { return m_dnd_text; }
    const GraphicsBitmap* dnd_bitmap() const { return m_dnd_bitmap; }
    Rect dnd_rect() const;

    void start_dnd_drag(WSClientConnection&, const String& text, GraphicsBitmap*);
    void end_dnd_drag();

    WSWindow* active_window() { return m_active_window.ptr(); }
    const WSClientConnection* active_client() const;
    bool active_window_is_modal() const { return m_active_window && m_active_window->is_modal(); }

    WSWindow* highlight_window() { return m_highlight_window.ptr(); }
    void set_highlight_window(WSWindow*);

    void move_to_front_and_make_active(WSWindow&);

    void draw_window_switcher();

    WSMenuManager& menu_manager() { return m_menu_manager; }
    const WSMenuManager& menu_manager() const { return m_menu_manager; }

    Rect menubar_rect() const;
    WSMenuBar* current_menubar() { return m_current_menubar.ptr(); }
    void set_current_menubar(WSMenuBar*);
    WSMenu* system_menu() { return m_system_menu.ptr(); }

    const WSCursor& active_cursor() const;
    const WSCursor& arrow_cursor() const { return *m_arrow_cursor; }
    const WSCursor& hand_cursor() const { return *m_hand_cursor; }
    const WSCursor& resize_horizontally_cursor() const { return *m_resize_horizontally_cursor; }
    const WSCursor& resize_vertically_cursor() const { return *m_resize_vertically_cursor; }
    const WSCursor& resize_diagonally_tlbr_cursor() const { return *m_resize_diagonally_tlbr_cursor; }
    const WSCursor& resize_diagonally_bltr_cursor() const { return *m_resize_diagonally_bltr_cursor; }
    const WSCursor& i_beam_cursor() const { return *m_i_beam_cursor; }
    const WSCursor& disallowed_cursor() const { return *m_disallowed_cursor; }
    const WSCursor& move_cursor() const { return *m_move_cursor; }

    void invalidate(const WSWindow&);
    void invalidate(const WSWindow&, const Rect&);
    void invalidate(const Rect&);
    void invalidate();
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

    void set_active_window(WSWindow*);
    void set_hovered_button(WSButton*);

    WSButton* cursor_tracking_button() { return m_cursor_tracking_button.ptr(); }
    void set_cursor_tracking_button(WSButton*);

    void set_resize_candidate(WSWindow&, ResizeDirection);
    void clear_resize_candidate();
    ResizeDirection resize_direction_of_window(const WSWindow&);

    bool any_opaque_window_contains_rect(const Rect&);
    bool any_opaque_window_above_this_one_contains_rect(const WSWindow&, const Rect&);

    void tell_wm_listeners_window_state_changed(WSWindow&);
    void tell_wm_listeners_window_icon_changed(WSWindow&);
    void tell_wm_listeners_window_rect_changed(WSWindow&);

    void start_window_resize(WSWindow&, const Point&, MouseButton);
    void start_window_resize(WSWindow&, const WSMouseEvent&);

    const WSWindow* active_fullscreen_window() const { return (m_active_window && m_active_window->is_fullscreen()) ? m_active_window : nullptr; }
    WSWindow* active_fullscreen_window() { return (m_active_window && m_active_window->is_fullscreen()) ? m_active_window : nullptr; }

    template<typename Callback>
    void for_each_active_menubar_menu(Callback callback)
    {
        callback(*m_system_menu);
        if (m_current_menubar)
            m_current_menubar->for_each_menu(callback);
    }

    WSMenu* find_internal_menu_by_id(int);

private:
    NonnullRefPtr<WSCursor> get_cursor(const String& name);
    NonnullRefPtr<WSCursor> get_cursor(const String& name, const Point& hotspot);

    void process_mouse_event(WSMouseEvent&, WSWindow*& hovered_window);
    void process_event_for_doubleclick(WSWindow& window, WSMouseEvent& event);
    void deliver_mouse_event(WSWindow& window, WSMouseEvent& event);
    bool process_ongoing_window_resize(const WSMouseEvent&, WSWindow*& hovered_window);
    bool process_ongoing_window_drag(WSMouseEvent&, WSWindow*& hovered_window);
    bool process_ongoing_drag(WSMouseEvent&, WSWindow*& hovered_window);
    void start_window_drag(WSWindow&, const WSMouseEvent&);
    void set_hovered_window(WSWindow*);
    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_back_to_front(WSWindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_front_to_back(WSWindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_visible_window_from_front_to_back(Callback);
    template<typename Callback>
    IterationDecision for_each_visible_window_from_back_to_front(Callback);
    template<typename Callback>
    void for_each_window_listening_to_wm_events(Callback);
    template<typename Callback>
    void for_each_window(Callback);

    virtual void event(CEvent&) override;
    void paint_window_frame(const WSWindow&);
    void tell_wm_listener_about_window(WSWindow& listener, WSWindow&);
    void tell_wm_listener_about_window_icon(WSWindow& listener, WSWindow&);
    void tell_wm_listener_about_window_rect(WSWindow& listener, WSWindow&);
    void pick_new_active_window();

    RefPtr<WSCursor> m_arrow_cursor;
    RefPtr<WSCursor> m_hand_cursor;
    RefPtr<WSCursor> m_resize_horizontally_cursor;
    RefPtr<WSCursor> m_resize_vertically_cursor;
    RefPtr<WSCursor> m_resize_diagonally_tlbr_cursor;
    RefPtr<WSCursor> m_resize_diagonally_bltr_cursor;
    RefPtr<WSCursor> m_i_beam_cursor;
    RefPtr<WSCursor> m_disallowed_cursor;
    RefPtr<WSCursor> m_move_cursor;

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

    InlineLinkedList<WSWindow> m_windows_in_order;

    struct DoubleClickInfo {
        struct ClickMetadata {
            CElapsedTimer clock;
            Point last_position;
        };

        ClickMetadata& metadata_for_button(MouseButton);

        void reset()
        {
            m_left = {};
            m_right = {};
            m_middle = {};
        }

        WeakPtr<WSWindow> m_clicked_window;

    private:
        ClickMetadata m_left;
        ClickMetadata m_right;
        ClickMetadata m_middle;
    };
    DoubleClickInfo m_double_click_info;
    int m_double_click_speed { 0 };
    int m_max_distance_for_double_click { 4 };

    WeakPtr<WSWindow> m_active_window;
    WeakPtr<WSWindow> m_hovered_window;
    WeakPtr<WSWindow> m_highlight_window;
    WeakPtr<WSWindow> m_active_input_window;

    WeakPtr<WSWindow> m_drag_window;
    Point m_drag_origin;
    Point m_drag_window_origin;

    WeakPtr<WSWindow> m_resize_window;
    WeakPtr<WSWindow> m_resize_candidate;
    MouseButton m_resizing_mouse_button { MouseButton::None };
    Rect m_resize_window_original_rect;
    Point m_resize_origin;
    ResizeDirection m_resize_direction { ResizeDirection::None };

    u8 m_keyboard_modifiers { 0 };

    RefPtr<WSMenu> m_system_menu;
    Color m_menu_selection_color;
    WeakPtr<WSMenuBar> m_current_menubar;

    WSWindowSwitcher m_switcher;
    WSMenuManager m_menu_manager;

    WeakPtr<WSButton> m_cursor_tracking_button;
    WeakPtr<WSButton> m_hovered_button;

    RefPtr<CConfigFile> m_wm_config;

    struct AppMetadata {
        String executable;
        String name;
        String icon_path;
        String category;
    };
    Vector<AppMetadata> m_apps;
    HashMap<String, NonnullRefPtr<WSMenu>> m_app_category_menus;

    WeakPtr<WSClientConnection> m_dnd_client;
    String m_dnd_text;
    RefPtr<GraphicsBitmap> m_dnd_bitmap;
};

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_of_type_from_back_to_front(WSWindowType type, Callback callback, bool ignore_highlight)
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
IterationDecision WSWindowManager::for_each_visible_window_from_back_to_front(Callback callback)
{
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Normal, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Taskbar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Tooltip, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Menubar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WSWindowType::Menu, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_visible_window_of_type_from_back_to_front(WSWindowType::WindowSwitcher, callback);
}

template<typename Callback>
IterationDecision WSWindowManager::for_each_visible_window_of_type_from_front_to_back(WSWindowType type, Callback callback, bool ignore_highlight)
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
IterationDecision WSWindowManager::for_each_visible_window_from_front_to_back(Callback callback)
{
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::WindowSwitcher, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Menu, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Menubar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Taskbar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WSWindowType::Tooltip, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_visible_window_of_type_from_front_to_back(WSWindowType::Normal, callback);
}

template<typename Callback>
void WSWindowManager::for_each_window_listening_to_wm_events(Callback callback)
{
    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (!window->listens_to_wm_events())
            continue;
        if (callback(*window) == IterationDecision::Break)
            return;
    }
}

template<typename Callback>
void WSWindowManager::for_each_window(Callback callback)
{
    for (auto* window = m_windows_in_order.tail(); window; window = window->prev()) {
        if (callback(*window) == IterationDecision::Break)
            return;
    }
}
