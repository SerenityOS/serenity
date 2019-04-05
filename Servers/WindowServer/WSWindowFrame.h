#pragma once

class Painter;
class Rect;
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

private:
    void handle_close_button_mouse_event(const WSMouseEvent&);

    WSWindow& m_window;
};
