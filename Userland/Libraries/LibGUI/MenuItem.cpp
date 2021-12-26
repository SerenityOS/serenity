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

#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/WindowServerConnection.h>

namespace GUI {

MenuItem::MenuItem(unsigned menu_id, Type type)
    : m_type(type)
    , m_menu_id(menu_id)
{
}

MenuItem::MenuItem(unsigned menu_id, NonnullRefPtr<Action> action)
    : m_type(Type::Action)
    , m_menu_id(menu_id)
    , m_action(move(action))
{
    m_action->register_menu_item({}, *this);
    m_enabled = m_action->is_enabled();
    m_checkable = m_action->is_checkable();
    if (m_checkable)
        m_checked = m_action->is_checked();
}

MenuItem::MenuItem(unsigned menu_id, NonnullRefPtr<Menu> submenu)
    : m_type(Type::Submenu)
    , m_menu_id(menu_id)
    , m_submenu(move(submenu))
{
}

MenuItem::~MenuItem()
{
    if (m_action)
        m_action->unregister_menu_item({}, *this);
}

void MenuItem::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    update_window_server();
}

void MenuItem::set_checked(bool checked)
{
    ASSERT(is_checkable());
    if (m_checked == checked)
        return;
    m_checked = checked;
    update_window_server();
}

void MenuItem::set_default(bool is_default)
{
    ASSERT(is_checkable());
    if (m_default == is_default)
        return;
    m_default = is_default;
    update_window_server();
}

void MenuItem::update_window_server()
{
    if (m_menu_id < 0)
        return;
    auto& action = *m_action;
    auto shortcut_text = action.shortcut().is_valid() ? action.shortcut().to_string() : String();
    WindowServerConnection::the().send_sync<Messages::WindowServer::UpdateMenuItem>(m_menu_id, m_identifier, -1, action.text(), action.is_enabled(), action.is_checkable(), action.is_checkable() ? action.is_checked() : false, m_default, shortcut_text);
}

void MenuItem::set_menu_id(Badge<Menu>, unsigned int menu_id)
{
    m_menu_id = menu_id;
}

void MenuItem::set_identifier(Badge<Menu>, unsigned identifier)
{
    m_identifier = identifier;
}

}
