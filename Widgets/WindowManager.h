#pragma once

#include "Object.h"
#include "Rect.h"
#include "Color.h"
#include <AK/HashTable.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>

class MouseEvent;
class PaintEvent;
class Widget;
class Window;

class WindowManager : public Object {
public:
    static WindowManager& the(); 
    void addWindow(Window&);
    void removeWindow(Window&);

    void notifyTitleChanged(Window&);
    void notifyRectChanged(Window&, const Rect& oldRect, const Rect& newRect);

    Widget* rootWidget() { return m_rootWidget; }
    void setRootWidget(Widget*);

    Window* activeWindow() { return m_activeWindow.ptr(); }
    void setActiveWindow(Window*);

    bool isVisible(Window&) const;

    void did_paint(Window&);

    void repaint();
    void move_to_front(Window&);

    static void initialize();

    void redraw_cursor();

private:
    WindowManager();
    ~WindowManager();

    void processMouseEvent(MouseEvent&);
    void handleTitleBarMouseEvent(Window&, MouseEvent&);
    void handlePaintEvent(PaintEvent&);
    
    virtual void event(Event&) override;

    Color m_activeWindowBorderColor;
    Color m_activeWindowTitleColor;

    Color m_inactiveWindowBorderColor;
    Color m_inactiveWindowTitleColor;

    void recompose();
    void paintWindowFrame(Window&);
    HashTable<Window*> m_windows;
    InlineLinkedList<Window> m_windows_in_order;
    Widget* m_rootWidget { nullptr };

    WeakPtr<Window> m_activeWindow;

    WeakPtr<Window> m_dragWindow;

    Point m_dragOrigin;
    Point m_dragWindowOrigin;
    Rect m_lastDragRect;
    Rect m_dragStartRect;
    Rect m_dragEndRect;

    Point m_last_drawn_cursor_location;

    unsigned m_recompose_count { 0 };
    unsigned m_frontmost_only_compose_count { 0 };
};
