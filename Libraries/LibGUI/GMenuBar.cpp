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
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::CreateMenubar;
    WSAPI_ServerMessage response = GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidCreateMenubar);
    return response.menu.menubar_id;
}

void GMenuBar::unrealize_menubar()
{
    if (m_menubar_id == -1)
        return;
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::DestroyMenubar;
    request.menu.menubar_id = m_menubar_id;
    GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidDestroyMenubar);
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
        WSAPI_ClientMessage request;
        request.type = WSAPI_ClientMessage::Type::AddMenuToMenubar;
        request.menu.menubar_id = m_menubar_id;
        request.menu.menu_id = menu_id;
        GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidAddMenuToMenubar);
    }
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetApplicationMenubar;
    request.menu.menubar_id = m_menubar_id;
    GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidSetApplicationMenubar);
}

void GMenuBar::notify_removed_from_application(Badge<GApplication>)
{
    unrealize_menubar();
}
