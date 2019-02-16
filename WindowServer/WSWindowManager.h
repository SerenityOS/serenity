#pragma once

#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Color.h>
#include <SharedGraphics/Painter.h>
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>
#include <AK/HashMap.h>
#include "WSMessageReceiver.h"
#include "WSMenuBar.h"
#include <WindowServer/WSWindowType.h>

class WSAPIClientRequest;
class WSScreen;
class WSMenuBar;
class WSMouseEvent;
class WSClientWantsToPaintMessage;
class WSWindow;
class WSClientConnection;
class CharacterBitmap;
class GraphicsBitmap;

enum class IterationDecision { Continue, Abort };

class WSWindowManager : public WSMessageReceiver {
public:
    static WSWindowManager& the();

    WSWindowManager();
    virtual ~WSWindowManager() override;

    void add_window(WSWindow&);
    void remove_window(WSWindow&);

    void notify_title_changed(WSWindow&);
    void notify_rect_changed(WSWindow&, const Rect& oldRect, const Rect& newRect);
    void notify_client_changed_app_menubar(WSClientConnection&);

    WSWindow* active_window() { return m_active_window.ptr(); }
    int active_client_id() const;

    void move_to_front(WSWindow&);

    void invalidate_cursor();
    void draw_cursor();
    void draw_menubar();

    Rect menubar_rect() const;
    WSMenuBar* current_menubar() { return m_current_menubar.ptr(); }
    void set_current_menubar(WSMenuBar*);
    WSMenu* current_menu() { return m_current_menu.ptr(); }
    void set_current_menu(WSMenu*);

    void invalidate(const WSWindow&);
    void invalidate(const WSWindow&, const Rect&);
    void invalidate(const Rect&);
    void invalidate();
    void flush(const Rect&);

    Font& font() { return *m_font; }
    const Font& font() const { return *m_font; }

    void close_menu(WSMenu&);
    void close_menubar(WSMenuBar&);
    Color menu_selection_color() const { return m_menu_selection_color; }
    int menubar_menu_margin() const;

    int framebuffer_fd() const { return m_framebuffer_fd; }
    void set_framebuffer_fd(int fd) { m_framebuffer_fd = fd; }

private:
    void process_mouse_event(WSMouseEvent&);
    void handle_menu_mouse_event(WSMenu&, WSMouseEvent&);
    void handle_menubar_mouse_event(WSMouseEvent&);
    void handle_titlebar_mouse_event(WSWindow&, WSMouseEvent&);
    void handle_close_button_mouse_event(WSWindow&, WSMouseEvent&);
    void handle_client_request(WSAPIClientRequest&);

    void set_active_window(WSWindow*);
    template<typename Callback> IterationDecision for_each_visible_window_of_type_from_back_to_front(WSWindowType, Callback);
    template<typename Callback> IterationDecision for_each_visible_window_of_type_from_front_to_back(WSWindowType, Callback);
    template<typename Callback> IterationDecision for_each_visible_window_from_front_to_back(Callback);
    template<typename Callback> IterationDecision for_each_visible_window_from_back_to_front(Callback);
    template<typename Callback> void for_each_active_menubar_menu(Callback);
    void close_current_menu();
    
    virtual void on_message(WSMessage&) override;

    void compose();
    void paint_window_frame(WSWindow&);
    void flip_buffers();

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

    HashMap<int, OwnPtr<WSWindow>> m_windows_by_id;
    HashTable<WSWindow*> m_windows;
    InlineLinkedList<WSWindow> m_windows_in_order;

    WeakPtr<WSWindow> m_active_window;

    WeakPtr<WSWindow> m_drag_window;

    Point m_drag_origin;
    Point m_drag_window_origin;
    Rect m_last_drag_rect;
    Rect m_drag_start_rect;
    Rect m_drag_end_rect;

    Rect m_last_cursor_rect;

    unsigned m_compose_count { 0 };
    unsigned m_flush_count { 0 };

    RetainPtr<GraphicsBitmap> m_front_bitmap;
    RetainPtr<GraphicsBitmap> m_back_bitmap;

    Vector<Rect> m_dirty_rects;

    bool m_pending_compose_event { false };

    RetainPtr<CharacterBitmap> m_cursor_bitmap_inner;
    RetainPtr<CharacterBitmap> m_cursor_bitmap_outer;

    OwnPtr<Painter> m_back_painter;
    OwnPtr<Painter> m_front_painter;

    RetainPtr<Font> m_font;

    String m_wallpaper_path;
    RetainPtr<GraphicsBitmap> m_wallpaper;

    bool m_flash_flush { false };
    bool m_buffers_are_flipped { false };

    OwnPtr<WSMenu> m_system_menu;
    Color m_menu_selection_color;
    WeakPtr<WSMenuBar> m_current_menubar;
    WeakPtr<WSMenu> m_current_menu;

    int m_framebuffer_fd { -1 };
};
