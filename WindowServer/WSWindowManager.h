#pragma once

#include <Widgets/Rect.h>
#include <Widgets/Color.h>
#include <Widgets/Painter.h>
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>
#include <AK/Lock.h>
#include "WSEventReceiver.h"

class WSScreen;
class MouseEvent;
class PaintEvent;
class WSWindow;
class CharacterBitmap;
class GraphicsBitmap;

class WSWindowManager : public WSEventReceiver {
public:
    static WSWindowManager& the();
    void add_window(WSWindow&);
    void remove_window(WSWindow&);

    void notify_title_changed(WSWindow&);
    void notify_rect_changed(WSWindow&, const Rect& oldRect, const Rect& newRect);

    WSWindow* active_window() { return m_active_window.ptr(); }

    void move_to_front(WSWindow&);

    static void initialize();

    void draw_cursor();

    void invalidate(const WSWindow&);
    void invalidate(const WSWindow&, const Rect&);
    void invalidate(const Rect&);
    void invalidate();
    void flush(const Rect&);

private:
    WSWindowManager();
    virtual ~WSWindowManager() override;

    void process_mouse_event(MouseEvent&);
    void handle_titlebar_mouse_event(WSWindow&, MouseEvent&);

    void set_active_window(WSWindow*);
    
    virtual void event(WSEvent&) override;

    void compose();
    void paint_window_frame(WSWindow&);

    WSScreen& m_screen;
    Rect m_screen_rect;

    Color m_active_window_border_color;
    Color m_active_window_title_color;

    Color m_inactive_window_border_color;
    Color m_inactive_window_title_color;

    Color m_dragging_window_border_color;
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

    mutable Lock m_lock;

    bool m_flash_flush { false };
};
