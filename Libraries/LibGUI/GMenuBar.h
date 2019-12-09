#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/GMenu.h>

class GApplication;

class GMenuBar {
public:
    GMenuBar();
    ~GMenuBar();

    void add_menu(NonnullRefPtr<GMenu>);

    void notify_added_to_application(Badge<GApplication>);
    void notify_removed_from_application(Badge<GApplication>);

private:
    int realize_menubar();
    void unrealize_menubar();

    int m_menubar_id { -1 };
    NonnullRefPtrVector<GMenu> m_menus;
};
