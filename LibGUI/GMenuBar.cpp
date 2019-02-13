#include <LibGUI/GMenuBar.h>
#include <LibGUI/GEventLoop.h>
#include <LibC/gui.h>

GMenuBar::GMenuBar()
{
}

GMenuBar::~GMenuBar()
{
    unrealize_menubar();
}

void GMenuBar::add_menu(OwnPtr<GMenu>&& menu)
{
    m_menus.append(move(menu));
}

int GMenuBar::realize_menubar()
{
    GUI_ClientMessage request;
    request.type = GUI_ClientMessage::Type::CreateMenubar;
    GUI_ServerMessage response = GEventLoop::main().sync_request(request, GUI_ServerMessage::Type::DidCreateMenubar);
    return response.menu.menubar_id;
}

void GMenuBar::unrealize_menubar()
{
    if (!m_menubar_id)
        return;
    GUI_ClientMessage request;
    request.type = GUI_ClientMessage::Type::DestroyMenubar;
    request.menu.menubar_id = m_menubar_id;
    GEventLoop::main().sync_request(request, GUI_ServerMessage::Type::DidDestroyMenubar);
    m_menubar_id = 0;
}

void GMenuBar::notify_added_to_application(Badge<GApplication>)
{
    ASSERT(!m_menubar_id);
    m_menubar_id = realize_menubar();
    ASSERT(m_menubar_id > 0);
    for (auto& menu : m_menus) {
        ASSERT(menu);
        int menu_id = menu->realize_menu();
        ASSERT(menu_id > 0);
        int rc = gui_menubar_add_menu(m_menubar_id, menu_id);
        ASSERT(rc == 0);
    }
    gui_app_set_menubar(m_menubar_id);
}

void GMenuBar::notify_removed_from_application(Badge<GApplication>)
{
    unrealize_menubar();
}
