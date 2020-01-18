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

#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindowServerConnection.h>

GMenuBar::GMenuBar()
{
}

GMenuBar::~GMenuBar()
{
    unrealize_menubar();
}

void GMenuBar::add_menu(NonnullRefPtr<GMenu> menu)
{
    m_menus.append(move(menu));
}

int GMenuBar::realize_menubar()
{
    return GWindowServerConnection::the().send_sync<WindowServer::CreateMenubar>()->menubar_id();
}

void GMenuBar::unrealize_menubar()
{
    if (m_menubar_id == -1)
        return;
    GWindowServerConnection::the().send_sync<WindowServer::DestroyMenubar>(m_menubar_id);
    m_menubar_id = -1;
}

void GMenuBar::notify_added_to_application(Badge<GApplication>)
{
    ASSERT(m_menubar_id == -1);
    m_menubar_id = realize_menubar();
    ASSERT(m_menubar_id != -1);
    for (auto& menu : m_menus) {
        int menu_id = menu.realize_menu();
        ASSERT(menu_id != -1);
        GWindowServerConnection::the().send_sync<WindowServer::AddMenuToMenubar>(m_menubar_id, menu_id);
    }
    GWindowServerConnection::the().send_sync<WindowServer::SetApplicationMenubar>(m_menubar_id);
}

void GMenuBar::notify_removed_from_application(Badge<GApplication>)
{
    unrealize_menubar();
}
