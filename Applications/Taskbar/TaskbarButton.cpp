#include "TaskbarButton.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GWindowServerConnection.h>
#include <WindowServer/WSAPITypes.h>

TaskbarButton::TaskbarButton(const WindowIdentifier& identifier, GWidget* parent)
    : GButton(parent)
    , m_identifier(identifier)
{
}

TaskbarButton::~TaskbarButton()
{
}

void TaskbarButton::context_menu_event(GContextMenuEvent&)
{
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::WM_PopupWindowMenu;
    request.wm.client_id = m_identifier.client_id();
    request.wm.window_id = m_identifier.window_id();
    request.wm.position = screen_relative_rect().location();
    GWindowServerConnection::the().post_message_to_server(request);
}
