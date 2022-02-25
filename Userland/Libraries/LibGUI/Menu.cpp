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
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuItem.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

static IDAllocator s_menu_id_allocator;

static HashMap<int, Menu*>& all_menus()
{
    static HashMap<int, Menu*> s_map;
    return s_map;
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

ErrorOr<void> Menu::try_add_action(NonnullRefPtr<Action> action)
{
    // NOTE: We grow the vector first, to get allocation failure handled immediately.
    TRY(m_items.try_ensure_capacity(m_items.size() + 1));

    auto item = TRY(adopt_nonnull_own_or_enomem(new (nothrow) MenuItem(m_menu_id, move(action))));
    if (m_menu_id != -1)
        realize_menu_item(*item, m_items.size());
    m_items.unchecked_append(move(item));
    return {};
}

void Menu::add_action(NonnullRefPtr<Action> action)
{
    MUST(try_add_action(move(action)));
}

void Menu::remove_all_actions()
{
    for (auto& item : m_items) {
        ConnectionToWindowServer::the().async_remove_menu_item(m_menu_id, item.identifier());
    }
    m_items.clear();
}

ErrorOr<NonnullRefPtr<Menu>> Menu::try_add_submenu(String name)
{
    // NOTE: We grow the vector first, to get allocation failure handled immediately.
    TRY(m_items.try_ensure_capacity(m_items.size() + 1));

    auto submenu = TRY(Menu::try_create(name));

    auto item = TRY(adopt_nonnull_own_or_enomem(new (nothrow) MenuItem(m_menu_id, submenu)));
    if (m_menu_id != -1)
        realize_menu_item(*item, m_items.size());

    m_items.unchecked_append(move(item));
    return submenu;
}

Menu& Menu::add_submenu(String name)
{
    auto menu = MUST(try_add_submenu(move(name)));
    return menu;
}

ErrorOr<void> Menu::try_add_separator()
{
    // NOTE: We grow the vector first, to get allocation failure handled immediately.
    TRY(m_items.try_ensure_capacity(m_items.size() + 1));

    auto item = TRY(adopt_nonnull_own_or_enomem(new (nothrow) MenuItem(m_menu_id, MenuItem::Type::Separator)));
    if (m_menu_id != -1)
        realize_menu_item(*item, m_items.size());
    m_items.unchecked_append(move(item));
    return {};
}

void Menu::add_separator()
{
    MUST(try_add_separator());
}

void Menu::realize_if_needed(const RefPtr<Action>& default_action)
{
    if (m_menu_id == -1 || m_current_default_action.ptr() != default_action)
        realize_menu(default_action);
}

void Menu::popup(const Gfx::IntPoint& screen_position, const RefPtr<Action>& default_action)
{
    realize_if_needed(default_action);
    ConnectionToWindowServer::the().async_popup_menu(m_menu_id, screen_position);
}

void Menu::dismiss()
{
    if (m_menu_id == -1)
        return;
    ConnectionToWindowServer::the().async_dismiss_menu(m_menu_id);
}

int Menu::realize_menu(RefPtr<Action> default_action)
{
    unrealize_menu();
    m_menu_id = s_menu_id_allocator.allocate();

    ConnectionToWindowServer::the().async_create_menu(m_menu_id, m_name);

    dbgln_if(MENU_DEBUG, "GUI::Menu::realize_menu(): New menu ID: {}", m_menu_id);
    VERIFY(m_menu_id > 0);
    m_current_default_action = default_action;

    for (size_t i = 0; i < m_items.size(); ++i) {
        realize_menu_item(m_items[i], i);
    }

    all_menus().set(m_menu_id, this);
    return m_menu_id;
}

void Menu::unrealize_menu()
{
    if (m_menu_id == -1)
        return;
    all_menus().remove(m_menu_id);
    ConnectionToWindowServer::the().async_destroy_menu(m_menu_id);
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

void Menu::set_children_actions_enabled(bool enabled)
{
    for (auto& item : m_items) {
        if (item.action())
            item.action()->set_enabled(enabled);
    }
}

void Menu::visibility_did_change(Badge<ConnectionToWindowServer>, bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    if (on_visibility_change)
        on_visibility_change(visible);
}

void Menu::realize_menu_item(MenuItem& item, int item_id)
{
    item.set_menu_id({}, m_menu_id);
    item.set_identifier({}, item_id);
    switch (item.type()) {
    case MenuItem::Type::Separator:
        ConnectionToWindowServer::the().async_add_menu_separator(m_menu_id);
        break;
    case MenuItem::Type::Action: {
        auto& action = *item.action();
        auto shortcut_text = action.shortcut().is_valid() ? action.shortcut().to_string() : String();
        bool exclusive = action.group() && action.group()->is_exclusive() && action.is_checkable();
        bool is_default = (m_current_default_action.ptr() == &action);
        auto icon = action.icon() ? action.icon()->to_shareable_bitmap() : Gfx::ShareableBitmap();
        ConnectionToWindowServer::the().async_add_menu_item(m_menu_id, item_id, -1, action.text(), action.is_enabled(), action.is_checkable(), action.is_checkable() ? action.is_checked() : false, is_default, shortcut_text, icon, exclusive);
        break;
    }
    case MenuItem::Type::Submenu: {
        auto& submenu = *item.submenu();
        submenu.realize_if_needed(m_current_default_action.strong_ref());
        auto icon = submenu.icon() ? submenu.icon()->to_shareable_bitmap() : Gfx::ShareableBitmap();
        ConnectionToWindowServer::the().async_add_menu_item(m_menu_id, item_id, submenu.menu_id(), submenu.name(), true, false, false, false, "", icon, false);
        break;
    }
    case MenuItem::Type::Invalid:
    default:
        VERIFY_NOT_REACHED();
    }
}

}
