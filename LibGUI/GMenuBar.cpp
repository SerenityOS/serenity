#include <LibGUI/GMenuBar.h>
#include <LibC/gui.h>

GMenuBar::GMenuBar()
{
}

GMenuBar::~GMenuBar()
{
    if (m_menubar_id) {
        gui_menubar_destroy(m_menubar_id);
        m_menubar_id = 0;
    }
}

void GMenuBar::add_menu(OwnPtr<GMenu>&& menu)
{
    m_menus.append(move(menu));
}

void GMenuBar::notify_added_to_application(Badge<GApplication>)
{
    ASSERT(!m_menubar_id);
    m_menubar_id = gui_menubar_create();
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
    ASSERT(m_menubar_id);
    gui_menubar_destroy(m_menubar_id);
    m_menubar_id = 0;
}
