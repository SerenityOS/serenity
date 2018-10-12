#include "Widget.h"
#include "Event.h"
#include "EventLoop.h"
#include "WindowManager.h"
#include <AK/Assertions.h>

Widget::Widget(Widget* parent)
    : Object(parent)
{
    m_backgroundColor = Color(255, 255, 255);
    m_foregroundColor = Color(0, 0, 0);
}

Widget::~Widget()
{
}

void Widget::setRect(const Rect& rect)
{
    // FIXME: Make some kind of event loop driven ResizeEvent?
    m_rect = rect;
    update();
}

void Widget::event(Event& event)
{
    switch (event.type()) {
    case Event::Paint:
        m_hasPendingPaintEvent = false;
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

void Widget::onPaint(PaintEvent& event)
{
    //printf("Widget::onPaint :)\n");
    for (auto* ch : children()) {
        auto* child = (Widget*)ch;
        child->onPaint(event);
    }
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
    if (m_hasPendingPaintEvent)
        return;
    m_hasPendingPaintEvent = true;
    EventLoop::main().postEvent(this, make<PaintEvent>());
}

Widget::HitTestResult Widget::hitTest(int x, int y)
{
    // FIXME: Care about z-order.
    for (auto* ch : children()) {
        auto* child = (Widget*)ch;
        if (child->rect().contains(x, y)) {
            return child->hitTest(x - child->rect().x(), y - child->rect().y());
        }
    }
    return { this, x, y };
}

void Widget::setWindow(Window* window)
{
    if (m_window == window)
        return;
    m_window = window;
}
