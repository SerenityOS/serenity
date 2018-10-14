#include "Widget.h"
#include "Event.h"
#include "EventLoop.h"
#include "WindowManager.h"
#include "Window.h"
#include "Painter.h"
#include <AK/Assertions.h>

Widget::Widget(Widget* parent)
    : Object(parent)
{
    setFont(nullptr);
    m_backgroundColor = Color::White;
    m_foregroundColor = Color::Black;
}

Widget::~Widget()
{
}

void Widget::setWindowRelativeRect(const Rect& rect)
{
    // FIXME: Make some kind of event loop driven ResizeEvent?
    m_relativeRect = rect;
    update();
}

void Widget::repaint(const Rect& rect)
{
    event(*make<PaintEvent>(rect));
}

void Widget::event(Event& event)
{
    switch (event.type()) {
    case Event::Paint:
        if (auto* win = window()) {
            if (win->isBeingDragged())
                return;
            if (!win->isVisible())
                return;
        }
        m_hasPendingPaintEvent = false;
        return paintEvent(static_cast<PaintEvent&>(event));
    case Event::Show:
        return showEvent(static_cast<ShowEvent&>(event));
    case Event::Hide:
        return hideEvent(static_cast<HideEvent&>(event));
    case Event::KeyDown:
        return keyDownEvent(static_cast<KeyEvent&>(event));
    case Event::KeyUp:
        return keyUpEvent(static_cast<KeyEvent&>(event));
    case Event::MouseMove:
        return mouseMoveEvent(static_cast<MouseEvent&>(event));
    case Event::MouseDown:
        if (auto* win = window()) {
            // FIXME: if (acceptsFocus())
            win->setFocusedWidget(this);
        }
        return mouseDownEvent(static_cast<MouseEvent&>(event));
    case Event::MouseUp:
        return mouseUpEvent(static_cast<MouseEvent&>(event));
    default:
        return Object::event(event);
    }
}

void Widget::paintEvent(PaintEvent& event)
{
    //printf("Widget::paintEvent :)\n");
    if (fillWithBackgroundColor()) {
        Painter painter(*this);
        painter.fillRect(rect(), backgroundColor());
    }
    for (auto* ch : children()) {
        auto* child = (Widget*)ch;
        child->paintEvent(event);
    }
}

void Widget::showEvent(ShowEvent&)
{
}

void Widget::hideEvent(HideEvent&)
{
}

void Widget::keyDownEvent(KeyEvent&)
{
}

void Widget::keyUpEvent(KeyEvent&)
{
}

void Widget::mouseDownEvent(MouseEvent&)
{
}

void Widget::mouseUpEvent(MouseEvent&)
{
}

void Widget::mouseMoveEvent(MouseEvent&)
{
}

void Widget::update()
{
    if (m_hasPendingPaintEvent)
        return;
    m_hasPendingPaintEvent = true;
    EventLoop::main().postEvent(this, make<PaintEvent>(rect()));
}

Widget::HitTestResult Widget::hitTest(int x, int y)
{
    // FIXME: Care about z-order.
    for (auto* ch : children()) {
        auto* child = (Widget*)ch;
        if (child->relativeRect().contains(x, y)) {
            return child->hitTest(x - child->relativeRect().x(), y - child->relativeRect().y());
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

bool Widget::isFocused() const
{
    if (auto* win = window())
        return win->isActive() &&  win->focusedWidget() == this;
    return false;
}

void Widget::setFocus(bool focus)
{
    if (focus == isFocused())
        return;
    if (auto* win = window())
        win->setFocusedWidget(this);
}

void Widget::setFont(RetainPtr<Font>&& font)
{
    if (!font)
        m_font = Font::defaultFont();
    else
        m_font = std::move(font);
}
