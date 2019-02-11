#pragma once

#include <LibGUI/GMenu.h>
#include <AK/Vector.h>

class GMenuBar {
public:
    GMenuBar();
    ~GMenuBar();

private:
    Vector<GMenu> m_menus;
};
