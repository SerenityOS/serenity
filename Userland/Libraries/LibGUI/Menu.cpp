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
#include <LibGUI/Application.h>
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

void Menu::set_icon(Gfx::Bitmap const* icon)
{
    m_icon = icon;
}

void Menu::add_action(NonnullRefPtr<Action> action)
{
    auto item = make<MenuItem>(m_menu_id, move(action));
    if (m_menu_id != -1)
        realize_menu_item(*item, m_items.size());
    m_items.append(move(item));
}

void Menu::remove_all_actions()
{
    for (auto& item : m_items) {
        ConnectionToWindowServer::the().async_remove_menu_item(m_menu_id, item->identifier());
    }
    m_items.clear();
}

void Menu::update_parent_menu_item()
{
    if (auto parent = m_parent_menu.strong_ref()) {
        auto const& parent_items = parent->items();
        if (m_index_in_parent_menu >= 0 && static_cast<unsigned>(m_index_in_parent_menu) < parent_items.size()) {
            auto& item = *parent_items[m_index_in_parent_menu];
            if (item.submenu() == this)
                item.update_from_menu(Badge<Menu> {});
            return;
        }
        // Parent has since been cleared/repopulated:
        parent = nullptr;
        m_index_in_parent_menu = -1;
    }
}

void Menu::set_name(String name)
{
    m_name = move(name);
    if (m_menu_id != -1) {
        ConnectionToWindowServer::the().async_set_menu_name(m_menu_id, m_name);
        update_parent_menu_item();
    }
}

void Menu::set_minimum_width(int minimum_width)
{
    m_minimum_width = minimum_width;
    if (m_menu_id != -1) {
        ConnectionToWindowServer::the().async_set_menu_minimum_width(m_menu_id, m_minimum_width);
        update_parent_menu_item();
    }
}

void Menu::set_parent(Menu& menu, int submenu_index)
{
    m_parent_menu = menu;
    m_index_in_parent_menu = submenu_index;
}

NonnullRefPtr<Menu> Menu::add_submenu(String name)
{
    auto submenu = Menu::construct(move(name));

    auto item = make<MenuItem>(m_menu_id, submenu);
    submenu->set_parent(*this, m_items.size());

    if (m_menu_id != -1)
        realize_menu_item(*item, m_items.size());

    m_items.append(move(item));
    return submenu;
}

void Menu::add_separator()
{
    auto item = make<MenuItem>(m_menu_id, MenuItem::Type::Separator);
    if (m_menu_id != -1)
        realize_menu_item(*item, m_items.size());
    m_items.append(move(item));
}

void Menu::realize_if_needed(RefPtr<Action> const& default_action)
{
    if (m_menu_id == -1 || m_current_default_action.ptr() != default_action)
        realize_menu(default_action);
}

void Menu::popup(Gfx::IntPoint screen_position, RefPtr<Action> const& default_action, Gfx::IntRect const& button_rect)
{
    realize_if_needed(default_action);
    ConnectionToWindowServer::the().async_popup_menu(m_menu_id, screen_position, button_rect);
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

    ConnectionToWindowServer::the().async_create_menu(m_menu_id, m_name, m_minimum_width);

    dbgln_if(MENU_DEBUG, "GUI::Menu::realize_menu(): New menu ID: {}", m_menu_id);
    VERIFY(m_menu_id > 0);
    m_current_default_action = default_action;

    for (size_t i = 0; i < m_items.size(); ++i) {
        realize_menu_item(*m_items[i], i);
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
    return m_items[index]->action();
}

void Menu::set_children_actions_enabled(bool enabled)
{
    for (auto& item : m_items) {
        if (item->action())
            item->action()->set_enabled(enabled);
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
        auto shortcut_text = action.shortcut().is_valid() ? action.shortcut().to_byte_string() : ByteString();
        bool exclusive = action.group() && action.group()->is_exclusive() && action.is_checkable();
        bool is_default = (m_current_default_action.ptr() == &action);
        auto icon = action.icon() ? action.icon()->to_shareable_bitmap() : Gfx::ShareableBitmap();
        ConnectionToWindowServer::the().async_add_menu_item(m_menu_id, item_id, -1, action.text(), action.is_enabled(), action.is_visible(), action.is_checkable(), action.is_checkable() ? action.is_checked() : false, is_default, shortcut_text, icon, exclusive);
        break;
    }
    case MenuItem::Type::Submenu: {
        auto& submenu = *item.submenu();
        submenu.realize_if_needed(m_current_default_action.strong_ref());
        auto icon = submenu.icon() ? submenu.icon()->to_shareable_bitmap() : Gfx::ShareableBitmap();
        ConnectionToWindowServer::the().async_add_menu_item(m_menu_id, item_id, submenu.menu_id(), submenu.name().to_byte_string(), true, true, false, false, false, "", icon, false);
        break;
    }
    case MenuItem::Type::Invalid:
    default:
        VERIFY_NOT_REACHED();
    }
}

void Menu::add_recent_files_list(Function<void(Action&)> callback, AddTrailingSeparator add_trailing_separator)
{
    m_recent_files_callback = move(callback);

    Vector<NonnullRefPtr<GUI::Action>> recent_file_actions;

    for (size_t i = 0; i < GUI::Application::max_recently_open_files(); ++i) {
        recent_file_actions.append(GUI::Action::create("", [&](auto& action) { m_recent_files_callback(action); }));
    }

    recent_file_actions.append(GUI::Action::create("(No recently open files)", nullptr));
    recent_file_actions.last()->set_enabled(false);

    auto* app = GUI::Application::the();
    app->register_recent_file_actions({}, recent_file_actions);

    for (auto& action : recent_file_actions) {
        add_action(action);
    }

    if (add_trailing_separator == AddTrailingSeparator::Yes)
        add_separator();
}

}
