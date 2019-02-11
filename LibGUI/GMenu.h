#pragma once

#include <LibGUI/GMenuItem.h>
#include <AK/Vector.h>

class GMenu {
public:
    GMenu();
    ~GMenu();

private:
    Vector<GMenuItem> m_items;
};
