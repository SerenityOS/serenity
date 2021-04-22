/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Menubar.h"
#include "Menu.h"

namespace WindowServer {

Menubar::Menubar(ClientConnection& client, int menubar_id)
    : m_client(client)
    , m_menubar_id(menubar_id)
{
}

Menubar::~Menubar()
{
}

void Menubar::add_menu(Menu& menu)
{
    m_menus.append(&menu);
}

}
