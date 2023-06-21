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
        bool duplicate_menu_detected = false;

        for_each_menu([&](Menu& existing_menu) {
            if (&menu == &existing_menu) {
                dbgln("Duplicate Menu \"{}\" ({})", menu.name(), &menu);
                duplicate_menu_detected = true;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (duplicate_menu_detected) {
            return;
        }

        m_menus.append(menu);
        layout_menu(menu, window_rect);
    }

    bool flash_menu(Menu* flashed_submenu)
    {
        Menu* const old_flashed_menu = m_flashed_menu;
        m_flashed_menu = nullptr;

        if (flashed_submenu) {
            for_each_menu([&](Menu& menu) {
                if ((&menu) == flashed_submenu || menu.is_menu_ancestor_of(*flashed_submenu)) {
                    m_flashed_menu = &menu;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        }

        return (old_flashed_menu != m_flashed_menu);
    }

    Menu* flashed_menu() const { return m_flashed_menu; }

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

    void font_changed(Gfx::IntRect window_rect)
    {
        m_next_menu_location = { 0, 0 };
        for (auto& menu : m_menus)
            layout_menu(menu, window_rect);
    }

private:
    void layout_menu(Menu&, Gfx::IntRect window_rect);

    Vector<Menu&> m_menus;
    Menu* m_flashed_menu { nullptr };

    // FIXME: This doesn't support removing menus from a menubar or inserting a
    //        menu in the middle.
    Gfx::IntPoint m_next_menu_location { 0, 0 };
};

}
