#pragma once

#include <LibGUI/GMenuItem.h>
#include <AK/Vector.h>

class GMenu {
public:
    explicit GMenu(const String& name);
    ~GMenu();

    void add_item(unsigned identifier, const String& text);
    void add_separator();

private:
    String m_name;
    Vector<GMenuItem> m_items;
};
