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

void WSWindow::on_message(WSMessage& message)
{
    GUI_Event gui_event;
    gui_event.window_id = window_id();

    switch (message.type()) {
    case WSMessage::WM_ClientWantsToPaint:
        gui_event.type = GUI_Event::Type::Paint;
        gui_event.paint.rect = static_cast<WSClientWantsToPaintMessage&>(message).rect();
        break;
    case WSMessage::MouseMove:
        gui_event.type = GUI_Event::Type::MouseMove;
        gui_event.mouse.position = static_cast<WSMouseEvent&>(message).position();
        gui_event.mouse.button = GUI_MouseButton::NoButton;
        gui_event.mouse.buttons = static_cast<WSMouseEvent&>(message).buttons();
        break;
    case WSMessage::MouseDown:
        gui_event.type = GUI_Event::Type::MouseDown;
        gui_event.mouse.position = static_cast<WSMouseEvent&>(message).position();
        gui_event.mouse.button = to_api(static_cast<WSMouseEvent&>(message).button());
        gui_event.mouse.buttons = static_cast<WSMouseEvent&>(message).buttons();
        break;
    case WSMessage::MouseUp:
        gui_event.type = GUI_Event::Type::MouseUp;
        gui_event.mouse.position = static_cast<WSMouseEvent&>(message).position();
        gui_event.mouse.button = to_api(static_cast<WSMouseEvent&>(message).button());
        gui_event.mouse.buttons = static_cast<WSMouseEvent&>(message).buttons();
        break;
    case WSMessage::KeyDown:
        gui_event.type = GUI_Event::Type::KeyDown;
        gui_event.key.character = static_cast<WSKeyEvent&>(message).character();
        gui_event.key.key = static_cast<WSKeyEvent&>(message).key();
        gui_event.key.alt = static_cast<WSKeyEvent&>(message).alt();
        gui_event.key.ctrl = static_cast<WSKeyEvent&>(message).ctrl();
        gui_event.key.shift = static_cast<WSKeyEvent&>(message).shift();
        break;
    case WSMessage::KeyUp:
        gui_event.type = GUI_Event::Type::KeyUp;
        gui_event.key.character = static_cast<WSKeyEvent&>(message).character();
        gui_event.key.key = static_cast<WSKeyEvent&>(message).key();
        gui_event.key.alt = static_cast<WSKeyEvent&>(message).alt();
        gui_event.key.ctrl = static_cast<WSKeyEvent&>(message).ctrl();
        gui_event.key.shift = static_cast<WSKeyEvent&>(message).shift();
        break;
    case WSMessage::WM_ClientFinishedPaint:
        WSWindowManager::the().invalidate(*this, static_cast<WSClientFinishedPaintMessage&>(message).rect());
        return;
    case WSMessage::WM_SetWindowRect:
        set_rect(static_cast<WSSetWindowRectMessage&>(message).rect());
        return;
    case WSMessage::WM_SetWindowTitle:
        set_title(static_cast<WSSetWindowTitleMessage&>(message).title());
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

void WSWindow::set_global_cursor_tracking_enabled(bool enabled)
{
    dbgprintf("WSWindow{%p} global_cursor_tracking <- %u\n", enabled);
    m_global_cursor_tracking_enabled = enabled;
}
