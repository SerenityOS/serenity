/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Badge.h>
#include <YAK/IterationDecision.h>
#include <YAK/NonnullRefPtrVector.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Menu.h>

namespace GUI {

class Menubar : public Core::Object {
    C_OBJECT(Menubar);

public:
    ~Menubar() { }

    Menu& add_menu(Badge<Window>, String name)
    {
        auto& menu = add<Menu>(move(name));
        m_menus.append(menu);
        return menu;
    }

    void for_each_menu(Function<IterationDecision(Menu&)> callback)
    {
        for (auto& menu : m_menus) {
            if (callback(menu) == IterationDecision::Break) {
                return;
            }
        }
    }

private:
    Menubar() { }

    NonnullRefPtrVector<Menu> m_menus;
};

}
