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
#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GWindowServerConnection.h>

//#define GMENU_DEBUG

static HashMap<int, GMenu*>& all_menus()
{
    static HashMap<int, GMenu*>* map;
    if (!map)
        map = new HashMap<int, GMenu*>();
    return *map;
}

GMenu* GMenu::from_menu_id(int menu_id)
{
    auto it = all_menus().find(menu_id);
    if (it == all_menus().end())
        return nullptr;
    return (*it).value;
}

GMenu::GMenu(const StringView& name)
    : m_name(name)
{
}

GMenu::~GMenu()
{
    unrealize_menu();
}

void GMenu::add_action(NonnullRefPtr<GAction> action)
{
    m_items.append(make<GMenuItem>(m_menu_id, move(action)));
#ifdef GMENU_DEBUG
    dbgprintf("GMenu::add_action(): MenuItem Menu ID: %d\n", m_menu_id);
#endif
}

void GMenu::add_submenu(NonnullRefPtr<GMenu> submenu)
{
    m_items.append(make<GMenuItem>(m_menu_id, move(submenu)));
}

void GMenu::add_separator()
{
    m_items.append(make<GMenuItem>(m_menu_id, GMenuItem::Separator));
}

void GMenu::realize_if_needed()
{
    if (m_menu_id == -1)
        realize_menu();
}

void GMenu::popup(const Point& screen_position)
{
    realize_if_needed();
    GWindowServerConnection::the().post_message(WindowServer::PopupMenu(m_menu_id, screen_position));
}

void GMenu::dismiss()
{
    if (m_menu_id == -1)
        return;
    GWindowServerConnection::the().post_message(WindowServer::DismissMenu(m_menu_id));
}

int GMenu::realize_menu()
{
    m_menu_id = GWindowServerConnection::the().send_sync<WindowServer::CreateMenu>(m_name)->menu_id();

#ifdef GMENU_DEBUG
    dbgprintf("GMenu::realize_menu(): New menu ID: %d\n", m_menu_id);
#endif
    ASSERT(m_menu_id > 0);
    for (int i = 0; i < m_items.size(); ++i) {
        auto& item = m_items[i];
        item.set_menu_id({}, m_menu_id);
        item.set_identifier({}, i);
        if (item.type() == GMenuItem::Separator) {
            GWindowServerConnection::the().send_sync<WindowServer::AddMenuSeparator>(m_menu_id);
            continue;
        }
        if (item.type() == GMenuItem::Submenu) {
            auto& submenu = *item.submenu();
            submenu.realize_if_needed();
            GWindowServerConnection::the().send_sync<WindowServer::AddMenuItem>(m_menu_id, i, submenu.menu_id(), submenu.name(), true, false, false, "", -1, false);
            continue;
        }
        if (item.type() == GMenuItem::Action) {
            auto& action = *item.action();
            int icon_buffer_id = -1;
            if (action.icon()) {
                ASSERT(action.icon()->format() == GraphicsBitmap::Format::RGBA32);
                ASSERT(action.icon()->size() == Size(16, 16));
                if (action.icon()->shared_buffer_id() == -1) {
                    auto shared_buffer = SharedBuffer::create_with_size(action.icon()->size_in_bytes());
                    ASSERT(shared_buffer);
                    auto shared_icon = GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, *shared_buffer, action.icon()->size());
                    memcpy(shared_buffer->data(), action.icon()->bits(0), action.icon()->size_in_bytes());
                    shared_buffer->seal();
                    shared_buffer->share_with(GWindowServerConnection::the().server_pid());
                    action.set_icon(shared_icon);
                }
                icon_buffer_id = action.icon()->shared_buffer_id();
            }
            auto shortcut_text = action.shortcut().is_valid() ? action.shortcut().to_string() : String();
            bool exclusive = action.group() && action.group()->is_exclusive() && action.is_checkable();
            GWindowServerConnection::the().send_sync<WindowServer::AddMenuItem>(m_menu_id, i, -1, action.text(), action.is_enabled(), action.is_checkable(), action.is_checkable() ? action.is_checked() : false, shortcut_text, icon_buffer_id, exclusive);
        }
    }
    all_menus().set(m_menu_id, this);
    return m_menu_id;
}

void GMenu::unrealize_menu()
{
    if (m_menu_id == -1)
        return;
    all_menus().remove(m_menu_id);
    GWindowServerConnection::the().send_sync<WindowServer::DestroyMenu>(m_menu_id);
    m_menu_id = 0;
}

GAction* GMenu::action_at(int index)
{
    if (index >= m_items.size())
        return nullptr;
    return m_items[index].action();
}
