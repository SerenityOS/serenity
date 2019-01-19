#include "Window.h"
#include "Event.h"
#include "EventLoop.h"
#include <SharedGraphics/GraphicsBitmap.h>

Window::Window(int window_id)
    : m_window_id(window_id)
{
}

Window::~Window()
{
}

void Window::set_title(String&& title)
{
    if (m_title == title)
        return;

    m_title = move(title);
}
void Window::set_rect(const Rect& rect)
{
    if (m_rect == rect)
        return;
    m_rect = rect;
    dbgprintf("Window::setRect %d,%d %dx%d\n", m_rect.x(), m_rect.y(), m_rect.width(), m_rect.height());
}

void Window::event(Event& event)
{
}

bool Window::is_visible() const
{
    return false;
}

void Window::close()
{
}

