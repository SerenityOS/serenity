#include "WSWindow.h"
#include "WSWindowManager.h"
#include "WSMessage.h"
#include "WSMessageLoop.h"
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSClientConnection.h>

WSWindow::WSWindow(WSMessageReceiver& internal_owner, WSWindowType type)
    : m_internal_owner(&internal_owner)
    , m_type(type)
{
    WSWindowManager::the().add_window(*this);
}

WSWindow::WSWindow(WSClientConnection& client, int window_id)
    : m_client(&client)
    , m_type(WSWindowType::Normal)
    , m_window_id(window_id)
{
    WSWindowManager::the().add_window(*this);
}

WSWindow::~WSWindow()
{
    WSWindowManager::the().remove_window(*this);
}

void WSWindow::set_title(String&& title)
{
    if (m_title == title)
        return;
    m_title = move(title);
    WSWindowManager::the().notify_title_changed(*this);
}

void WSWindow::set_rect(const Rect& rect)
{
    Rect old_rect;
    if (m_rect == rect)
        return;
    old_rect = m_rect;
    m_rect = rect;
    if (!m_client && (!m_backing_store || old_rect.size() != rect.size())) {
        m_backing_store = GraphicsBitmap::create(GraphicsBitmap::Format::RGB32, m_rect.size());
    }
    WSWindowManager::the().notify_rect_changed(*this, old_rect, rect);
}

// FIXME: Just use the same types.
static WSAPI_MouseButton to_api(MouseButton button)
{
    switch (button) {
    case MouseButton::None: return WSAPI_MouseButton::NoButton;
    case MouseButton::Left: return WSAPI_MouseButton::Left;
    case MouseButton::Right: return WSAPI_MouseButton::Right;
    case MouseButton::Middle: return WSAPI_MouseButton::Middle;
    }
    ASSERT_NOT_REACHED();
}

void WSWindow::on_message(WSMessage& message)
{
    if (m_internal_owner)
        return m_internal_owner->on_message(message);

    WSAPI_ServerMessage server_message;
    server_message.window_id = window_id();

    switch (message.type()) {
    case WSMessage::MouseMove:
        server_message.type = WSAPI_ServerMessage::Type::MouseMove;
        server_message.mouse.position = static_cast<WSMouseEvent&>(message).position();
        server_message.mouse.button = WSAPI_MouseButton::NoButton;
        server_message.mouse.buttons = static_cast<WSMouseEvent&>(message).buttons();
        break;
    case WSMessage::MouseDown:
        server_message.type = WSAPI_ServerMessage::Type::MouseDown;
        server_message.mouse.position = static_cast<WSMouseEvent&>(message).position();
        server_message.mouse.button = to_api(static_cast<WSMouseEvent&>(message).button());
        server_message.mouse.buttons = static_cast<WSMouseEvent&>(message).buttons();
        break;
    case WSMessage::MouseUp:
        server_message.type = WSAPI_ServerMessage::Type::MouseUp;
        server_message.mouse.position = static_cast<WSMouseEvent&>(message).position();
        server_message.mouse.button = to_api(static_cast<WSMouseEvent&>(message).button());
        server_message.mouse.buttons = static_cast<WSMouseEvent&>(message).buttons();
        break;
    case WSMessage::WindowEntered:
        server_message.type = WSAPI_ServerMessage::Type::WindowEntered;
        break;
    case WSMessage::WindowLeft:
        server_message.type = WSAPI_ServerMessage::Type::WindowLeft;
        break;
    case WSMessage::KeyDown:
        server_message.type = WSAPI_ServerMessage::Type::KeyDown;
        server_message.key.character = static_cast<WSKeyEvent&>(message).character();
        server_message.key.key = static_cast<WSKeyEvent&>(message).key();
        server_message.key.modifiers = static_cast<WSKeyEvent&>(message).modifiers();
        break;
    case WSMessage::KeyUp:
        server_message.type = WSAPI_ServerMessage::Type::KeyUp;
        server_message.key.character = static_cast<WSKeyEvent&>(message).character();
        server_message.key.key = static_cast<WSKeyEvent&>(message).key();
        server_message.key.modifiers = static_cast<WSKeyEvent&>(message).modifiers();
        break;
    case WSMessage::WindowActivated:
        server_message.type = WSAPI_ServerMessage::Type::WindowActivated;
        break;
    case WSMessage::WindowDeactivated:
        server_message.type = WSAPI_ServerMessage::Type::WindowDeactivated;
        break;
    case WSMessage::WindowCloseRequest:
        server_message.type = WSAPI_ServerMessage::Type::WindowCloseRequest;
        break;
    case WSMessage::WindowResized:
        server_message.type = WSAPI_ServerMessage::Type::WindowResized;
        server_message.window.old_rect = static_cast<WSResizeEvent&>(message).old_rect();
        server_message.window.rect = static_cast<WSResizeEvent&>(message).rect();
        break;
    default:
        break;
    }

    if (server_message.type == WSAPI_ServerMessage::Type::Invalid)
        return;

    ASSERT(m_client);
    m_client->post_message(server_message);
}

void WSWindow::set_global_cursor_tracking_enabled(bool enabled)
{
    m_global_cursor_tracking_enabled = enabled;
}

void WSWindow::set_visible(bool b)
{
    if (m_visible == b)
        return;
    m_visible = b;
    invalidate();
}

void WSWindow::invalidate()
{
    WSWindowManager::the().invalidate(*this);
}

bool WSWindow::is_active() const
{
    return WSWindowManager::the().active_window() == this;
}
