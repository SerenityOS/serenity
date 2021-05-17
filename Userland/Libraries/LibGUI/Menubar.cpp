/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/IDAllocator.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/WindowServerConnection.h>

namespace GUI {

static IDAllocator s_menubar_id_allocator;

Menubar::Menubar()
{
}

Menubar::~Menubar()
{
    unrealize_menubar();
}

Menu& Menubar::add_menu(String name)
{
    auto& menu = add<Menu>(move(name));
    m_menus.append(menu);
    return menu;
}

int Menubar::realize_menubar()
{
    auto menubar_id = s_menubar_id_allocator.allocate();
    WindowServerConnection::the().async_create_menubar(menubar_id);
    return menubar_id;
}

void Menubar::unrealize_menubar()
{
    if (m_menubar_id == -1)
        return;
    WindowServerConnection::the().async_destroy_menubar(m_menubar_id);
    m_menubar_id = -1;
}

void Menubar::notify_added_to_window(Badge<Window>)
{
    VERIFY(m_menubar_id == -1);
    m_menubar_id = realize_menubar();
    VERIFY(m_menubar_id != -1);
    for (auto& menu : m_menus) {
        int menu_id = menu.realize_menu();
        VERIFY(menu_id != -1);
        WindowServerConnection::the().async_add_menu_to_menubar(m_menubar_id, menu_id);
    }
}

void Menubar::notify_removed_from_window(Badge<Window>)
{
    unrealize_menubar();
}

}
