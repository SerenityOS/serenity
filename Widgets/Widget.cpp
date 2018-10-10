#include "Widget.h"
#include "Event.h"
#include "EventLoop.h"
#include <AK/Assertions.h>

Widget::Widget(Widget* parent)
    : Object(parent)
{
}

Widget::~Widget()
{
}

void Widget::event(Event& event)
{
    switch (event.type()) {
    case Event::Paint:
        return onPaint(static_cast<PaintEvent&>(event));
    case Event::Show:
        return onShow(static_cast<ShowEvent&>(event));
    case Event::Hide:
        return onHide(static_cast<HideEvent&>(event));
    case Event::KeyDown:
        return onKeyDown(static_cast<KeyEvent&>(event));
    case Event::KeyUp:
        return onKeyUp(static_cast<KeyEvent&>(event));
    case Event::MouseMove:
        return onMouseMove(static_cast<MouseEvent&>(event));
    case Event::MouseDown:
        return onMouseDown(static_cast<MouseEvent&>(event));
    case Event::MouseUp:
        return onMouseUp(static_cast<MouseEvent&>(event));
    default:
        return Object::event(event);
    }
}

void Widget::onPaint(PaintEvent&)
{
}

void Widget::onShow(ShowEvent&)
{
    update();
}

void Widget::onHide(HideEvent&)
{
}

void Widget::onKeyDown(KeyEvent&)
{
}

void Widget::onKeyUp(KeyEvent&)
{
}

void Widget::onMouseDown(MouseEvent&)
{
}

void Widget::onMouseUp(MouseEvent&)
{
}

void Widget::onMouseMove(MouseEvent&)
{
}

void Widget::update()
{
    EventLoop::main().postEvent(this, make<PaintEvent>());
}

