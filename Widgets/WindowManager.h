#pragma once

class Widget;

#include "Object.h"
#include <AK/HashTable.h>

class WindowManager : public Object {
public:
    static WindowManager& the();

    void addWindow(Widget&);
    void paintWindowFrames();

    void notifyTitleChanged(Widget&);

private:
    WindowManager();
    ~WindowManager();

    void paintWindowFrame(Widget&);

    HashTable<Widget*> m_windows;
};
