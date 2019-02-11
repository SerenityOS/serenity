#include <LibGUI/GMenuBar.h>

GMenuBar::GMenuBar()
{
}

GMenuBar::~GMenuBar()
{
}

void GMenuBar::add_menu(OwnPtr<GMenu>&& menu)
{
    m_menus.append(move(menu));
}

void GMenuBar::notify_added_to_application(Badge<GApplication>)
{
}

void GMenuBar::notify_removed_from_application(Badge<GApplication>)
{
}
