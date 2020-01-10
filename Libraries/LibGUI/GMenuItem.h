#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>

class GAction;
class GMenu;

class GMenuItem {
public:
    enum Type {
        Invalid,
        Action,
        Separator,
        Submenu,
    };

    GMenuItem(unsigned menu_id, Type);
    GMenuItem(unsigned menu_id, NonnullRefPtr<GAction>&&);
    GMenuItem(unsigned menu_id, NonnullRefPtr<GMenu>&&);
    ~GMenuItem();

    Type type() const { return m_type; }
    String text() const;
    const GAction* action() const { return m_action.ptr(); }
    GAction* action() { return m_action.ptr(); }
    unsigned identifier() const { return m_identifier; }

    GMenu* submenu() { return m_submenu.ptr(); }
    const GMenu* submenu() const { return m_submenu.ptr(); }

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    void set_menu_id(Badge<GMenu>, unsigned menu_id) { m_menu_id = menu_id; }
    void set_identifier(Badge<GMenu>, unsigned identifier) { m_identifier = identifier; }

private:
    void update_window_server();

    Type m_type { Invalid };
    int m_menu_id { -1 };
    unsigned m_identifier { 0 };
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    RefPtr<GAction> m_action;
    RefPtr<GMenu> m_submenu;
};
