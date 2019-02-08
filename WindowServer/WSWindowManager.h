#pragma once

#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Color.h>
#include <SharedGraphics/Painter.h>
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>
#include <AK/Lock.h>
#include <AK/CircularQueue.h>
#include "WSMessageReceiver.h"

class WSScreen;
class WSMouseEvent;
class WSClientWantsToPaintMessage;
class WSWindow;
class CharacterBitmap;
class GraphicsBitmap;

class WSWindowManager : public WSMessageReceiver {
public:
    static WSWindowManager& the();
    void add_window(WSWindow&);
    void remove_window(WSWindow&);

    void notify_title_changed(WSWindow&);
    void notify_rect_changed(WSWindow&, const Rect& oldRect, const Rect& newRect);

    WSWindow* active_window() { return m_active_window.ptr(); }

    void move_to_front(WSWindow&);

    void invalidate_cursor();
    void draw_cursor();

    void invalidate(const WSWindow&);
    void invalidate(const WSWindow&, const Rect&);
    void invalidate(const Rect&);
    void invalidate();
    void flush(const Rect&);

    Font& font() { return *m_font; }
    const Font& font() const { return *m_font; }

private:
    WSWindowManager();
    virtual ~WSWindowManager() override;

    void process_mouse_event(WSMouseEvent&);
    void handle_titlebar_mouse_event(WSWindow&, WSMouseEvent&);
    void handle_close_button_mouse_event(WSWindow&, WSMouseEvent&);

    void set_active_window(WSWindow*);
    
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

    Lockable<String> m_wallpaper_path;
    RetainPtr<GraphicsBitmap> m_wallpaper;

    mutable Lock m_lock;

    bool m_flash_flush { false };
    bool m_buffers_are_flipped { false };
};
