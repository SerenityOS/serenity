/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Menubar.h>

namespace GUI {

Menubar::Menubar()
{
}

Menubar::~Menubar()
{
}

ErrorOr<NonnullRefPtr<Menu>> Menubar::try_add_menu(Badge<Window>, String name)
{
    auto menu = TRY(try_add<Menu>(move(name)));
    TRY(m_menus.try_append(menu));
    return menu;
}

Menu& Menubar::add_menu(Badge<Window>, String name)
{
    auto& menu = add<Menu>(move(name));
    m_menus.append(menu);
    return menu;
}

void Menubar::for_each_menu(Function<IterationDecision(Menu&)> callback)
{
    for (auto& menu : m_menus) {
        if (callback(menu) == IterationDecision::Break) {
            return;
        }
    }
}

}
