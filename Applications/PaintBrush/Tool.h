#pragma once

#include "PaintableWidget.h"

class Tool {
public:
    virtual ~Tool();

    virtual const char* class_name() const = 0;

    virtual void on_mousedown(GMouseEvent&) {}
    virtual void on_mousemove(GMouseEvent&) {}
    virtual void on_mouseup(GMouseEvent&) {}
    virtual void on_contextmenu(GContextMenuEvent&) {}
    virtual void on_second_paint(GPaintEvent&) {}
    virtual void on_keydown(GKeyEvent&) {}

    void clear() { m_widget = nullptr; }
    void setup(PaintableWidget& widget) { m_widget = widget.make_weak_ptr(); }

protected:
    Tool();
    WeakPtr<PaintableWidget> m_widget;
};
