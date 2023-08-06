/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Menu.h"
#include "Menubar.h"
#include "Window.h"
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>

namespace WindowServer {

class MenuManager final : public Core::EventReceiver {
    C_OBJECT(MenuManager);

public:
    static MenuManager& the();

    virtual ~MenuManager() override = default;

    bool is_open(Menu const&) const;
    bool has_open_menu() const { return !m_open_menu_stack.is_empty(); }

    Menu* current_menu() { return m_current_menu.ptr(); }
    Menu* closest_open_ancestor_of(Menu const&) const;
    void set_current_menu(Menu*);
    void clear_current_menu();
    void open_menu(Menu&, bool as_current_menu = true);

    void close_everyone();
    void close_everyone_not_in_lineage(Menu&);
    void close_menu_and_descendants(Menu&);

    void close_all_menus_from_client(Badge<ConnectionFromClient>, ConnectionFromClient&);

    int theme_index() const { return m_theme_index; }

    Menu* previous_menu(Menu* current);
    Menu* next_menu(Menu* current);

    void did_change_theme();

    void set_hovered_menu(Menu*);
    Menu* hovered_menu() { return m_hovered_menu; }

private:
    MenuManager();

    void close_menus(Vector<Menu&>&);

    virtual void event(Core::Event&) override;
    void handle_mouse_event(MouseEvent&);

    void refresh();

    WeakPtr<Menu> m_current_menu;
    Vector<WeakPtr<Menu>> m_open_menu_stack;

    int m_theme_index { 0 };

    WeakPtr<Menu> m_hovered_menu;
};

}
