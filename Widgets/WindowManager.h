#pragma once

#include "Object.h"
#include "Rect.h"
#include "Color.h"
#include <AK/HashTable.h>

class MouseEvent;
class PaintEvent;
class Widget;
class Window;

class WindowManager : public Object {
public:
    static WindowManager& the(); 
    void addWindow(Window&);
    void paintWindowFrames();

    void notifyTitleChanged(Window&);
    void notifyRectChanged(Window&, const Rect& oldRect, const Rect& newRect);

    Widget* rootWidget() { return m_rootWidget; }
    void setRootWidget(Widget*);

private:
    WindowManager();
    ~WindowManager();

    void processMouseEvent(MouseEvent&);
    void handleTitleBarMouseEvent(Window&, MouseEvent&);
    void handlePaintEvent(PaintEvent&);

    virtual void event(Event&) override;

    Color m_windowBorderColor;
    Color m_windowTitleColor;

    void paintWindowFrame(Window&);
    HashTable<Window*> m_windows;
    Widget* m_rootWidget { nullptr };

    Window* m_dragWindow { nullptr };
    Point m_dragOrigin;
    Point m_dragWindowOrigin;
    Rect m_lastDragRect;
};
