/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Forward.h>
#include <AK/RefPtr.h>
#include <LibGUI/Forward.h>

namespace GUI {

class MenuItem {
public:
    enum class Type {
        Invalid,
        Action,
        Separator,
        Submenu,
    };

    MenuItem(unsigned menu_id, Type);
    MenuItem(unsigned menu_id, NonnullRefPtr<Action>);
    MenuItem(unsigned menu_id, NonnullRefPtr<Menu>);
    ~MenuItem();

    Type type() const { return m_type; }

    Action const* action() const { return m_action.ptr(); }
    Action* action() { return m_action.ptr(); }
    unsigned identifier() const { return m_identifier; }

    Menu* submenu() { return m_submenu.ptr(); }
    Menu const* submenu() const { return m_submenu.ptr(); }

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    bool is_default() const { return m_default; }
    void set_default(bool);

    int menu_id() const { return m_menu_id; }
    void set_menu_id(Badge<Menu>, unsigned menu_id);
    void set_identifier(Badge<Menu>, unsigned identifier);

    void update_from_action(Badge<Action>) { update_window_server(); }
    void update_from_menu(Badge<Menu>) { update_window_server(); }

private:
    void update_window_server();

    Type m_type { Type::Invalid };
    int m_menu_id { -1 };
    unsigned m_identifier { 0 };
    bool m_enabled { true };
    bool m_visible { true };
    bool m_checkable { false };
    bool m_checked { false };
    bool m_default { false };
    RefPtr<Action> m_action;
    RefPtr<Menu> m_submenu;
};

}
