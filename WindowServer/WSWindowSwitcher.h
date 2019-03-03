#pragma once

#include <SharedGraphics/Rect.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>

class Painter;
class WSKeyEvent;
class WSWindow;

class WSWindowSwitcher {
public:
    WSWindowSwitcher();
    ~WSWindowSwitcher();

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    void show() { set_visible(true); }
    void hide() { set_visible(false); }

    void on_key_event(const WSKeyEvent&);
    void invalidate();

    void draw(Painter&);

    int item_height() { return 20; }
    int padding() { return 8; }

    WSWindow* selected_window();

private:

    Rect m_rect;
    bool m_visible { false };
    Vector<WeakPtr<WSWindow>> m_windows;
    int m_selected_index { 0 };
};
