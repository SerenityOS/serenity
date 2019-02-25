#pragma once

#include <LibGUI/GMenuItem.h>
#include <AK/Function.h>
#include <AK/Vector.h>

class GAction;

class GMenu {
public:
    explicit GMenu(const String& name);
    ~GMenu();

    static GMenu* from_menu_id(int);

    GAction* action_at(int);

    void add_action(RetainPtr<GAction>&&);
    void add_separator();

    Function<void(unsigned)> on_item_activation;

private:
    friend class GMenuBar;
    int menu_id() const { return m_menu_id; }
    int realize_menu();
    void unrealize_menu();

    int m_menu_id { 0 };
    String m_name;
    Vector<OwnPtr<GMenuItem>> m_items;
};
