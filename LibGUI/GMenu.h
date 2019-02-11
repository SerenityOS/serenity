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
    friend class GMenuBar;
    int menu_id() const { return m_menu_id; }
    int realize_menu();

    int m_menu_id { 0 };
    String m_name;
    Vector<GMenuItem> m_items;
};
