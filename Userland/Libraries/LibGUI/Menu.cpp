/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/IDAllocator.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

static IDAllocator s_menu_id_allocator;

static HashMap<int, Menu*>& all_menus()
{
    static HashMap<int, Menu*>* map;
    if (!map)
        map = new HashMap<int, Menu*>();
    return *map;
}

Menu* Menu::from_menu_id(int menu_id)
{
    auto it = all_menus().find(menu_id);
    if (it == all_menus().end())
        return nullptr;
    return (*it).value;
}

Menu::Menu(String name)
    : m_name(move(name))
{
}

Menu::~Menu()
{
    unrealize_menu();
}

void Menu::set_icon(const Gfx::Bitmap* icon)
{
    m_icon = icon;
}

void Menu::add_action(NonnullRefPtr<Action> action)
{
    m_items.append(make<MenuItem>(m_menu_id, move(action)));
}

Menu& Menu::add_submenu(const String& name)
{
    auto submenu = Menu::construct(name);
    m_items.append(make<MenuItem>(m_menu_id, submenu));
    return submenu;
}

void Menu::add_separator()
{
    m_items.append(make<MenuItem>(m_menu_id, MenuItem::Type::Separator));
}

void Menu::realize_if_needed(const RefPtr<Action>& default_action)
{
    if (m_menu_id == -1 || m_last_default_action.ptr() != default_action)
        realize_menu(default_action);
}

void Menu::popup(const Gfx::IntPoint& screen_position, const RefPtr<Action>& default_action)
{
    realize_if_needed(default_action);
    WindowServerConnection::the().async_popup_menu(m_menu_id, screen_position);
}

void Menu::dismiss()
{
    if (m_menu_id == -1)
        return;
    WindowServerConnection::the().async_dismiss_menu(m_menu_id);
}

int Menu::realize_menu(RefPtr<Action> default_action)
{
    unrealize_menu();
    m_menu_id = s_menu_id_allocator.allocate();

    WindowServerConnection::the().async_create_menu(m_menu_id, m_name);

    dbgln_if(MENU_DEBUG, "GUI::Menu::realize_menu(): New menu ID: {}", m_menu_id);
    VERIFY(m_menu_id > 0);
    for (size_t i = 0; i < m_items.size(); ++i) {
        auto& item = m_items[i];
        item.set_menu_id({}, m_menu_id);
        item.set_identifier({}, i);
        if (item.type() == MenuItem::Type::Separator) {
            WindowServerConnection::the().async_add_menu_separator(m_menu_id);
            continue;
        }
        if (item.type() == MenuItem::Type::Submenu) {
            auto& submenu = *item.submenu();
            submenu.realize_if_needed(default_action);
            auto icon = submenu.icon() ? submenu.icon()->to_shareable_bitmap() : Gfx::ShareableBitmap();
            WindowServerConnection::the().async_add_menu_item(m_menu_id, i, submenu.menu_id(), submenu.name(), true, false, false, false, "", icon, false);
            continue;
        }
        if (item.type() == MenuItem::Type::Action) {
            auto& action = *item.action();
            auto shortcut_text = action.shortcut().is_valid() ? action.shortcut().to_string() : String();
            bool exclusive = action.group() && action.group()->is_exclusive() && action.is_checkable();
            bool is_default = (default_action.ptr() == &action);
            auto icon = action.icon() ? action.icon()->to_shareable_bitmap() : Gfx::ShareableBitmap();
            WindowServerConnection::the().async_add_menu_item(m_menu_id, i, -1, action.text(), action.is_enabled(), action.is_checkable(), action.is_checkable() ? action.is_checked() : false, is_default, shortcut_text, icon, exclusive);
        }
    }
    all_menus().set(m_menu_id, this);
    m_last_default_action = default_action;
    return m_menu_id;
}

void Menu::unrealize_menu()
{
    if (m_menu_id == -1)
        return;
    all_menus().remove(m_menu_id);
    WindowServerConnection::the().async_destroy_menu(m_menu_id);
    m_menu_id = -1;
}

void Menu::realize_menu_if_needed()
{
    if (menu_id() == -1)
        realize_menu();
}

Action* Menu::action_at(size_t index)
{
    if (index >= m_items.size())
        return nullptr;
    return m_items[index].action();
}

void Menu::visibility_did_change(Badge<WindowServerConnection>, bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    if (on_visibility_change)
        on_visibility_change(visible);
}

}
