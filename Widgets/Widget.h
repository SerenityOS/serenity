#pragma once

#include "Event.h"
#include "Object.h"

class Widget : public Object {
public:
    explicit Widget(Widget* parent = nullptr);
    virtual ~Widget();

    virtual void event(Event&);
    virtual void onPaint(PaintEvent&);
    virtual void onShow(ShowEvent&);
    virtual void onHide(HideEvent&);
    virtual void onKeyDown(KeyEvent&);
    virtual void onKeyUp(KeyEvent&);
    virtual void onMouseMove(MouseEvent&);
    virtual void onMouseDown(MouseEvent&);
    virtual void onMouseUp(MouseEvent&);

    void update();

private:
    int m_x { 0 };
    int m_y { 0 };
};
