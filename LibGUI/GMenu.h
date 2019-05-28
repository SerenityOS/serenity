#pragma once

#include <AK/Function.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <AK/Vector.h>
#include <LibGUI/GMenuItem.h>

class GAction;
class Point;

class GMenu {
public:
    explicit GMenu(const String& name);
    ~GMenu();

    static GMenu* from_menu_id(int);

    GAction* action_at(int);

    void add_action(Retained<GAction>);
    void add_separator();

    void popup(const Point& screen_position);
    void dismiss();

    Function<void(unsigned)> on_item_activation;

private:
    friend class GMenuBar;

    int menu_id() const { return m_menu_id; }
    int realize_menu();
    void unrealize_menu();

    int m_menu_id { -1 };
    String m_name;
    Vector<OwnPtr<GMenuItem>> m_items;
};
