#include "TaskbarButton.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GWindowServerConnection.h>

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
    GWindowServerConnection::the().post_message(WindowServer::WM_PopupWindowMenu(m_identifier.client_id(), m_identifier.window_id(), screen_relative_rect().location()));
}

void TaskbarButton::resize_event(GResizeEvent& event)
{
    GWindowServerConnection::the().post_message(
        WindowServer::WM_SetWindowTaskbarRect(
            m_identifier.client_id(),
            m_identifier.window_id(),
            screen_relative_rect()));
    return GButton::resize_event(event);
}
