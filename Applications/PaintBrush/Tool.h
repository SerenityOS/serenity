#pragma once

class GMouseEvent;
class PaintableWidget;

class Tool {
public:
    virtual ~Tool();

    virtual const char* class_name() const = 0;

    virtual void on_mousedown(PaintableWidget&, GMouseEvent&) { }
    virtual void on_mousemove(PaintableWidget&, GMouseEvent&) { }
    virtual void on_mouseup(PaintableWidget&, GMouseEvent&) { }

protected:
    Tool();
};
