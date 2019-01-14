#include "Widget.h"
#include "Event.h"
#include "EventLoop.h"
#include "GraphicsBitmap.h"
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

void Widget::setWindowRelativeRect(const Rect& rect, bool should_update)
{
    // FIXME: Make some kind of event loop driven ResizeEvent?
    m_relativeRect = rect;
    if (should_update)
        update();
}

void Widget::repaint(const Rect& rect)
{
    if (auto* w = window())
        w->repaint(rect);
}

void Widget::event(Event& event)
{
    switch (event.type()) {
    case Event::Paint:
        m_hasPendingPaintEvent = false;
        if (auto* win = window()) {
            if (win->isBeingDragged())
                return;
            if (!win->isVisible())
                return;
        }
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
        // FIXME: Focus self if needed.
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
        painter.fill_rect(rect(), backgroundColor());
    }
    for (auto* ch : children()) {
        auto* child = (Widget*)ch;
        child->event(event);
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
    auto* w = window();
    if (!w)
        return;
    if (m_hasPendingPaintEvent)
        return;
    m_hasPendingPaintEvent = true;
    EventLoop::main().postEvent(w, make<PaintEvent>(relativeRect()));
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
    // FIXME: Implement.
    return false;
}

void Widget::setFocus(bool focus)
{
    if (focus == isFocused())
        return;
    // FIXME: Implement.
}

void Widget::setFont(RetainPtr<Font>&& font)
{
    if (!font)
        m_font = Font::defaultFont();
    else
        m_font = move(font);
}

GraphicsBitmap* Widget::backing()
{
    if (auto* w = window())
        return w->backing();
    return nullptr;
}
