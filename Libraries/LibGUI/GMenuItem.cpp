#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuItem.h>
#include <LibGUI/GWindowServerConnection.h>

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

GMenuItem::GMenuItem(unsigned menu_id, NonnullRefPtr<GMenu>&& submenu)
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
    auto shortcut_text = action.shortcut().is_valid() ? action.shortcut().to_string() : String();
    GWindowServerConnection::the().send_sync<WindowServer::UpdateMenuItem>(m_menu_id, m_identifier, -1, action.text(), action.is_enabled(), action.is_checkable(), action.is_checkable() ? action.is_checked() : false, shortcut_text);
}
