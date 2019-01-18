#pragma once

#include <Widgets/Rect.h>
#include <Widgets/Color.h>
#include <Widgets/Painter.h>
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>
#include <AK/Lock.h>
#include "WSEventReceiver.h"

class WSFrameBuffer;
class MouseEvent;
class PaintEvent;
class WSWindow;
class CharacterBitmap;
class GraphicsBitmap;

class WSWindowManager : public WSEventReceiver {
public:
    static WSWindowManager& the();
    void addWindow(WSWindow&);
    void removeWindow(WSWindow&);

    void notifyTitleChanged(WSWindow&);
    void notifyRectChanged(WSWindow&, const Rect& oldRect, const Rect& newRect);

    WSWindow* activeWindow() { return m_active_window.ptr(); }

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

    void processMouseEvent(MouseEvent&);
    void handleTitleBarMouseEvent(WSWindow&, MouseEvent&);

    void set_active_window(WSWindow*);
    
    virtual void event(WSEvent&) override;

    void compose();
    void paintWindowFrame(WSWindow&);

    WSFrameBuffer& m_framebuffer;
    Rect m_screen_rect;

    Color m_activeWindowBorderColor;
    Color m_activeWindowTitleColor;

    Color m_inactiveWindowBorderColor;
    Color m_inactiveWindowTitleColor;

    HashTable<WSWindow*> m_windows;
    InlineLinkedList<WSWindow> m_windows_in_order;

    WeakPtr<WSWindow> m_active_window;

    WeakPtr<WSWindow> m_dragWindow;

    Point m_dragOrigin;
    Point m_dragWindowOrigin;
    Rect m_lastDragRect;
    Rect m_dragStartRect;
    Rect m_dragEndRect;

    Rect m_last_cursor_rect;

    unsigned m_recompose_count { 0 };
    unsigned m_flush_count { 0 };

    RetainPtr<GraphicsBitmap> m_front_bitmap;
    RetainPtr<GraphicsBitmap> m_back_bitmap;

    Vector<Rect> m_invalidated_rects;

    bool m_pending_compose_event { false };

    RetainPtr<CharacterBitmap> m_cursor_bitmap_inner;
    RetainPtr<CharacterBitmap> m_cursor_bitmap_outer;

    OwnPtr<Painter> m_back_painter;
    OwnPtr<Painter> m_front_painter;

    mutable Lock m_lock;
};
