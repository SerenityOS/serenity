/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Menubar.h>

namespace GUI {

ErrorOr<void> Menubar::try_add_menu(Badge<Window>, NonnullRefPtr<Menu> menu)
{
    TRY(m_menus.try_append(menu));
    return {};
}

ErrorOr<NonnullRefPtr<Menu>> Menubar::try_add_menu(Badge<Window>, DeprecatedString name)
{
    auto menu = TRY(try_add<Menu>(TRY(String::from_deprecated_string(name))));
    TRY(m_menus.try_append(menu));
    return menu;
}

Menu& Menubar::add_menu(Badge<Window>, DeprecatedString name)
{
    auto& menu = add<Menu>(String::from_deprecated_string(name).release_value_but_fixme_should_propagate_errors());
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
