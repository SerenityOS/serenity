/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/HashMap.h>
#include <AK/SharedBuffer.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>

//#define MENU_DEBUG

namespace GUI {

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

Menu::Menu(const StringView& name)
    : m_name(name)
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
#ifdef GMENU_DEBUG
    dbgprintf("GUI::Menu::add_action(): MenuItem Menu ID: %d\n", m_menu_id);
#endif
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
    WindowServerConnection::the().post_message(Messages::WindowServer::PopupMenu(m_menu_id, screen_position));
}

void Menu::dismiss()
{
    if (m_menu_id == -1)
        return;
    WindowServerConnection::the().post_message(Messages::WindowServer::DismissMenu(m_menu_id));
}

template<typename IconContainerType>
static int ensure_realized_icon(IconContainerType& container)
{
    int icon_buffer_id = -1;
    if (container.icon()) {
        ASSERT(container.icon()->format() == Gfx::BitmapFormat::RGBA32);
        ASSERT(container.icon()->size() == Gfx::IntSize(16, 16));
        if (container.icon()->shbuf_id() == -1) {
            auto shared_buffer = SharedBuffer::create_with_size(container.icon()->size_in_bytes());
            ASSERT(shared_buffer);
            auto shared_icon = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, *shared_buffer, container.icon()->size());
            memcpy(shared_buffer->template data<u8>(), container.icon()->scanline_u8(0), container.icon()->size_in_bytes());
            shared_buffer->seal();
            shared_buffer->share_with(WindowServerConnection::the().server_pid());
            container.set_icon(shared_icon);
        }
        icon_buffer_id = container.icon()->shbuf_id();
    }
    return icon_buffer_id;
}

int Menu::realize_menu(RefPtr<Action> default_action)
{
    unrealize_menu();
    m_menu_id = WindowServerConnection::the().send_sync<Messages::WindowServer::CreateMenu>(m_name)->menu_id();

#ifdef MENU_DEBUG
    dbgprintf("GUI::Menu::realize_menu(): New menu ID: %d\n", m_menu_id);
#endif
    ASSERT(m_menu_id > 0);
    for (size_t i = 0; i < m_items.size(); ++i) {
        auto& item = m_items[i];
        item.set_menu_id({}, m_menu_id);
        item.set_identifier({}, i);
        if (item.type() == MenuItem::Type::Separator) {
            WindowServerConnection::the().send_sync<Messages::WindowServer::AddMenuSeparator>(m_menu_id);
            continue;
        }
        if (item.type() == MenuItem::Type::Submenu) {
            auto& submenu = *item.submenu();
            submenu.realize_if_needed(default_action);
            int icon_buffer_id = ensure_realized_icon(submenu);
            WindowServerConnection::the().send_sync<Messages::WindowServer::AddMenuItem>(m_menu_id, i, submenu.menu_id(), submenu.name(), true, false, false, false, "", icon_buffer_id, false);
            continue;
        }
        if (item.type() == MenuItem::Type::Action) {
            auto& action = *item.action();
            int icon_buffer_id = ensure_realized_icon(action);
            auto shortcut_text = action.shortcut().is_valid() ? action.shortcut().to_string() : String();
            bool exclusive = action.group() && action.group()->is_exclusive() && action.is_checkable();
            bool is_default = (default_action.ptr() == &action);
            WindowServerConnection::the().send_sync<Messages::WindowServer::AddMenuItem>(m_menu_id, i, -1, action.text(), action.is_enabled(), action.is_checkable(), action.is_checkable() ? action.is_checked() : false, is_default, shortcut_text, icon_buffer_id, exclusive);
        }
    }
    all_menus().set(m_menu_id, this);
    m_last_default_action = default_action ? default_action->make_weak_ptr() : nullptr;
    return m_menu_id;
}

void Menu::unrealize_menu()
{
    if (m_menu_id == -1)
        return;
    all_menus().remove(m_menu_id);
    WindowServerConnection::the().send_sync<Messages::WindowServer::DestroyMenu>(m_menu_id);
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

}
