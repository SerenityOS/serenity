#pragma once

#include <AK/Badge.h>
#include <AK/Vector.h>
#include <LibGUI/GMenu.h>

class GApplication;

class GMenuBar {
public:
    GMenuBar();
    ~GMenuBar();

    void add_menu(OwnPtr<GMenu>&&);

    void notify_added_to_application(Badge<GApplication>);
    void notify_removed_from_application(Badge<GApplication>);

private:
    int realize_menubar();
    void unrealize_menubar();

    int m_menubar_id { -1 };
    Vector<OwnPtr<GMenu>> m_menus;
};
