#include "Window.h"
#include "WindowManager.h"
#include "Event.h"
#include "Widget.h"

Window::Window(Object* parent)
    : Object(parent)
{
    WindowManager::the().addWindow(*this);
}

Window::~Window()
{
}

void Window::setMainWidget(Widget* widget)
{
    if (m_mainWidget == widget)
        return;

    m_mainWidget = widget;
    widget->setWindow(this);
}

void Window::setTitle(String&& title)
{
    if (m_title == title)
        return;

    m_title = std::move(title);
    WindowManager::the().notifyTitleChanged(*this);
}


void Window::setRect(const Rect& rect)
{
    if (m_rect == rect)
        return;
    auto oldRect = m_rect;
    m_rect = rect;
    WindowManager::the().notifyRectChanged(*this, oldRect, m_rect);
}

void Window::event(Event& event)
{
    if (event.isMouseEvent()) {
        auto& me = static_cast<MouseEvent&>(event);
        printf("Window{%p}: %s %d,%d\n", this, me.name(), me.x(), me.y());
        if (m_mainWidget) {
            auto result = m_mainWidget->hitTest(me.x(), me.y());
            //printf("hit test for %d,%d found: %s{%p} %d,%d\n", me.x(), me.y(), result.widget->className(), result.widget, result.localX, result.localY);
            // FIXME: Re-use the existing event instead of crafting a new one?
            auto localEvent = make<MouseEvent>(event.type(), result.localX, result.localY, me.button());
            result.widget->event(*localEvent);
            return m_mainWidget->event(event);
        }
        return Object::event(event);
    }

    if (event.isPaintEvent()) {
        if (isBeingDragged()) {
            // Ignore paint events during window drag.
            return;
        }
        if (m_mainWidget)
            return m_mainWidget->event(event);
        return Object::event(event);
    }

    return Object::event(event);
}
