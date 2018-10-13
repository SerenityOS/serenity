#pragma once

#include "Object.h"
#include "Rect.h"
#include "Color.h"
#include <AK/HashTable.h>
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
    void paintWindowFrames();

    void notifyTitleChanged(Window&);
    void notifyRectChanged(Window&, const Rect& oldRect, const Rect& newRect);

    Widget* rootWidget() { return m_rootWidget; }
    void setRootWidget(Widget*);

    Window* activeWindow() { return m_activeWindow.ptr(); }
    void setActiveWindow(Window*);

private:
    WindowManager();
    ~WindowManager();

    void processMouseEvent(MouseEvent&);
    void handleTitleBarMouseEvent(Window&, MouseEvent&);
    void handlePaintEvent(PaintEvent&);
    void repaintAfterMove(const Rect& oldRect, const Rect& newRect);
    
    virtual void event(Event&) override;

    Color m_activeWindowBorderColor;
    Color m_activeWindowTitleColor;

    Color m_inactiveWindowBorderColor;
    Color m_inactiveWindowTitleColor;

    void paintWindowFrame(Window&);
    HashTable<Window*> m_windows;
    Widget* m_rootWidget { nullptr };

    WeakPtr<Window> m_activeWindow;

    WeakPtr<Window> m_dragWindow;

    Point m_dragOrigin;
    Point m_dragWindowOrigin;
    Rect m_lastDragRect;
    Rect m_dragStartRect;
    Rect m_dragEndRect;
};
