#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>

class GAction;
class GMenu;

class GMenuItem {
public:
    enum Type { Invalid, Action, Separator };

    GMenuItem(unsigned menu_id, Type);
    GMenuItem(unsigned menu_id, Retained<GAction>&&);
    ~GMenuItem();

    Type type() const { return m_type; }
    String text() const;
    const GAction* action() const { return m_action.ptr(); }
    GAction* action() { return m_action.ptr(); }
    unsigned identifier() const { return m_identifier; }

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
    unsigned m_menu_id { 0 };
    unsigned m_identifier { 0 };
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    RetainPtr<GAction> m_action;
};

