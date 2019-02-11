#pragma once

#include <LibGUI/GMenu.h>
#include <AK/Badge.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class GApplication;

class GMenuBar {
public:
    GMenuBar();
    ~GMenuBar();

    void add_menu(OwnPtr<GMenu>&&);

    void notify_added_to_application(Badge<GApplication>);
    void notify_removed_from_application(Badge<GApplication>);

private:
    Vector<OwnPtr<GMenu>> m_menus;
};
