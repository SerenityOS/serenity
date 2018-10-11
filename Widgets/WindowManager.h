#pragma once

#include "Object.h"
#include "Rect.h"
#include <AK/HashTable.h>

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

    virtual void event(Event&) override;

    void paintWindowFrame(Window&);
    HashTable<Window*> m_windows;
    Widget* m_rootWidget { nullptr };
};
