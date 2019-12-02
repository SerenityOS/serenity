#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindowServerConnection.h>

GMenuBar::GMenuBar()
{
}

GMenuBar::~GMenuBar()
{
    unrealize_menubar();
}

void GMenuBar::add_menu(NonnullOwnPtr<GMenu>&& menu)
{
    m_menus.append(move(menu));
}

int GMenuBar::realize_menubar()
{
    return GWindowServerConnection::the().send_sync<WindowServer::CreateMenubar>()->menubar_id();
}

void GMenuBar::unrealize_menubar()
{
    if (m_menubar_id == -1)
        return;
    GWindowServerConnection::the().send_sync<WindowServer::DestroyMenubar>(m_menubar_id);
    m_menubar_id = -1;
}

void GMenuBar::notify_added_to_application(Badge<GApplication>)
{
    ASSERT(m_menubar_id == -1);
    m_menubar_id = realize_menubar();
    ASSERT(m_menubar_id != -1);
    for (auto& menu : m_menus) {
        int menu_id = menu.realize_menu();
        ASSERT(menu_id != -1);
        GWindowServerConnection::the().send_sync<WindowServer::AddMenuToMenubar>(m_menubar_id, menu_id);
    }
    GWindowServerConnection::the().send_sync<WindowServer::SetApplicationMenubar>(m_menubar_id);
}

void GMenuBar::notify_removed_from_application(Badge<GApplication>)
{
    unrealize_menubar();
}
