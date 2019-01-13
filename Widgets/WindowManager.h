#pragma once

#include "Object.h"
#include "Rect.h"
#include "Color.h"
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>

class FrameBuffer;
class MouseEvent;
class PaintEvent;
class Widget;
class Window;
class CharacterBitmap;
class GraphicsBitmap;

class WindowManager : public Object {
public:
    static WindowManager& the(); 
    void addWindow(Window&);
    void removeWindow(Window&);

    void notifyTitleChanged(Window&);
    void notifyRectChanged(Window&, const Rect& oldRect, const Rect& newRect);

    Window* activeWindow() { return m_activeWindow.ptr(); }
    void setActiveWindow(Window*);

    bool isVisible(Window&) const;

    void did_paint(Window&);

    void move_to_front(Window&);

    static void initialize();

    void redraw_cursor();

    void invalidate(const Window&);
    void invalidate(const Rect&);
    void invalidate();
    void flush(const Rect&);

private:
    WindowManager();
    virtual ~WindowManager() override;

    void processMouseEvent(MouseEvent&);
    void handleTitleBarMouseEvent(Window&, MouseEvent&);
    
    virtual void event(Event&) override;

    void compose();
    void paintWindowFrame(Window&);

    FrameBuffer& m_framebuffer;
    Rect m_screen_rect;

    Color m_activeWindowBorderColor;
    Color m_activeWindowTitleColor;

    Color m_inactiveWindowBorderColor;
    Color m_inactiveWindowTitleColor;

    HashTable<Window*> m_windows;
    InlineLinkedList<Window> m_windows_in_order;

    WeakPtr<Window> m_activeWindow;

    WeakPtr<Window> m_dragWindow;

    Point m_dragOrigin;
    Point m_dragWindowOrigin;
    Rect m_lastDragRect;
    Rect m_dragStartRect;
    Rect m_dragEndRect;

    Rect m_last_cursor_rect;

    unsigned m_recompose_count { 0 };

    RetainPtr<GraphicsBitmap> m_front_bitmap;
    RetainPtr<GraphicsBitmap> m_back_bitmap;

    Vector<Rect> m_invalidated_rects;

    bool m_pending_compose_event { false };

    RetainPtr<CharacterBitmap> m_cursor_bitmap_inner;
    RetainPtr<CharacterBitmap> m_cursor_bitmap_outer;
};
