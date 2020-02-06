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

#include <LibGUI/MenuBar.h>
#include <LibGUI/WindowServerConnection.h>

namespace GUI {

MenuBar::MenuBar()
{
}

MenuBar::~MenuBar()
{
    unrealize_menubar();
}

void MenuBar::add_menu(NonnullRefPtr<Menu> menu)
{
    m_menus.append(move(menu));
}

int MenuBar::realize_menubar()
{
    return WindowServerConnection::the().send_sync<Messages::WindowServer::CreateMenubar>()->menubar_id();
}

void MenuBar::unrealize_menubar()
{
    if (m_menubar_id == -1)
        return;
    WindowServerConnection::the().send_sync<Messages::WindowServer::DestroyMenubar>(m_menubar_id);
    m_menubar_id = -1;
}

void MenuBar::notify_added_to_application(Badge<Application>)
{
    ASSERT(m_menubar_id == -1);
    m_menubar_id = realize_menubar();
    ASSERT(m_menubar_id != -1);
    for (auto& menu : m_menus) {
        int menu_id = menu.realize_menu();
        ASSERT(menu_id != -1);
        WindowServerConnection::the().send_sync<Messages::WindowServer::AddMenuToMenubar>(m_menubar_id, menu_id);
    }
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetApplicationMenubar>(m_menubar_id);
}

void MenuBar::notify_removed_from_application(Badge<Application>)
{
    unrealize_menubar();
}

}
