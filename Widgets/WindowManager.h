#pragma once

class Window;

#include "Object.h"
#include "Rect.h"
#include <AK/HashTable.h>

class WindowManager : public Object {
public:
    static WindowManager& the();

    void addWindow(Window&);
    void paintWindowFrames();

    void notifyTitleChanged(Window&);
    void notifyRectChanged(Window&, const Rect& oldRect, const Rect& newRect);

private:
    WindowManager();
    ~WindowManager();

    void paintWindowFrame(Window&);

    HashTable<Window*> m_windows;
};
