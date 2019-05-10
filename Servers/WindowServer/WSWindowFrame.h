#pragma once

#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class Painter;
class Rect;
class WSButton;
class WSMouseEvent;
class WSWindow;

class WSWindowFrame {
public:
    WSWindowFrame(WSWindow&);
    ~WSWindowFrame();

    Rect rect() const;
    void paint(Painter&);
    void on_mouse_event(const WSMouseEvent&);
    void notify_window_rect_changed(const Rect& old_rect, const Rect& new_rect);
    void invalidate_title_bar();

private:
    Rect title_bar_rect() const;
    Rect title_bar_icon_rect() const;
    Rect title_bar_text_rect() const;

    WSWindow& m_window;
    Vector<OwnPtr<WSButton>> m_buttons;
};
