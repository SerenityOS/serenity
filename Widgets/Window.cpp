#include "Window.h"
#include "WindowManager.h"

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
