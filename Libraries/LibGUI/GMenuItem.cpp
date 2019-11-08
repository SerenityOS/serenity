#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuItem.h>
#include <LibGUI/GWindowServerConnection.h>
#include <WindowServer/WSAPITypes.h>

GMenuItem::GMenuItem(unsigned menu_id, Type type)
    : m_type(type)
    , m_menu_id(menu_id)
{
}

GMenuItem::GMenuItem(unsigned menu_id, NonnullRefPtr<GAction>&& action)
    : m_type(Action)
    , m_menu_id(menu_id)
    , m_action(move(action))
{
    m_action->register_menu_item({}, *this);
    m_enabled = m_action->is_enabled();
    m_checkable = m_action->is_checkable();
    if (m_checkable)
        m_checked = m_action->is_checked();
}

GMenuItem::GMenuItem(unsigned menu_id, NonnullOwnPtr<GMenu>&& submenu)
    : m_type(Submenu)
    , m_menu_id(menu_id)
    , m_submenu(move(submenu))
{
}

GMenuItem::~GMenuItem()
{
    if (m_action)
        m_action->unregister_menu_item({}, *this);
}

void GMenuItem::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    update_window_server();
}

void GMenuItem::set_checked(bool checked)
{
    ASSERT(is_checkable());
    if (m_checked == checked)
        return;
    m_checked = checked;
    update_window_server();
}

void GMenuItem::update_window_server()
{
    if (m_menu_id < 0)
        return;
    auto& action = *m_action;
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::UpdateMenuItem;
    request.menu.menu_id = m_menu_id;
    request.menu.identifier = m_identifier;
    request.menu.enabled = action.is_enabled();
    request.menu.checkable = action.is_checkable();
    if (action.is_checkable())
        request.menu.checked = action.is_checked();
    ASSERT(action.text().length() < (ssize_t)sizeof(request.text));
    strcpy(request.text, action.text().characters());
    request.text_length = action.text().length();

    if (action.shortcut().is_valid()) {
        auto shortcut_text = action.shortcut().to_string();
        ASSERT(shortcut_text.length() < (ssize_t)sizeof(request.menu.shortcut_text));
        strcpy(request.menu.shortcut_text, shortcut_text.characters());
        request.menu.shortcut_text_length = shortcut_text.length();
    } else {
        request.menu.shortcut_text_length = 0;
    }

    GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidUpdateMenuItem);
}
