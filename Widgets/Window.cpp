#include "Window.h"
#include "WindowManager.h"
#include "Event.h"
#include "EventLoop.h"
#include "Widget.h"
#include "Process.h"

Window::Window(Process& process, int window_id)
    : m_process(process)
    , m_window_id(window_id)
{
    WindowManager::the().addWindow(*this);
}

Window::~Window()
{
    WindowManager::the().removeWindow(*this);
}

void Window::setTitle(String&& title)
{
    if (m_title == title)
        return;

    m_title = move(title);
    WindowManager::the().notifyTitleChanged(*this);
}

void Window::setRect(const Rect& rect)
{
    if (m_rect == rect)
        return;
    auto oldRect = m_rect;
    m_rect = rect;
    dbgprintf("Window::setRect %d,%d %dx%d\n", m_rect.x(), m_rect.y(), m_rect.width(), m_rect.height());
    m_backing = GraphicsBitmap::create(m_process, m_rect.size());
    WindowManager::the().notifyRectChanged(*this, oldRect, m_rect);
}

// FIXME: Just use the same types.
static GUI_MouseButton to_api(MouseButton button)
{
    switch (button) {
    case MouseButton::None: return GUI_MouseButton::NoButton;
    case MouseButton::Left: return GUI_MouseButton::Left;
    case MouseButton::Right: return GUI_MouseButton::Right;
    case MouseButton::Middle: return GUI_MouseButton::Middle;
    }
}

void Window::event(Event& event)
{
    GUI_Event gui_event;
    gui_event.window_id = window_id();

    switch (event.type()) {
    case Event::Paint:
        gui_event.type = GUI_Event::Type::Paint;
        gui_event.paint.rect = static_cast<PaintEvent&>(event).rect();
        break;
    case Event::MouseMove:
        gui_event.type = GUI_Event::Type::MouseMove;
        gui_event.mouse.position = static_cast<MouseEvent&>(event).position();
        break;
    case Event::MouseDown:
        gui_event.type = GUI_Event::Type::MouseDown;
        gui_event.mouse.position = static_cast<MouseEvent&>(event).position();
        gui_event.mouse.button = to_api(static_cast<MouseEvent&>(event).button());
        break;
    case Event::MouseUp:
        gui_event.type = GUI_Event::Type::MouseUp;
        gui_event.mouse.position = static_cast<MouseEvent&>(event).position();
        gui_event.mouse.button = to_api(static_cast<MouseEvent&>(event).button());
        break;
    case Event::KeyDown:
        gui_event.type = GUI_Event::Type::KeyDown;
        gui_event.key.character = static_cast<KeyEvent&>(event).text()[0];
        break;
    }

    if (gui_event.type == GUI_Event::Type::Invalid)
        return;

    {
        LOCKER(m_process.gui_events_lock());
        m_process.gui_events().append(move(gui_event));
    }
}

void Window::did_paint()
{
    WindowManager::the().did_paint(*this);
}

bool Window::isActive() const
{
    return WindowManager::the().activeWindow() == this;
}

bool Window::isVisible() const
{
    return WindowManager::the().isVisible(const_cast<Window&>(*this));
}

void Window::close()
{
    WindowManager::the().removeWindow(*this);
    deleteLater();
}

