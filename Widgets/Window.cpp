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
    delete m_mainWidget;
    m_mainWidget = nullptr;
    if (parent())
        parent()->removeChild(*this);
    WindowManager::the().removeWindow(*this);
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

void Window::repaint()
{
    event(*make<PaintEvent>());
}

void Window::event(Event& event)
{
    if (event.isMouseEvent()) {
        auto& me = static_cast<MouseEvent&>(event);
        //printf("Window{%p}: %s %d,%d\n", this, me.name(), me.x(), me.y());
        if (m_mainWidget) {
            auto result = m_mainWidget->hitTest(me.x(), me.y());
            //printf("hit test for %d,%d found: %s{%p} %d,%d\n", me.x(), me.y(), result.widget->class_name(), result.widget, result.localX, result.localY);
            // FIXME: Re-use the existing event instead of crafting a new one?
            auto localEvent = make<MouseEvent>(event.type(), result.localX, result.localY, me.button());
            return result.widget->event(*localEvent);
        }
        return Object::event(event);
    }

    if (event.isPaintEvent()) {
        auto& pe = static_cast<PaintEvent&>(event);
        printf("Window[\"%s\"]: paintEvent %d,%d %dx%d\n", class_name(),
                pe.rect().x(),
                pe.rect().y(),
                pe.rect().width(),
                pe.rect().height());

        if (isBeingDragged()) {
            // Ignore paint events during window drag.
            return;
        }
        if (m_mainWidget) {
            if (pe.rect().isEmpty())
                return m_mainWidget->event(*make<PaintEvent>(m_mainWidget->rect()));
            else
                return m_mainWidget->event(event);
        }
        return Object::event(event);
    }

    if (event.isKeyEvent()) {
        if (m_focusedWidget)
            return m_focusedWidget->event(event);
        return Object::event(event);
    }

    return Object::event(event);
}

bool Window::isActive() const
{
    return WindowManager::the().activeWindow() == this;
}

bool Window::isVisible() const
{
    return WindowManager::the().isVisible(const_cast<Window&>(*this));
}

void Window::setFocusedWidget(Widget* widget)
{
    if (m_focusedWidget.ptr() == widget)
        return;
    auto* previouslyFocusedWidget = m_focusedWidget.ptr();
    if (!widget) {
        m_focusedWidget = nullptr;
    } else {
        m_focusedWidget = widget->makeWeakPtr();
        m_focusedWidget->repaint(Rect());
    }
    if (previouslyFocusedWidget)
        previouslyFocusedWidget->repaint(Rect());
}

void Window::close()
{
    WindowManager::the().removeWindow(*this);
    deleteLater();
}

