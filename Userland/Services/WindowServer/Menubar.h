/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Menu.h"
#include <AK/Function.h>
#include <AK/IterationDecision.h>
#include <AK/Vector.h>

namespace WindowServer {

class Menubar {
public:
    void add_menu(Menu& menu, Gfx::IntRect window_rect)
    {
        // FIXME: Check against duplicate menu additions.
        m_menus.append(menu);
        layout_menu(menu, window_rect);
    }

    bool has_menus()
    {
        return !m_menus.is_empty();
    }

    void for_each_menu(Function<IterationDecision(Menu&)> callback)
    {
        for (auto& menu : m_menus) {
            if (callback(menu) == IterationDecision::Break)
                return;
        }
    }

private:
    void layout_menu(Menu&, Gfx::IntRect window_rect);

    Vector<Menu&> m_menus;

    // FIXME: This doesn't support removing menus from a menubar or inserting a
    //        menu in the middle.
    Gfx::IntPoint m_next_menu_location { 0, 0 };
};

}
