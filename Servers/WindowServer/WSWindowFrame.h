#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtrVector.h>

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

    Rect title_bar_rect() const;
    Rect title_bar_icon_rect() const;
    Rect title_bar_text_rect() const;

    void did_set_maximized(Badge<WSWindow>, bool);

private:
    WSWindow& m_window;
    NonnullOwnPtrVector<WSButton> m_buttons;
    WSButton* m_maximize_button { nullptr };
    WSButton* m_minimize_button { nullptr };
};
