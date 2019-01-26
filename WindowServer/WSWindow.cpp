#include "WSWindow.h"
#include "WSWindowManager.h"
#include "WSMessage.h"
#include "WSMessageLoop.h"
#include "Process.h"

WSWindow::WSWindow(Process& process, int window_id)
    : m_process(process)
    , m_window_id(window_id)
    , m_pid(process.pid())
{
    WSWindowManager::the().add_window(*this);
}

WSWindow::~WSWindow()
{
    WSWindowManager::the().remove_window(*this);
}

void WSWindow::set_title(String&& title)
{
    {
        WSWindowLocker locker(*this);
        if (m_title == title)
            return;
        m_title = move(title);
    }

    WSWindowManager::the().notify_title_changed(*this);
}

void WSWindow::set_rect(const Rect& rect)
{
    Rect old_rect;
    {
        WSWindowLocker locker(*this);
        if (m_rect == rect)
            return;
        old_rect = m_rect;
        m_rect = rect;
        m_backing = GraphicsBitmap::create(m_process, m_rect.size());
    }
    WSWindowManager::the().notify_rect_changed(*this, old_rect, rect);
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

void WSWindow::event(WSMessage& event)
{
    GUI_Event gui_event;
    gui_event.window_id = window_id();

    switch (event.type()) {
    case WSMessage::Paint:
        gui_event.type = GUI_Event::Type::Paint;
        gui_event.paint.rect = static_cast<WSPaintEvent&>(event).rect();
        break;
    case WSMessage::MouseMove:
        gui_event.type = GUI_Event::Type::MouseMove;
        gui_event.mouse.position = static_cast<WSMouseEvent&>(event).position();
        gui_event.mouse.button = GUI_MouseButton::NoButton;
        gui_event.mouse.buttons = static_cast<WSMouseEvent&>(event).buttons();
        break;
    case WSMessage::MouseDown:
        gui_event.type = GUI_Event::Type::MouseDown;
        gui_event.mouse.position = static_cast<WSMouseEvent&>(event).position();
        gui_event.mouse.button = to_api(static_cast<WSMouseEvent&>(event).button());
        gui_event.mouse.buttons = static_cast<WSMouseEvent&>(event).buttons();
        break;
    case WSMessage::MouseUp:
        gui_event.type = GUI_Event::Type::MouseUp;
        gui_event.mouse.position = static_cast<WSMouseEvent&>(event).position();
        gui_event.mouse.button = to_api(static_cast<WSMouseEvent&>(event).button());
        gui_event.mouse.buttons = static_cast<WSMouseEvent&>(event).buttons();
        break;
    case WSMessage::KeyDown:
        gui_event.type = GUI_Event::Type::KeyDown;
        gui_event.key.character = static_cast<WSKeyEvent&>(event).character();
        gui_event.key.key = static_cast<WSKeyEvent&>(event).key();
        gui_event.key.alt = static_cast<WSKeyEvent&>(event).alt();
        gui_event.key.ctrl = static_cast<WSKeyEvent&>(event).ctrl();
        gui_event.key.shift = static_cast<WSKeyEvent&>(event).shift();
        break;
    case WSMessage::WM_Invalidate:
        WSWindowManager::the().invalidate(*this, static_cast<WSWindowInvalidationEvent&>(event).rect());
        return;
    case WSMessage::WM_SetWindowRect:
        set_rect(static_cast<WSSetWindowRect&>(event).rect());
        return;
    case WSMessage::WM_SetWindowTitle:
        set_title(static_cast<WSSetWindowTitle&>(event).title());
        return;
    case WSMessage::WindowActivated:
        gui_event.type = GUI_Event::Type::WindowActivated;
        break;
    case WSMessage::WindowDeactivated:
        gui_event.type = GUI_Event::Type::WindowDeactivated;
        break;
    }

    if (gui_event.type == GUI_Event::Type::Invalid)
        return;

    {
        LOCKER(m_process.gui_events_lock());
        m_process.gui_events().append(move(gui_event));
    }
}
